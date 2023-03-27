package ca.bc.gov.educ.studentdatacollection.api.rest;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeCriteriaEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class is used for REST calls
 *
 * @author Marco Villeneuve
 */
@Component
@Slf4j
public class RestUtils {
  public static final String SEARCH_CRITERIA_LIST = "searchCriteriaList";
  public static final String SCHOOL_CATEGORY_CODE = "schoolCategoryCode";
  public static final String FACILITY_TYPE_CODE = "facilityTypeCode";
  public static final String OPEN_DATE = "openedDate";
  public static final String CLOSE_DATE = "closedDate";
  private static final String CONTENT_TYPE = "Content-Type";
  private final Map<String, School> schoolMap = new ConcurrentHashMap<>();
  public static final String PAGE_SIZE = "pageSize";
  private static final String INSTITUTE_API_TOPIC = "INSTITUTE_API_TOPIC";
  private final WebClient webClient;
  private final MessagePublisher messagePublisher;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ReadWriteLock schoolLock = new ReentrantReadWriteLock();
  @Getter
  private final ApplicationProperties props;

  @Value("${initialization.background.enabled}")
  private Boolean isBackgroundInitializationEnabled;

  @Autowired
  public RestUtils(WebClient webClient, final ApplicationProperties props, final MessagePublisher messagePublisher) {
    this.webClient = webClient;
    this.props = props;
    this.messagePublisher = messagePublisher;
  }

  @PostConstruct
  public void init() {
    if (this.isBackgroundInitializationEnabled != null && this.isBackgroundInitializationEnabled) {
      ApplicationProperties.bgTask.execute(this::initialize);
    }
  }

  private void initialize() {
    this.populateSchoolMap();
  }

  @Scheduled(cron = "${schedule.jobs.load.school.cron}")
  public void scheduled() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      this.init();
    } finally {
      writeLock.unlock();
    }
  }

  public void populateSchoolMap() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      for (val school : this.getSchools()) {
        this.schoolMap.put(school.getSchoolId(), school);
      }
    }
    catch (Exception ex) {
      log.error("Unable to load map cache school {}", ex);
    }
    finally {
      writeLock.unlock();
    }
    log.info("loaded  {} schools to memory", this.schoolMap.values().size());
  }

  public List<School> getSchools() {
    log.info("Calling Institute api to load schools to memory");
    return this.webClient.get()
      .uri(this.props.getInstituteApiURL() + "/school")
      .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .retrieve()
      .bodyToFlux(School.class)
      .collectList()
      .block();
  }

  /**
   * Gets list of schools based on criteria.
   *
   * @return list of schools
   */
  @Retryable(retryFor = {Exception.class}, noRetryFor = {StudentDataCollectionAPIRuntimeException.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public List<School> getSchoolListGivenCriteria(List<CollectionCodeCriteriaEntity> collectionCodeCriteria, UUID correlationID) {
    try {
      final List<Search> searches = new LinkedList<>();
      var currentDate = LocalDateTime.now();

      for (CollectionCodeCriteriaEntity criteria: collectionCodeCriteria) {
        //for open schools
        final SearchCriteria openSchoolOpenDateCriteria = this.getCriteria(OPEN_DATE, FilterOperation.LESS_THAN_OR_EQUAL_TO, StringUtils.substring(currentDate.toString(),0,19), ValueType.DATE_TIME, Condition.AND);
        final SearchCriteria openSchoolCloseDateCriteria = this.getCriteria(CLOSE_DATE, FilterOperation.EQUAL, null, ValueType.STRING, Condition.AND);
        //for closing schools
        final SearchCriteria closingSchoolOpenDateCriteria = this.getCriteria(OPEN_DATE, FilterOperation.LESS_THAN_OR_EQUAL_TO, StringUtils.substring(currentDate.toString(),0,19), ValueType.DATE_TIME, Condition.AND);
        final SearchCriteria closingSchoolCloseDateCriteria = this.getCriteria(CLOSE_DATE, FilterOperation.GREATER_THAN, StringUtils.substring(currentDate.toString(),0,19), ValueType.DATE_TIME, Condition.AND);

        final SearchCriteria facilityTypeCodeCriteria = this.getCriteria(FACILITY_TYPE_CODE, FilterOperation.EQUAL, criteria.getFacilityTypeCode(), ValueType.STRING, Condition.AND);
        final SearchCriteria schoolCategoryCodeCriteria = this.getCriteria(SCHOOL_CATEGORY_CODE, FilterOperation.EQUAL, criteria.getSchoolCategoryCode(), ValueType.STRING, Condition.AND);

        final List<SearchCriteria> openSchoolCriteriaList = new LinkedList<>(Collections.singletonList(openSchoolOpenDateCriteria));
        openSchoolCriteriaList.add(openSchoolCloseDateCriteria);
        openSchoolCriteriaList.add(facilityTypeCodeCriteria);
        openSchoolCriteriaList.add(schoolCategoryCodeCriteria);

        final List<SearchCriteria> closingSchoolCriteriaList = new LinkedList<>(Collections.singletonList(closingSchoolOpenDateCriteria));
        closingSchoolCriteriaList.add(closingSchoolCloseDateCriteria);
        closingSchoolCriteriaList.add(facilityTypeCodeCriteria);
        closingSchoolCriteriaList.add(schoolCategoryCodeCriteria);

        searches.add(Search.builder().searchCriteriaList(openSchoolCriteriaList).build());
        searches.add(Search.builder().searchCriteriaList(closingSchoolCriteriaList).build());
      }

      log.trace("Sys Criteria: {}", searches);
      final TypeReference<List<School>> ref = new TypeReference<>() {
      };
      val event = Event.builder().sagaId(correlationID).eventType(EventType.GET_PAGINATED_SCHOOLS).eventPayload(SEARCH_CRITERIA_LIST.concat("=").concat(
          URLEncoder.encode(this.objectMapper.writeValueAsString(searches), StandardCharsets.UTF_8)).concat("&").concat(PAGE_SIZE).concat("=").concat("100000")).build();
      val responseMessage = this.messagePublisher.requestMessage(INSTITUTE_API_TOPIC, JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 60, TimeUnit.SECONDS).get();
      if (null != responseMessage) {
        return objectMapper.readValue(responseMessage.getData(), ref);
      } else {
        throw new StudentDataCollectionAPIRuntimeException("Either NATS timed out or the response is null , correlationID :: " + correlationID);
      }

    } catch (final Exception ex) {
      Thread.currentThread().interrupt();
      throw new StudentDataCollectionAPIRuntimeException("Either NATS timed out or the response is null , correlationID :: " + correlationID + ex.getMessage());
    }
  }

  public Optional<School> getSchoolBySchoolID(final String schoolID) {
    if (this.schoolMap.isEmpty()) {
      log.info("School map is empty reloading schools");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.schoolMap.get(schoolID));
  }

  private SearchCriteria getCriteria(final String key, final FilterOperation operation, final String value, final ValueType valueType, final Condition condition) {
    return SearchCriteria.builder().key(key).operation(operation).value(value).valueType(valueType).condition(condition).build();
  }

}
