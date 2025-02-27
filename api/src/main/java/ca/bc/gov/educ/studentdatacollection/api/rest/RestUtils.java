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
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.studentapi.v1.Student;
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
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
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
  private final Map<UUID, List<EdxUser>> edxSchoolUserMap = new ConcurrentHashMap<>();
  private final Map<UUID, List<EdxUser>> edxDistrictUserMap = new ConcurrentHashMap<>();
  private final Map<String, IndependentAuthority> authorityMap = new ConcurrentHashMap<>();
  private final Map<String, SchoolTombstone> schoolMap = new ConcurrentHashMap<>();
  private final Map<String, SchoolTombstone> schoolMincodeMap = new ConcurrentHashMap<>();
  private final Map<String, District> districtMap = new ConcurrentHashMap<>();
  private final Map<String, School> allSchoolMap = new ConcurrentHashMap<>();
  private final Map<String, FacilityTypeCode> facilityTypeCodesMap = new ConcurrentHashMap<>();
  private final Map<String, SchoolCategoryCode> schoolCategoryCodesMap = new ConcurrentHashMap<>();
  public static final String PAGE_SIZE = "pageSize";
  public static final String PAGE_SIZE_VALUE = "500";
  public static final String PAGE_NUMBER = "pageNumber";
  public static final Integer PAGE_COUNT_VALUE = 12;
  private final WebClient webClient;
  private final WebClient chesWebClient;
  private final MessagePublisher messagePublisher;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ReadWriteLock facilityTypesLock = new ReentrantReadWriteLock();
  private final ReadWriteLock edxUsersLock = new ReentrantReadWriteLock();
  private final ReadWriteLock schoolCategoriesLock = new ReentrantReadWriteLock();
  private final ReadWriteLock authorityLock = new ReentrantReadWriteLock();
  private final ReadWriteLock schoolLock = new ReentrantReadWriteLock();
  private final ReadWriteLock districtLock = new ReentrantReadWriteLock();
  private final ReadWriteLock allSchoolLock = new ReentrantReadWriteLock();
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
    this.populateAllSchoolMap();
    this.populateSchoolCategoryCodesMap();
    this.populateFacilityTypeCodesMap();
    this.populateSchoolMap();
    this.populateSchoolMincodeMap();
    this.populateDistrictMap();
    this.populateAuthorityMap();
    this.populateEdxUsersMap();
  }

  @Scheduled(cron = "${schedule.jobs.load.school.cron}")
  public void scheduled() {
    this.init();
  }

  public void populateEdxUsersMap() {
    val writeLock = this.edxUsersLock.writeLock();
    try {
      writeLock.lock();
      for (val edxUser : this.getEdxUsers()) {
        for(val edxUserSchool: edxUser.getEdxUserSchools()) {
          this.edxSchoolUserMap.computeIfAbsent(edxUserSchool.getSchoolID(), k -> new ArrayList<>()).add(edxUser);
        }
        for(val edxUserDistrict: edxUser.getEdxUserDistricts()) {
          this.edxDistrictUserMap.computeIfAbsent(edxUserDistrict.getDistrictID(), k -> new ArrayList<>()).add(edxUser);
        }
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache EDX users {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} EDX school users to memory", this.edxSchoolUserMap.values().size());
    log.info("Loaded  {} EDX district users to memory", this.edxDistrictUserMap.values().size());
  }

  public void populateAuthorityMap() {
    val writeLock = this.authorityLock.writeLock();
    try {
      writeLock.lock();
      for (val authority : this.getAuthorities()) {
        this.authorityMap.put(authority.getIndependentAuthorityId(), authority);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache authorities {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} authorities to memory", this.authorityMap.values().size());
  }

  public void populateSchoolCategoryCodesMap() {
    val writeLock = this.schoolCategoriesLock.writeLock();
    try {
      writeLock.lock();
      for (val categoryCode : this.getSchoolCategoryCodes()) {
        this.schoolCategoryCodesMap.put(categoryCode.getSchoolCategoryCode(), categoryCode);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache school categories {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} school categories to memory", this.schoolCategoryCodesMap.values().size());
  }

  public void populateFacilityTypeCodesMap() {
    val writeLock = this.facilityTypesLock.writeLock();
    try {
      writeLock.lock();
      for (val categoryCode : this.getFacilityTypeCodes()) {
        this.facilityTypeCodesMap.put(categoryCode.getFacilityTypeCode(), categoryCode);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache facility types {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} facility types to memory", this.facilityTypeCodesMap.values().size());
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

  public List<IndependentAuthority> getAuthorities() {
    log.info("Calling Institute api to load authority to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/authority")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(IndependentAuthority.class)
            .collectList()
            .block();
  }

  public List<SchoolCategoryCode> getSchoolCategoryCodes() {
    log.info("Calling Institute api to load school categories to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/category-codes")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(SchoolCategoryCode.class)
            .collectList()
            .block();
  }

  public List<FacilityTypeCode> getFacilityTypeCodes() {
    log.info("Calling Institute api to load facility type codes to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/facility-codes")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(FacilityTypeCode.class)
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

  public List<EdxUser> getEdxUsers() {
    log.info("Calling Institute api to load EDX users to memory");
    return this.webClient.get()
            .uri(this.props.getEdxApiURL() + "/users")
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
  public List<StudentMerge> getMergedStudentIds(UUID correlationID, UUID assignedStudentId) {
    try {
      final TypeReference<Event> refEventResponse = new TypeReference<>() {};
      final TypeReference<List<StudentMerge>> refMergedStudentResponse = new TypeReference<>() {};
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.GET_MERGES).eventPayload(String.valueOf(assignedStudentId)).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.PEN_SERVICES_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 120, TimeUnit.SECONDS).get();
      if (responseMessage != null) {
        val eventResponse = objectMapper.readValue(responseMessage.getData(), refEventResponse);
        return objectMapper.readValue(eventResponse.getEventPayload(), refMergedStudentResponse);
      } else {
        throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID);
      }

    } catch (final Exception ex) {
      log.error("Error occurred calling PEN SERVICES API service :: " + ex.getMessage());
      Thread.currentThread().interrupt();
      throw new StudentDataCollectionAPIRuntimeException(ex.getMessage());
    }
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

  public Optional<SchoolCategoryCode> getSchoolCategoryCode(final String schoolCategoryCode) {
    if (this.schoolCategoryCodesMap.isEmpty()) {
      log.info("School categories map is empty reloading them");
      this.populateSchoolCategoryCodesMap();
    }
    return Optional.ofNullable(this.schoolCategoryCodesMap.get(schoolCategoryCode));
  }

  public Optional<FacilityTypeCode> getFacilityTypeCode(final String facilityTypeCode) {
    if (this.facilityTypeCodesMap.isEmpty()) {
      log.info("Facility types map is empty reloading them");
      this.populateFacilityTypeCodesMap();
    }
    return Optional.ofNullable(this.facilityTypeCodesMap.get(facilityTypeCode));
  }

  public Optional<SchoolTombstone> getSchoolBySchoolID(final String schoolID) {
    if (this.schoolMap.isEmpty()) {
      log.info("School map is empty reloading schools");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.schoolMap.get(schoolID));
  }

  public Optional<IndependentAuthority> getAuthorityByAuthorityID(final String authorityID) {
    if (this.authorityMap.isEmpty()) {
      log.info("Authority map is empty reloading authorities");
      this.populateAuthorityMap();
    }
    return Optional.ofNullable(this.authorityMap.get(authorityID));
  }

  public Optional<SchoolTombstone> getSchoolByMincode(final String mincode) {
    if (this.schoolMincodeMap.isEmpty()) {
      log.info("School mincode map is empty reloading schools");
      this.populateSchoolMincodeMap();
    }
    return Optional.ofNullable(this.schoolMincodeMap.get(mincode));
  }

  public List<IndependentSchoolFundingGroup> getSchoolFundingGroupsBySchoolID(final String schoolID) {
    if (this.allSchoolMap.isEmpty()) {
      log.info("All schools map is empty reloading schools");
      this.populateAllSchoolMap();
    }
    if(!this.allSchoolMap.containsKey(schoolID)){
      return new ArrayList<>();
    }
    return this.allSchoolMap.get(schoolID).getSchoolFundingGroups();
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

  @Retryable(retryFor = {Exception.class}, noRetryFor = {SagaRuntimeException.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Student getStudentByPEN(UUID correlationID, String assignedPEN) {
    try {
      final TypeReference<Student> refPenMatchResult = new TypeReference<>() {
      };
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.GET_STUDENT).eventPayload(assignedPEN).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.STUDENT_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 120, TimeUnit.SECONDS).get();
      if (responseMessage != null) {
        return objectMapper.readValue(responseMessage.getData(), refPenMatchResult);
      } else {
        throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID);
      }

    } catch (final Exception ex) {
      log.error("Error occurred calling GET STUDENT service :: " + ex.getMessage());
      Thread.currentThread().interrupt();
      throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID + ex.getMessage());
    }
  }

  public void populateAllSchoolMap() {
    val writeLock = this.allSchoolLock.writeLock();
    try {
      writeLock.lock();
      log.info("Populating all school map :: {}", this.allSchoolLock);
//      List<School> pageOneOfSchools = this.getAllSchoolList(UUID.randomUUID(),"0");
//      log.info("Populating all school map page 1 size:: {}", pageOneOfSchools.size());
//      List<School> pageTwoOfSchools = this.getAllSchoolList(UUID.randomUUID(), "1");
//      log.info("Populating all school map page 2 size:: {}", pageTwoOfSchools.size());
//      List<School> pageThreeOfSchools = this.getAllSchoolList(UUID.randomUUID(), "2");
//      log.info("Populating all school map page 3 size:: {}", pageThreeOfSchools.size());
//      List<School> pageFourOfSchools = this.getAllSchoolList(UUID.randomUUID(), "3");
//      log.info("Populating all school map page 4 size:: {}", pageFourOfSchools.size());

      List<CompletableFuture<List<School>>> pageFutures = new ArrayList<>();

      for (int i = 0; i < PAGE_COUNT_VALUE; i++) {
        final String pageNumber = String.valueOf(i);
        CompletableFuture<List<School>> future = CompletableFuture.supplyAsync(() -> getAllSchoolList(UUID.randomUUID(), pageNumber));
        pageFutures.add(future);
        log.info("populated all school map - future number  :: {}", pageNumber);
      }

//      List<School> allSchools = Stream.of(pageOneOfSchools, pageTwoOfSchools, pageThreeOfSchools, pageFourOfSchools)
//              .flatMap(Collection::stream).toList();


      log.info("joining all pages");
      CompletableFuture<Void> allPages = CompletableFuture.allOf(pageFutures.toArray(new CompletableFuture[0]));
      allPages.join();

      log.info("page futures to all schools");
      List<School> allSchools = pageFutures.stream()
              .map(CompletableFuture::join)
              .flatMap(Collection::stream)
              .toList();

      log.info("putting all schools into all schools map");
      allSchools.forEach(school -> this.allSchoolMap.put(school.getSchoolId(), school));

//      for (val school : allSchools) {
//        this.allSchoolMap.put(school.getSchoolId(), school);
//      }
    } catch (Exception ex) {
      log.error("Unable to load map cache for allSchool {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} allSchools to memory", this.allSchoolMap.values().size());
  }

  public Optional<School> getAllSchoolBySchoolID(final String schoolID) {
    if (this.allSchoolMap.isEmpty()) {
      log.info("All School map is empty reloading schools");
      this.populateAllSchoolMap();
    }
    return Optional.ofNullable(this.allSchoolMap.get(schoolID));
  }

  public List<EdxUser> getEdxUsersForSchool(final UUID schoolID) {
    if (this.edxSchoolUserMap.isEmpty()) {
      log.info("EDX users school map is empty reloading schools");
      this.populateEdxUsersMap();
    }

    var users = this.edxSchoolUserMap.get(schoolID);
    return users != null ? users : new ArrayList<>();
  }

  public List<EdxUser> getEdxUsersForDistrict(final UUID districtID) {
    if (this.edxDistrictUserMap.isEmpty()) {
      log.info("EDX users district map is empty reloading schools");
      this.populateEdxUsersMap();
    }
    var users = this.edxDistrictUserMap.get(districtID);
    return users != null ? users : new ArrayList<>();
  }

  @Retryable(retryFor = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public List<School> getAllSchoolList(UUID correlationID, String pageNumber) {
    try {
      log.info("Calling Institute API to load all schools to memory, current page " + (Integer.parseInt(pageNumber) + 1) + " of 12");
      final TypeReference<List<School>> ref = new TypeReference<>() {
      };
      val event = Event.builder().sagaId(correlationID).eventType(EventType.GET_PAGINATED_SCHOOLS).eventPayload(PAGE_SIZE.concat("=").concat(PAGE_SIZE_VALUE)
              .concat("&").concat(PAGE_NUMBER).concat("=").concat(pageNumber)).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.INSTITUTE_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 30, TimeUnit.SECONDS).get();
      if (null != responseMessage) {
        log.info("Response from Institute API is good");
        return objectMapper.readValue(responseMessage.getData(), ref);
      } else {
        log.info("Response from Institute API returned empty list");
        throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID);
      }
    } catch (final Exception ex) {
      log.error("Error getting all schools list: ", ex);
      throw new StudentDataCollectionAPIRuntimeException(NATS_TIMEOUT + correlationID + ex);
    }
  }
}
