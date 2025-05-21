package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorChainProcessor;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.helpers.SdcHelper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcStudentEllMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentValidationIssueRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcStudentEllRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityRulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.rules.RulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.UpdateStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil.isCollectionInProvDupes;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentService {

  public static final String SDC_SCHOOL_COLLECTION_STUDENT_WAS_NOT_SAVED = "SdcSchoolCollectionStudent was not saved to the database because it has errors :: {}";
  private final MessagePublisher messagePublisher;

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  private final SdcStudentEllRepository sdcStudentEllRepository;

  private final SdcSchoolCollectionStudentStorageService sdcSchoolCollectionStudentStorageService;

  private final SdcDuplicatesService sdcDuplicatesService;

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  private final FteCalculatorChainProcessor fteCalculatorChainProcessor;

  private final ProgramEligibilityRulesProcessor programEligibilityRulesProcessor;

  private final SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository;

  private final RestUtils restUtils;

  private static SdcStudentEllMapper sdcStudentEllMapper = SdcStudentEllMapper.mapper;

  private static final String IN_REVIEW = "INREVIEW";

  private static final String SDC_SCHOOL_COLLECTION_STUDENT_STRING = "SdcSchoolCollectionStudentEntity";

  private static final String EVENT_EMPTY_MSG = "Event String is empty, skipping the publish to topic :: {}";

  private final RulesProcessor rulesProcessor;

  private static final String SDC_SCHOOL_COLLECTION_STUDENT_ID = "sdcSchoolCollectionStudentId";

  public SdcSchoolCollectionStudentEntity getSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    return sdcSchoolCollectionStudentEntityOptional.orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudent.class, SDC_SCHOOL_COLLECTION_STUDENT_ID, sdcSchoolCollectionStudentID.toString()));
  }

  public void publishUnprocessedStudentRecordsForProcessing(final List<SdcStudentSagaData> sdcStudentSagaDatas) {
    sdcStudentSagaDatas.forEach(this::sendIndividualStudentAsMessageToTopic);
  }

  public void publishUnprocessedMigratedStudentRecordsForProcessing(final List<SdcStudentSagaData> sdcStudentSagaDatas) {
    sdcStudentSagaDatas.forEach(this::sendIndividualMigratedStudentAsMessageToTopic);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processSagaStudentRecord(final UUID sdcSchoolCollectionStudentID, SchoolTombstone schoolTombstone) {
    var currentStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    if(currentStudentEntity.isPresent()) {
      sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(processStudentRecord(schoolTombstone, currentStudentEntity.get(), true));
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, sdcSchoolCollectionStudentID.toString());
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processSagaStudentMigrationRecord(final UUID sdcSchoolCollectionStudentID, SchoolTombstone schoolTombstone) {
    var currentStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    if(currentStudentEntity.isPresent()) {
      this.sdcSchoolCollectionStudentRepository.save(processMigratedStudentRecord(schoolTombstone, currentStudentEntity.get()));
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, sdcSchoolCollectionStudentID.toString());
    }
  }

  public SdcSchoolCollectionStudentEntity processStudentRecord(SchoolTombstone schoolTombstone, SdcSchoolCollectionStudentEntity incomingStudentEntity, boolean wipePENMatch) {
    StudentRuleData studentRuleData = new StudentRuleData();
    studentRuleData.setSdcSchoolCollectionStudentEntity(incomingStudentEntity);
    studentRuleData.setSchool(schoolTombstone);

    TransformUtil.clearCalculatedFields(incomingStudentEntity, wipePENMatch);
    var validationErrors = validateStudent(studentRuleData);
    if(validationErrors.stream().noneMatch(issueValue -> issueValue.getValidationIssueSeverityCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString()))){
      calculateAdditionalStudentAttributes(studentRuleData);
    }

    return studentRuleData.getSdcSchoolCollectionStudentEntity();
  }

  public SdcSchoolCollectionStudentEntity processMigratedStudentRecord(SchoolTombstone schoolTombstone, SdcSchoolCollectionStudentEntity incomingStudentEntity) {
    StudentRuleData studentRuleData = new StudentRuleData();
    studentRuleData.setSdcSchoolCollectionStudentEntity(incomingStudentEntity);
    studentRuleData.setSchool(schoolTombstone);
    studentRuleData.setMigratedStudent(true);

    // Update program eligibility
    List<ProgramEligibilityIssueCode> programEligibilityErrors = this.programEligibilityRulesProcessor.processRules(studentRuleData);
    updateProgramEligibilityColumns(programEligibilityErrors, studentRuleData.getSdcSchoolCollectionStudentEntity());

    // Convert number of courses string to decimal
    if(StringUtils.isNotBlank(incomingStudentEntity.getNumberOfCourses())){
      convertNumOfCourses(incomingStudentEntity);
    }
    studentRuleData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.VERIFIED.getCode());
    return studentRuleData.getSdcSchoolCollectionStudentEntity();
  }

  private List<SdcSchoolCollectionStudentValidationIssue> validateStudent(final StudentRuleData studentRuleData){
    val validationErrors = this.rulesProcessor.processRules(studentRuleData);
    var entity = studentRuleData.getSdcSchoolCollectionStudentEntity();
    entity.getSDCStudentValidationIssueEntities().clear();
    entity.getSDCStudentValidationIssueEntities().addAll(SdcHelper.populateValidationErrors(validationErrors, entity));
    if(validationErrors.stream().anyMatch(val -> val.getValidationIssueSeverityCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString()))){
      entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.getCode());
    } else if(validationErrors.stream().anyMatch(val -> val.getValidationIssueSeverityCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.FUNDING_WARNING.toString()))) {
      entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.FUNDING_WARNING.getCode());
    } else if(validationErrors.stream().anyMatch(val -> val.getValidationIssueSeverityCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.INFO_WARNING.toString()))) {
      entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.INFO_WARNING.getCode());
    } else{
      entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.VERIFIED.getCode());
    }
    return validationErrors;
  }

  private void calculateAdditionalStudentAttributes(final StudentRuleData studentRuleData) {
    var sdcSchoolCollectionStudentEntity = studentRuleData.getSdcSchoolCollectionStudentEntity();
    sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().clear();

    // Write Enrolled Program Codes
    if(StringUtils.isNotBlank(sdcSchoolCollectionStudentEntity.getEnrolledProgramCodes())) {
      List<String> enrolledProgramList = TransformUtil.splitIntoChunks(sdcSchoolCollectionStudentEntity.getEnrolledProgramCodes(), 2);
      writeEnrolledProgramCodes(sdcSchoolCollectionStudentEntity, enrolledProgramList);
    }

    // Update program eligibility
    clearSdcSchoolStudentProgramEligibilityColumns(sdcSchoolCollectionStudentEntity);
    List<ProgramEligibilityIssueCode> programEligibilityErrors = this.programEligibilityRulesProcessor.processRules(studentRuleData);
    updateProgramEligibilityColumns(programEligibilityErrors, studentRuleData.getSdcSchoolCollectionStudentEntity());

    // Convert number of courses string to decimal
    if(StringUtils.isNotBlank(sdcSchoolCollectionStudentEntity.getNumberOfCourses())){
      convertNumOfCourses(sdcSchoolCollectionStudentEntity);
    }else{
      sdcSchoolCollectionStudentEntity.setNumberOfCoursesDec(new BigDecimal(0));
      sdcSchoolCollectionStudentEntity.setNumberOfCourses("0");
    }

    // Calculate Fte
    var fteResults = this.fteCalculatorChainProcessor.processFteCalculator(studentRuleData);
    updateFteColumns(fteResults, sdcSchoolCollectionStudentEntity);
  }

  @Async("publisherExecutor")
  @Transactional
  public void prepareAndSendSdcStudentsForFurtherProcessing(final List<SdcSchoolCollectionStudentEntity> sdcStudentEntities) {
    final List<SdcStudentSagaData> sdcStudentSagaDatas = sdcStudentEntities.stream()
      .map(el -> {
        val sdcStudentSagaData = new SdcStudentSagaData();
        if(el.getSdcSchoolCollection() != null) {
          var sdcSchoolCollection = el.getSdcSchoolCollection();
          var school = this.restUtils.getSchoolBySchoolID(sdcSchoolCollection.getSchoolID().toString());
          sdcStudentSagaData.setCollectionTypeCode(sdcSchoolCollection.getCollectionEntity().getCollectionTypeCode());
          sdcStudentSagaData.setSchool(school.get());
        }
        sdcStudentSagaData.setSdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(el));
        return sdcStudentSagaData;
      }).toList();
    this.publishUnprocessedStudentRecordsForProcessing(sdcStudentSagaDatas);
  }

  @Async("publisherExecutor")
  public void prepareAndSendMigratedSdcStudentsForFurtherProcessing(final List<SdcStudentSagaData> sdcStudentSagaDatas) {
    this.publishUnprocessedMigratedStudentRecordsForProcessing(sdcStudentSagaDatas);
  }

  @Async("publisherExecutor")
  public void prepareStudentsForDemogUpdate(final List<SdcSchoolCollectionStudentEntity> sdcStudentEntities) {
    CollectionEntity collection = sdcStudentEntities.get(0).getSdcSchoolCollection().getCollectionEntity();

    final List<UpdateStudentSagaData> updateStudentSagas = sdcStudentEntities.stream()
            .map(el -> {
              val updateStudentSagaData = new UpdateStudentSagaData();
              var school = this.restUtils.getSchoolBySchoolID(el.getSdcSchoolCollection().getSchoolID().toString());

              updateStudentSagaData.setDob(el.getDob());
              updateStudentSagaData.setSexCode(el.getGender());
              updateStudentSagaData.setGenderCode(el.getGender());
              updateStudentSagaData.setUsualFirstName(el.getUsualFirstName());
              updateStudentSagaData.setUsualLastName(el.getUsualLastName());
              updateStudentSagaData.setUsualMiddleNames(el.getUsualMiddleNames());
              updateStudentSagaData.setPostalCode(el.getPostalCode());
              updateStudentSagaData.setLocalID(el.getLocalID());
              updateStudentSagaData.setGradeCode(el.getEnrolledGradeCode());
              updateStudentSagaData.setMincode(school.get().getMincode());
              updateStudentSagaData.setSdcSchoolCollectionStudentID(el.getSdcSchoolCollectionStudentID().toString());
              updateStudentSagaData.setAssignedPEN(el.getAssignedPen());
              updateStudentSagaData.setAssignedStudentID(el.getAssignedStudentId() != null ? el.getAssignedStudentId().toString() : null);
              updateStudentSagaData.setNumberOfCourses(el.getNumberOfCourses());
              updateStudentSagaData.setCollectionID(el.getSdcSchoolCollection().getCollectionEntity().getCollectionID().toString());
              updateStudentSagaData.setCollectionTypeCode(el.getSdcSchoolCollection().getCollectionEntity().getCollectionTypeCode());

              return updateStudentSagaData;

            }).toList();

    if(!Objects.equals(collection.getCollectionTypeCode(), CollectionTypeCodes.JULY.getTypeCode())) {
      publishStudentRecordsForDemogUpdate(updateStudentSagas);
    } else {
      publishStudentRecordsForStatusUpdate(updateStudentSagas);
    }
  }

  public void publishStudentRecordsForDemogUpdate(final List<UpdateStudentSagaData> updateStudentSagas) {
    updateStudentSagas.forEach(this::sendStudentRecordsForDemogUpdateAsMessageToTopic);
  }

  public void publishStudentRecordsForStatusUpdate(final List<UpdateStudentSagaData> students) {
    students.forEach(this::sendStudentRecordsStatusUpdateMessageToTopic);
  }

  private void sendStudentRecordsForDemogUpdateAsMessageToTopic(final UpdateStudentSagaData updateStudentSagaData) {
    try {
      final var eventPayload = JsonUtil.getJsonString(updateStudentSagaData);
      final Event event = Event.builder().eventType(EventType.UPDATE_STUDENTS_DEMOG_DOWNSTREAM).eventOutcome(EventOutcome.STUDENTS_DEMOG_UPDATED).eventPayload(eventPayload).sdcSchoolStudentID(updateStudentSagaData.getSdcSchoolCollectionStudentID()).build();
      final var eventString = JsonUtil.getJsonString(event);
      this.messagePublisher.dispatchMessage(TopicsEnum.UPDATE_STUDENT_DOWNSTREAM_TOPIC.toString(), eventString.getBytes());
    }
    catch(JsonProcessingException e){
      log.error(EVENT_EMPTY_MSG, updateStudentSagaData);
    }
  }

  private void sendStudentRecordsStatusUpdateMessageToTopic(final UpdateStudentSagaData updateStudentSagaData) {
    try {
      final var eventPayload = JsonUtil.getJsonString(updateStudentSagaData);
      final Event event = Event.builder().eventType(EventType.UPDATE_SDC_STUDENT_STATUS).eventOutcome(EventOutcome.SDC_STUDENT_STATUS_UPDATED).eventPayload(eventPayload).sdcSchoolStudentID(String.valueOf(updateStudentSagaData.getSdcSchoolCollectionStudentID())).build();
      final var eventString = JsonUtil.getJsonString(event);
      this.messagePublisher.dispatchMessage(TopicsEnum.UPDATE_STUDENT_STATUS_TOPIC.toString(), eventString.getBytes());
    } catch(JsonProcessingException e){
      log.error(EVENT_EMPTY_MSG, updateStudentSagaData);
    }
  }

  private void sendIndividualStudentAsMessageToTopic(final SdcStudentSagaData sdcStudentSagaData) {
    try{
      final var eventPayload = JsonUtil.getJsonString(sdcStudentSagaData);
      final Event event = Event.builder().eventType(EventType.READ_FROM_TOPIC).eventOutcome(EventOutcome.READ_FROM_TOPIC_SUCCESS).eventPayload(eventPayload).sdcSchoolStudentID(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID()).build();
      final var eventString = JsonUtil.getJsonString(event);
      this.messagePublisher.dispatchMessage(TopicsEnum.STUDENT_DATA_COLLECTION_API_TOPIC.toString(), eventString.getBytes());
    } catch(JsonProcessingException e){
      log.error(EVENT_EMPTY_MSG, sdcStudentSagaData);
    }
  }

  private void sendIndividualMigratedStudentAsMessageToTopic(final SdcStudentSagaData sdcStudentSagaData) {
    try{
      final var eventPayload = JsonUtil.getJsonString(sdcStudentSagaData);
      final Event event = Event.builder().eventType(EventType.PROCESS_SDC_MIGRATION_STUDENT).eventOutcome(EventOutcome.STUDENT_MIGRATION_PROCESSED).eventPayload(eventPayload).sdcSchoolStudentID(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID()).build();
      final var eventString = JsonUtil.getJsonString(event);
      this.messagePublisher.dispatchMessage(TopicsEnum.MIGRATE_STUDENT_TOPIC.toString(), eventString.getBytes());
    } catch(JsonProcessingException e){
      log.error(EVENT_EMPTY_MSG, sdcStudentSagaData);
    }
  }

  public void clearSdcSchoolStudentProgramEligibilityColumns(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity) {
    sdcSchoolCollectionStudentEntity.setFrenchProgramNonEligReasonCode(null);
    sdcSchoolCollectionStudentEntity.setEllNonEligReasonCode(null);
    sdcSchoolCollectionStudentEntity.setIndigenousSupportProgramNonEligReasonCode(null);
    sdcSchoolCollectionStudentEntity.setCareerProgramNonEligReasonCode(null);
    sdcSchoolCollectionStudentEntity.setSpecialEducationNonEligReasonCode(null);
  }

  public void writeEnrolledProgramCodes(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity, List<String> enrolledProgramCodes) {
    enrolledProgramCodes.forEach(enrolledProgramCode -> {
      var enrolledProgramEntity = new SdcSchoolCollectionStudentEnrolledProgramEntity();
      enrolledProgramEntity.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentEntity);
      enrolledProgramEntity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      enrolledProgramEntity.setUpdateDate(LocalDateTime.now());
      enrolledProgramEntity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      enrolledProgramEntity.setCreateDate(LocalDateTime.now());
      enrolledProgramEntity.setEnrolledProgramCode(enrolledProgramCode);
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().add(enrolledProgramEntity);
    });
  }

  public List<SdcSchoolCollectionStudentValidationIssueErrorWarningCount> errorAndWarningCountBySdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
    List<ICountValidationIssuesBySeverityCode> issues = sdcSchoolCollectionStudentRepository.getCountByValidationIssueSeverityCodeAndSdcSchoolCollectionID(sdcSchoolCollectionID);
    return issues.stream().map(issue -> new SdcSchoolCollectionStudentValidationIssueErrorWarningCount(issue.getSeverityCode(), issue.getTotal())).toList();
  }

  public void updateProgramEligibilityColumns(List<ProgramEligibilityIssueCode> errors, SdcSchoolCollectionStudentEntity student) {
    Optional<ProgramEligibilityIssueCode> baseProgramEligibilityFailure = getBaseProgramEligibilityFailure(errors);

    if (baseProgramEligibilityFailure.isPresent()) {
      String reasonCode = baseProgramEligibilityFailure.get().getCode();
      student.setFrenchProgramNonEligReasonCode(reasonCode);
      student.setEllNonEligReasonCode(reasonCode);
      student.setIndigenousSupportProgramNonEligReasonCode(reasonCode);
      student.setCareerProgramNonEligReasonCode(reasonCode);
      student.setSpecialEducationNonEligReasonCode(reasonCode);
    } else {
      student.setFrenchProgramNonEligReasonCode(getReasonCode(errors, ProgramEligibilityTypeCode.FRENCH));
      student.setEllNonEligReasonCode(getReasonCode(errors, ProgramEligibilityTypeCode.ELL));
      student.setIndigenousSupportProgramNonEligReasonCode(getReasonCode(errors, ProgramEligibilityTypeCode.IND_SUPPORT));
      student.setCareerProgramNonEligReasonCode(getReasonCode(errors, ProgramEligibilityTypeCode.CAREER_PROGRAMS));
      student.setSpecialEducationNonEligReasonCode(getReasonCode(errors, ProgramEligibilityTypeCode.SPED));
    }
  }

  private String getReasonCode(List<ProgramEligibilityIssueCode> errors, ProgramEligibilityTypeCode typeCode){
    var first = errors.stream().filter(programEligibilityIssueCode -> programEligibilityIssueCode.getProgramEligibilityTypeCode().getCode().equalsIgnoreCase(typeCode.getCode())).findFirst();
    return first.isPresent() ? first.get().getCode() : null;
  }

  public static Optional<ProgramEligibilityIssueCode> getBaseProgramEligibilityFailure(List<ProgramEligibilityIssueCode> errors) {
    return errors.stream().filter(programEligibilityIssueCode -> programEligibilityIssueCode.getProgramEligibilityTypeCode().getCode().equalsIgnoreCase(ProgramEligibilityTypeCode.BASE.getCode())).findFirst();
  }

  public void updateFteColumns(FteCalculationResult fteCalculationResult, SdcSchoolCollectionStudentEntity studentEntity) {
    studentEntity.setFte(fteCalculationResult.getFte());
    studentEntity.setFteZeroReasonCode(fteCalculationResult.getFteZeroReason());
  }

  public void convertNumOfCourses(SdcSchoolCollectionStudentEntity studentEntity) {
    studentEntity.setNumberOfCoursesDec(BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(studentEntity.getNumberOfCourses(), studentEntity.getAssignedStudentId())));
  }

  public SdcStudentEll createOrReturnSdcStudentEll(SdcStudentEll studentEll) {
    Optional<SdcStudentEllEntity> existingEll = this.sdcStudentEllRepository
      .findByStudentID(UUID.fromString(studentEll.getStudentID()));

    if (existingEll.isPresent()) {
      return sdcStudentEllMapper.toStructure(existingEll.get());
    }

    RequestUtil.setAuditColumnsForCreateIfBlank(studentEll);
    SdcStudentEllEntity entity = sdcStudentEllMapper.toModel(studentEll);
    SdcStudentEllEntity savedEllEntity = this.sdcStudentEllRepository.save(entity);
    return sdcStudentEllMapper.toStructure(savedEllEntity);
  }

  public List<SdcStudentEll> createOrReturnSdcStudentEll(List<SdcStudentEll> studentEll) {
    return studentEll.stream().map(this::createOrReturnSdcStudentEll).toList();
  }

  public StudentRuleData createStudentRuleDataForValidation(SdcSchoolCollectionStudentEntity studentEntity) {
    StudentRuleData sdcStudentSagaData = new StudentRuleData();
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID());
    if(sdcSchoolCollection.isPresent()) {
      var school = this.restUtils.getSchoolBySchoolID(sdcSchoolCollection.get().getSchoolID().toString());
      school.ifPresent(sdcStudentSagaData::setSchool);
      sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(studentEntity);
      return sdcStudentSagaData;
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "SdcSchoolCollectionEntity", studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID().toString());
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcSchoolCollectionStudentEntity updatePENStatus(String penCode, SdcSchoolCollectionStudentEntity studentEntity) {
    var curStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(studentEntity.getSdcSchoolCollectionStudentID()).orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, studentEntity.getSdcSchoolCollectionStudentID().toString()));

    curStudentEntity.setPenMatchResult(penCode);
    curStudentEntity.setAssignedStudentId(studentEntity.getAssignedStudentId());
    curStudentEntity.setAssignedPen(studentEntity.getAssignedPen());
    curStudentEntity.setUnderReviewAssignedPen(null);
    curStudentEntity.setUnderReviewAssignedStudentId(null);

    if(StringUtils.isBlank(curStudentEntity.getStudentPen())){
      curStudentEntity.setStudentPen(studentEntity.getAssignedPen());
    }

    var studentRuleData = createStudentRuleDataForValidation(curStudentEntity);

    var processedSdcSchoolCollectionStudent = processStudentRecord(studentRuleData.getSchool(), curStudentEntity, false);
    if (processedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString())) {
      log.debug(SDC_SCHOOL_COLLECTION_STUDENT_WAS_NOT_SAVED, processedSdcSchoolCollectionStudent);
      processedSdcSchoolCollectionStudent.setUpdateDate(curStudentEntity.getUpdateDate());
      processedSdcSchoolCollectionStudent.setUpdateUser(curStudentEntity.getUpdateUser());
      return processedSdcSchoolCollectionStudent;
    }

    processedSdcSchoolCollectionStudent.setCurrentDemogHash(Integer.toString(processedSdcSchoolCollectionStudent.getUniqueObjectHash()));
    return sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(processedSdcSchoolCollectionStudent);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<SdcSchoolCollectionStudentEntity> moveSldRecords(SldMove sldMove) {
    var toStudent = this.restUtils.getStudentByPEN(UUID.randomUUID(), sldMove.getToStudentPen());
    var sdcSchoolCollectionStudentEntities = sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollectionStudentIDIn(sldMove.getSdcSchoolCollectionIdsToUpdate());
    sdcSchoolCollectionStudentEntities.forEach(sdcSchoolCollectionStudentEntity -> {
      sdcSchoolCollectionStudentEntity.setAssignedStudentId(UUID.fromString(toStudent.getStudentID()));
      sdcSchoolCollectionStudentEntity.setAssignedPen(toStudent.getPen());
    });
    return sdcSchoolCollectionStudentStorageService.saveAllSDCStudentsWithHistory(sdcSchoolCollectionStudentEntities);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<SdcSchoolCollectionStudentEntity> softDeleteSdcSchoolCollectionStudents(SoftDeleteRecordSet softDeleteRecordSet) {
    List<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntities = sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollectionStudentIDIn(softDeleteRecordSet.getSoftDeleteStudentIDs());

    sdcSchoolCollectionStudentEntities.forEach(student -> {
      this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(student.getSdcSchoolCollectionStudentID());
      student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.toString());
      student.setUpdateUser(softDeleteRecordSet.getUpdateUser());
    });

    return sdcSchoolCollectionStudentStorageService.saveAllSDCStudentsWithHistory(sdcSchoolCollectionStudentEntities);
  }

  public SdcSchoolCollectionStudentEntity validateAndProcessNewSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity, boolean isStaffMember) {
    TransformUtil.uppercaseFields(sdcSchoolCollectionStudentEntity);
    var studentRuleData = createStudentRuleDataForValidation(sdcSchoolCollectionStudentEntity);

    var processedSdcSchoolCollectionStudent = processStudentRecord(studentRuleData.getSchool(), sdcSchoolCollectionStudentEntity, true);
    if (processedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString())) {
      log.debug(SDC_SCHOOL_COLLECTION_STUDENT_WAS_NOT_SAVED, processedSdcSchoolCollectionStudent);
      return processedSdcSchoolCollectionStudent;
    }

    processedSdcSchoolCollectionStudent.setCurrentDemogHash(Integer.toString(processedSdcSchoolCollectionStudent.getUniqueObjectHash()));

    sdcDuplicatesService.checkIfDuplicateIsGeneratedAndThrow(processedSdcSchoolCollectionStudent, isCollectionInProvDupes(processedSdcSchoolCollectionStudent.getSdcSchoolCollection().getCollectionEntity()), isStaffMember);
    return sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(processedSdcSchoolCollectionStudent);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markPENForReview(SdcSchoolCollectionStudentEntity studentEntity) {
    var curStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(studentEntity.getSdcSchoolCollectionStudentID()).orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, studentEntity.getSdcSchoolCollectionStudentID().toString()));

    var assignedPen = curStudentEntity.getAssignedPen();
    var assignedStudentId = curStudentEntity.getAssignedStudentId();
    curStudentEntity.setPenMatchResult(IN_REVIEW);
    curStudentEntity.setUnderReviewAssignedPen(assignedPen);
    curStudentEntity.setUnderReviewAssignedStudentId(assignedStudentId);
    curStudentEntity.setAssignedStudentId(null);
    curStudentEntity.setAssignedPen(null);

    sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(curStudentEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity createSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity, boolean isStaffMember) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID());
    if(sdcSchoolCollection.isPresent()) {
      studentEntity.setEnrolledProgramCodes(TransformUtil.sanitizeEnrolledProgramString(studentEntity.getEnrolledProgramCodes()));
      studentEntity.setSdcSchoolCollection(sdcSchoolCollection.get());
      studentEntity.setOriginalDemogHash(Integer.toString(studentEntity.getUniqueObjectHash()));
      studentEntity.setCurrentDemogHash(Integer.toString(studentEntity.getUniqueObjectHash()));
      return validateAndProcessNewSdcSchoolCollectionStudent(studentEntity, isStaffMember);
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "SdcSchoolCollectionEntity", studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID().toString());
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcSchoolCollectionStudentEntity updateSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity, boolean isStaffMember){
    var currentStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(studentEntity.getSdcSchoolCollectionStudentID()).orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, studentEntity.getSdcSchoolCollectionStudentID().toString()));
    final SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = new SdcSchoolCollectionStudentEntity();
    BeanUtils.copyProperties(studentEntity, sdcSchoolCollectionStudentEntity, "sdcSchoolCollection", "createUser", "createDate", "sdcStudentValidationIssueEntities");

    sdcSchoolCollectionStudentEntity.setEnrolledProgramCodes(TransformUtil.sanitizeEnrolledProgramString(sdcSchoolCollectionStudentEntity.getEnrolledProgramCodes()));
    sdcSchoolCollectionStudentEntity.setSdcSchoolCollection(currentStudentEntity.getSdcSchoolCollection());
    sdcSchoolCollectionStudentEntity.setOriginalDemogHash(currentStudentEntity.getOriginalDemogHash());
    sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().clear();
    sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().addAll(currentStudentEntity.getSDCStudentValidationIssueEntities());

    HashSet<String> existingEnrolledProgramCodes = currentStudentEntity.getSdcStudentEnrolledProgramEntities().stream()
            .map(SdcSchoolCollectionStudentEnrolledProgramEntity::getEnrolledProgramCode)
            .collect(Collectors.toCollection(HashSet::new));
    HashSet<SdcSchoolCollectionStudentEnrolledProgramEntity> updatedEnrolledPrograms = new HashSet<>();

    if (StringUtils.isNotBlank(sdcSchoolCollectionStudentEntity.getEnrolledProgramCodes())) {
      List<String> incomingEnrolledProgramCodes = TransformUtil.splitIntoChunks(sdcSchoolCollectionStudentEntity.getEnrolledProgramCodes(), 2);
      for (String programCode : incomingEnrolledProgramCodes) {
        if (!existingEnrolledProgramCodes.contains(programCode)) {
          SdcSchoolCollectionStudentEnrolledProgramEntity enrolledProgramEntity = new SdcSchoolCollectionStudentEnrolledProgramEntity();
          enrolledProgramEntity.setSdcSchoolCollectionStudentEntity(currentStudentEntity);
          enrolledProgramEntity.setEnrolledProgramCode(programCode);
          enrolledProgramEntity.setUpdateUser(studentEntity.getUpdateUser());
          enrolledProgramEntity.setUpdateDate(LocalDateTime.now());
          enrolledProgramEntity.setCreateUser(studentEntity.getCreateUser());
          enrolledProgramEntity.setCreateDate(LocalDateTime.now());
          updatedEnrolledPrograms.add(enrolledProgramEntity);
        } else {
          // If the program code already exists, retain the existing entity for update tracking
          currentStudentEntity.getSdcStudentEnrolledProgramEntities().stream()
                  .filter(ep -> ep.getEnrolledProgramCode().equals(programCode))
                  .findFirst()
                  .ifPresent(updatedEnrolledPrograms::add);
        }
      }
      // Clear existing and add the merged set
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().clear();
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().addAll(updatedEnrolledPrograms);

    } else {
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().clear();
    }

    TransformUtil.uppercaseFields(sdcSchoolCollectionStudentEntity);
    var studentRuleData = createStudentRuleDataForValidation(sdcSchoolCollectionStudentEntity);

    var processedSdcSchoolCollectionStudent = processStudentRecord(studentRuleData.getSchool(), sdcSchoolCollectionStudentEntity, true);

    if (processedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString())) {
      log.debug(SDC_SCHOOL_COLLECTION_STUDENT_WAS_NOT_SAVED, processedSdcSchoolCollectionStudent);
      processedSdcSchoolCollectionStudent.setUpdateDate(currentStudentEntity.getUpdateDate());
      processedSdcSchoolCollectionStudent.setUpdateUser(currentStudentEntity.getUpdateUser());
      return processedSdcSchoolCollectionStudent;
    }

    processedSdcSchoolCollectionStudent.setCurrentDemogHash(Integer.toString(processedSdcSchoolCollectionStudent.getUniqueObjectHash()));
    sdcDuplicatesService.checkIfDuplicateIsGeneratedAndThrow(processedSdcSchoolCollectionStudent, isCollectionInProvDupes(processedSdcSchoolCollectionStudent.getSdcSchoolCollection().getCollectionEntity()), isStaffMember);
    return sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(processedSdcSchoolCollectionStudent);
  }

}
