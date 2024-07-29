package ca.bc.gov.educ.studentdatacollection.api.rest;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.PenMatchSagaMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeCriteriaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.struct.CHESEmail;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.grad.v1.GradStatusResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
  public static final String NATS_TIMEOUT = "Either NATS timed out or the response is null , correlationID :: ";
  public static final String SCHOOL_REPORTING_REQUIREMENT_CODE = "schoolReportingRequirementCode";
  public static final String FACILITY_TYPE_CODE = "facilityTypeCode";
  public static final String OPEN_DATE = "openedDate";
  public static final String CLOSE_DATE = "closedDate";
  private static final String CONTENT_TYPE = "Content-Type";
  private final Map<String, SchoolTombstone> schoolMap = new ConcurrentHashMap<>();
  private final Map<String, SchoolTombstone> schoolMincodeMap = new ConcurrentHashMap<>();
  private final Map<String, District> districtMap = new ConcurrentHashMap<>();
  public static final String PAGE_SIZE = "pageSize";
  private final WebClient webClient;
  private final WebClient chesWebClient;
  private final MessagePublisher messagePublisher;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ReadWriteLock schoolLock = new ReentrantReadWriteLock();
  private final ReadWriteLock districtLock = new ReentrantReadWriteLock();
  @Getter
  private final ApplicationProperties props;

  @Value("${initialization.background.enabled}")
  private Boolean isBackgroundInitializationEnabled;

  private final Map<String, List<UUID>> independentAuthorityToSchoolIDMap = new ConcurrentHashMap<>();

  @Autowired
  public RestUtils(@Qualifier("chesWebClient") final WebClient chesWebClient, WebClient webClient, final ApplicationProperties props, final MessagePublisher messagePublisher) {
    this.webClient = webClient;
    this.chesWebClient = chesWebClient;
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
    this.populateSchoolMincodeMap();
    this.populateDistrictMap();
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
        if (StringUtils.isNotBlank(school.getIndependentAuthorityId())) {
          this.independentAuthorityToSchoolIDMap.computeIfAbsent(school.getIndependentAuthorityId(), k -> new ArrayList<>()).add(UUID.fromString(school.getSchoolId()));
        }
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache school {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} schools to memory", this.schoolMap.values().size());
  }

  public void populateSchoolMincodeMap() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      for (val school : this.getSchools()) {
        this.schoolMincodeMap.put(school.getMincode(), school);
        if (StringUtils.isNotBlank(school.getIndependentAuthorityId())) {
          this.independentAuthorityToSchoolIDMap.computeIfAbsent(school.getIndependentAuthorityId(), k -> new ArrayList<>()).add(UUID.fromString(school.getSchoolId()));
        }
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache school mincodes {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} school mincodes to memory", this.schoolMincodeMap.values().size());
  }

  public List<SchoolTombstone> getSchools() {
    log.info("Calling Institute api to load schools to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/school")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(SchoolTombstone.class)
            .collectList()
            .block();
  }

  public School getSchoolDetails(UUID schoolID) {
    log.debug("Retrieving school by ID: {}", schoolID);
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/school/" + schoolID)
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(School.class)
            .blockFirst();
  }

  public List<EdxUser> get1701Users(UUID schoolID, UUID districtID) {
    log.debug("Retrieving users for school: {}", schoolID);
    String url;
    if (schoolID != null && districtID == null) {
      url = this.props.getEdxApiURL() + "/users?schoolID=" + schoolID;
    } else if (districtID != null && schoolID == null) {
      url = this.props.getEdxApiURL() + "/users?districtID=" + districtID;
    } else {
      return null;
    }
    return this.webClient.get()
            .uri(url)
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(EdxUser.class)
            .collectList()
            .block();
  }

  public void populateDistrictMap() {
    val writeLock = this.districtLock.writeLock();
    try {
      writeLock.lock();
      for (val district : this.getDistricts()) {
        this.districtMap.put(district.getDistrictId(), district);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache district {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} districts to memory", this.districtMap.values().size());
  }

  public List<District> getDistricts() {
    log.info("Calling Institute api to load districts to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/district")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(District.class)
            .collectList()
            .block();
  }

  @Retryable(retryFor = {Exception.class}, noRetryFor = {SagaRuntimeException.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public PenMatchResult getPenMatchResult(UUID correlationID, SdcSchoolCollectionStudentEntity sdcSchoolStudent, String mincode) {
    try {
      val penMatchRequest = PenMatchSagaMapper.mapper.toPenMatchStudent(sdcSchoolStudent, mincode);
      penMatchRequest.setDob(StringUtils.replace(penMatchRequest.getDob(), "-", "")); // pen-match api expects yyyymmdd
      val penMatchRequestJson = JsonUtil.mapper.writeValueAsString(penMatchRequest);
      final TypeReference<Event> refEventResponse = new TypeReference<>() {
      };
      final TypeReference<PenMatchResult> refPenMatchResult = new TypeReference<>() {
      };
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.PROCESS_PEN_MATCH).eventPayload(penMatchRequestJson).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.PEN_MATCH_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 120, TimeUnit.SECONDS).get();
      if (responseMessage != null) {
        val eventResponse = objectMapper.readValue(responseMessage.getData(), refEventResponse);
        val penMatchResult = objectMapper.readValue(eventResponse.getEventPayload(), refPenMatchResult);
        log.debug("PEN Match Result is :: " + penMatchResult);
        return penMatchResult;
      } else {
        throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID);
      }

    } catch (final Exception ex) {
      log.error("Error occurred calling PEN Match service :: " + ex.getMessage());
      Thread.currentThread().interrupt();
      throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID + ex.getMessage());
    }
  }

  @Retryable(retryFor = {Exception.class}, noRetryFor = {SagaRuntimeException.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public GradStatusResult getGradStatusResult(UUID correlationID, SdcSchoolCollectionStudent sdcSchoolStudent) {
    try {
      val gradStatusJSON = JsonUtil.mapper.writeValueAsString(sdcSchoolStudent.getAssignedStudentId());
      final TypeReference<GradStatusResult> ref = new TypeReference<>() {
      };
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.FETCH_GRAD_STATUS).eventPayload(gradStatusJSON).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.GRAD_STUDENT_API_FETCH_GRAD_STATUS_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 60, TimeUnit.SECONDS).get();
      if (responseMessage != null) {
        log.debug("Grad status Payload is :: " + responseMessage);
        return objectMapper.readValue(responseMessage.getData(), ref);
      } else {
        throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID);
      }

    } catch (final Exception ex) {
      log.error("Error occurred calling Grad service :: " + ex);
      Thread.currentThread().interrupt();
      throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID + ex.getMessage());
    }
  }

  /**
   * Gets list of schools based on criteria.
   *
   * @return list of schools
   */
  @Retryable(retryFor = {Exception.class}, noRetryFor = {StudentDataCollectionAPIRuntimeException.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public List<SchoolTombstone> getSchoolListGivenCriteria(List<CollectionCodeCriteriaEntity> collectionCodeCriteria, UUID correlationID) {
    try {
      final List<Search> searches = new LinkedList<>();
      var currentDate = LocalDateTime.now();

      for (CollectionCodeCriteriaEntity criteria : collectionCodeCriteria) {
        //for open schools
        final SearchCriteria openSchoolOpenDateCriteria = this.getCriteria(OPEN_DATE, FilterOperation.LESS_THAN_OR_EQUAL_TO, StringUtils.substring(currentDate.toString(), 0, 19), ValueType.DATE_TIME, Condition.AND);
        final SearchCriteria openSchoolCloseDateCriteria = this.getCriteria(CLOSE_DATE, FilterOperation.EQUAL, null, ValueType.STRING, Condition.AND);
        //for closing schools
        final SearchCriteria closingSchoolOpenDateCriteria = this.getCriteria(OPEN_DATE, FilterOperation.LESS_THAN_OR_EQUAL_TO, StringUtils.substring(currentDate.toString(), 0, 19), ValueType.DATE_TIME, Condition.AND);
        final SearchCriteria closingSchoolCloseDateCriteria = this.getCriteria(CLOSE_DATE, FilterOperation.GREATER_THAN, StringUtils.substring(currentDate.toString(), 0, 19), ValueType.DATE_TIME, Condition.AND);

        final SearchCriteria facilityTypeCodeCriteria = this.getCriteria(FACILITY_TYPE_CODE, FilterOperation.EQUAL, criteria.getFacilityTypeCode(), ValueType.STRING, Condition.AND);
        final SearchCriteria schoolCategoryCodeCriteria = this.getCriteria(SCHOOL_CATEGORY_CODE, FilterOperation.EQUAL, criteria.getSchoolCategoryCode(), ValueType.STRING, Condition.AND);
        final SearchCriteria schoolReportingRequirementCodeCriteria = this.getCriteria(SCHOOL_REPORTING_REQUIREMENT_CODE, FilterOperation.EQUAL, criteria.getReportingRequirementCode(), ValueType.STRING, Condition.AND);

        final List<SearchCriteria> openSchoolCriteriaList = new LinkedList<>(Collections.singletonList(openSchoolOpenDateCriteria));
        openSchoolCriteriaList.add(openSchoolCloseDateCriteria);
        openSchoolCriteriaList.add(facilityTypeCodeCriteria);
        openSchoolCriteriaList.add(schoolCategoryCodeCriteria);
        openSchoolCriteriaList.add(schoolReportingRequirementCodeCriteria);

        final List<SearchCriteria> closingSchoolCriteriaList = new LinkedList<>(Collections.singletonList(closingSchoolOpenDateCriteria));
        closingSchoolCriteriaList.add(closingSchoolCloseDateCriteria);
        closingSchoolCriteriaList.add(facilityTypeCodeCriteria);
        closingSchoolCriteriaList.add(schoolCategoryCodeCriteria);
        closingSchoolCriteriaList.add(schoolReportingRequirementCodeCriteria);

        searches.add(Search.builder().searchCriteriaList(openSchoolCriteriaList).build());
        searches.add(Search.builder().searchCriteriaList(closingSchoolCriteriaList).build());
      }

      log.trace("Sys Criteria: {}", searches);
      final TypeReference<List<SchoolTombstone>> ref = new TypeReference<>() {
      };
      val event = Event.builder().sagaId(correlationID).eventType(EventType.GET_PAGINATED_SCHOOLS).eventPayload(SEARCH_CRITERIA_LIST.concat("=").concat(
              URLEncoder.encode(this.objectMapper.writeValueAsString(searches), StandardCharsets.UTF_8)).concat("&").concat(PAGE_SIZE).concat("=").concat("100000")).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.INSTITUTE_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 60, TimeUnit.SECONDS).get();
      if (null != responseMessage) {
        return objectMapper.readValue(responseMessage.getData(), ref);
      } else {
        throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID);
      }

    } catch (final Exception ex) {
      Thread.currentThread().interrupt();
      throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID + ex.getMessage());
    }
  }

  public Optional<SchoolTombstone> getSchoolBySchoolID(final String schoolID) {
    if (this.schoolMap.isEmpty()) {
      log.info("School map is empty reloading schools");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.schoolMap.get(schoolID));
  }

  public Optional<SchoolTombstone> getSchoolByMincode(final String mincode) {
    if (this.schoolMincodeMap.isEmpty()) {
      log.info("School mincode map is empty reloading schools");
      this.populateSchoolMincodeMap();
    }
    return Optional.ofNullable(this.schoolMincodeMap.get(mincode));
  }

  public void sendEmail(final String fromEmail, final List<String> toEmail, final String body, final String subject) {
    this.sendEmail(this.getChesEmail(fromEmail, toEmail, body, subject));
  }

  public void sendEmail(final CHESEmail chesEmail) {
    this.chesWebClient
            .post()
            .uri(this.props.getChesEndpointURL())
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(Mono.just(chesEmail), CHESEmail.class)
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(error -> this.logError(error, chesEmail))
            .doOnSuccess(success -> this.onSendEmailSuccess(success, chesEmail))
            .block();
  }

  private void logError(final Throwable throwable, final CHESEmail chesEmailEntity) {
    log.error("Error from CHES API call :: {} ", chesEmailEntity, throwable);
  }

  private void onSendEmailSuccess(final String s, final CHESEmail chesEmailEntity) {
    log.info("Email sent success :: {} :: {}", chesEmailEntity, s);
  }

  public CHESEmail getChesEmail(final String fromEmail, final List<String> toEmail, final String body, final String subject) {
    final CHESEmail chesEmail = new CHESEmail();
    chesEmail.setBody(body);
    chesEmail.setBodyType("html");
    chesEmail.setDelayTS(0);
    chesEmail.setEncoding("utf-8");
    chesEmail.setFrom(fromEmail);
    chesEmail.setPriority("normal");
    chesEmail.setSubject(subject);
    chesEmail.setTag("tag");
    chesEmail.getTo().addAll(toEmail);
    return chesEmail;
  }

  public Optional<District> getDistrictByDistrictID(final String districtID) {
    if (this.districtMap.isEmpty()) {
      log.info("District map is empty reloading schools");
      this.populateDistrictMap();
    }
    return Optional.ofNullable(this.districtMap.get(districtID));
  }

  private SearchCriteria getCriteria(final String key, final FilterOperation operation, final String value, final ValueType valueType, final Condition condition) {
    return SearchCriteria.builder().key(key).operation(operation).value(value).valueType(valueType).condition(condition).build();
  }

  public Optional<List<UUID>> getSchoolIDsByIndependentAuthorityID(final String independentAuthorityID) {
    if (this.independentAuthorityToSchoolIDMap.isEmpty()) {
      log.info("The map is empty reloading schools");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.independentAuthorityToSchoolIDMap.get(independentAuthorityID));
  }
}
