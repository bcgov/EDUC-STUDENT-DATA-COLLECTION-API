package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorChainProcessor;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateResolutionCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramDuplicateTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.helpers.SdcHelper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcStudentEllMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
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

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode.*;
import static ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil.isCollectionInProvDupes;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentService {

  private final MessagePublisher messagePublisher;

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  private final SdcStudentEllRepository sdcStudentEllRepository;

  private final SdcSchoolCollectionStudentStorageService sdcSchoolCollectionStudentStorageService;

  private final SdcDuplicatesService sdcDuplicatesService;

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  private final CollectionRepository collectionRepository;

  private final FteCalculatorChainProcessor fteCalculatorChainProcessor;

  private final ProgramEligibilityRulesProcessor programEligibilityRulesProcessor;

  private final SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository;

  private final RestUtils restUtils;

  private static SdcStudentEllMapper sdcStudentEllMapper = SdcStudentEllMapper.mapper;

  private static final String IN_REVIEW = "INREVIEW";

  private static final String SDC_SCHOOL_COLLECTION_STUDENT_STRING = "SdcSchoolCollectionStudentEntity";

  private final RulesProcessor rulesProcessor;

  private static final String SDC_SCHOOL_COLLECTION_STUDENT_ID = "sdcSchoolCollectionStudentId";

  public SdcSchoolCollectionStudentEntity getSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    return sdcSchoolCollectionStudentEntityOptional.orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudent.class, SDC_SCHOOL_COLLECTION_STUDENT_ID, sdcSchoolCollectionStudentID.toString()));
  }

  public void publishUnprocessedStudentRecordsForProcessing(final List<SdcStudentSagaData> sdcStudentSagaDatas) {
    sdcStudentSagaDatas.forEach(this::sendIndividualStudentAsMessageToTopic);
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

    // Calculate Fte
    var fteResults = this.fteCalculatorChainProcessor.processFteCalculator(studentRuleData);
    updateFteColumns(fteResults, sdcSchoolCollectionStudentEntity);

    // Convert number of courses string to decimal
    if(StringUtils.isNotBlank(sdcSchoolCollectionStudentEntity.getNumberOfCourses())){
      convertNumOfCourses(sdcSchoolCollectionStudentEntity);
    }
  }

  @Async("publisherExecutor")
  public void prepareAndSendSdcStudentsForFurtherProcessing(final List<SdcSchoolCollectionStudentEntity> sdcStudentEntities) {
    final List<SdcStudentSagaData> sdcStudentSagaDatas = sdcStudentEntities.stream()
      .map(el -> {
        val sdcStudentSagaData = new SdcStudentSagaData();
        Optional<SdcSchoolCollectionEntity> sdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(el.getSdcSchoolCollection().getSdcSchoolCollectionID());
        if(sdcSchoolCollection.isPresent()) {
          var school = this.restUtils.getSchoolBySchoolID(sdcSchoolCollection.get().getSchoolID().toString());
          sdcStudentSagaData.setCollectionTypeCode(sdcSchoolCollection.get().getCollectionEntity().getCollectionTypeCode());
          sdcStudentSagaData.setSchool(school.get());
        }
        sdcStudentSagaData.setSdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(el));
        return sdcStudentSagaData;
      }).toList();
    this.publishUnprocessedStudentRecordsForProcessing(sdcStudentSagaDatas);
  }

  @Async("publisherExecutor")
  public void prepareStudentsForDemogUpdate(final List<SdcSchoolCollectionStudentEntity> sdcStudentEntities) {
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

              return updateStudentSagaData;
            }).toList();
    publishStudentRecordsForDemogUpdate(updateStudentSagas);
  }

  public void publishStudentRecordsForDemogUpdate(final List<UpdateStudentSagaData> updateStudentSagas) {
    updateStudentSagas.forEach(this::sendStudentRecordsForDemogUpdateAsMessageToTopic);
  }

  private void sendStudentRecordsForDemogUpdateAsMessageToTopic(final UpdateStudentSagaData updateStudentSagaData) {
    final var eventPayload = JsonUtil.getJsonString(updateStudentSagaData);
    if (eventPayload.isPresent()) {
      final Event event = Event.builder().eventType(EventType.UPDATE_STUDENTS_DEMOG_DOWNSTREAM).eventOutcome(EventOutcome.STUDENTS_DEMOG_UPDATED).eventPayload(eventPayload.get()).sdcSchoolStudentID(updateStudentSagaData.getSdcSchoolCollectionStudentID()).build();
      final var eventString = JsonUtil.getJsonString(event);
      if (eventString.isPresent()) {
        this.messagePublisher.dispatchMessage(TopicsEnum.UPDATE_STUDENT_DOWNSTREAM_TOPIC.toString(), eventString.get().getBytes());
      } else {
        log.error("Event String is empty, skipping the publish to topic :: {}", updateStudentSagaData);
      }
    } else {
      log.error("Event payload is empty, skipping the publish to topic :: {}", updateStudentSagaData);
    }
  }

  /**
   * Send individual student as message to topic consumer.
   */
  private void sendIndividualStudentAsMessageToTopic(final SdcStudentSagaData sdcStudentSagaData) {
    final var eventPayload = JsonUtil.getJsonString(sdcStudentSagaData);
    if (eventPayload.isPresent()) {
      final Event event = Event.builder().eventType(EventType.READ_FROM_TOPIC).eventOutcome(EventOutcome.READ_FROM_TOPIC_SUCCESS).eventPayload(eventPayload.get()).sdcSchoolStudentID(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID()).build();
      final var eventString = JsonUtil.getJsonString(event);
      if (eventString.isPresent()) {
        this.messagePublisher.dispatchMessage(TopicsEnum.STUDENT_DATA_COLLECTION_API_TOPIC.toString(), eventString.get().getBytes());
      } else {
        log.error("Event String is empty, skipping the publish to topic :: {}", sdcStudentSagaData);
      }
    } else {
      log.error("Event payload is empty, skipping the publish to topic :: {}", sdcStudentSagaData);
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
      student.setFrenchProgramNonEligReasonCode(getReasonCode(errors, Arrays.asList(NOT_ENROLLED_FRENCH)));
      student.setEllNonEligReasonCode(getReasonCode(errors, Arrays.asList(NOT_ENROLLED_ELL, YEARS_IN_ELL, ELL_INDY_SCHOOL)));
      student.setIndigenousSupportProgramNonEligReasonCode(getReasonCode(errors, Arrays.asList(NOT_ENROLLED_INDIGENOUS, INDIGENOUS_ADULT, NO_INDIGENOUS_ANCESTRY, INDIGENOUS_INDY_SCHOOL)));
      student.setCareerProgramNonEligReasonCode(getReasonCode(errors, Arrays.asList(NOT_ENROLLED_CAREER, ENROLLED_CAREER_INDY_SCHOOL)));
      student.setSpecialEducationNonEligReasonCode(getReasonCode(errors, Arrays.asList(NOT_ENROLLED_SPECIAL_ED, NON_ELIG_SPECIAL_EDUCATION)));
    }
  }

  private String getReasonCode(List<ProgramEligibilityIssueCode> errors, List<ProgramEligibilityIssueCode> codes){
    var first = errors.stream().filter(codes::contains).findFirst();
    return first.isPresent() ? first.get().getCode() : null;
  }

  public static final Optional<ProgramEligibilityIssueCode> getBaseProgramEligibilityFailure(List<ProgramEligibilityIssueCode> errors) {
    List<ProgramEligibilityIssueCode> ineligibleCodes = Arrays.asList(
      HOMESCHOOL,
      OFFSHORE,
      OUT_OF_PROVINCE,
      INACTIVE_ADULT,
      INACTIVE_SCHOOL_AGE,
      TOO_YOUNG
    );

    return errors.stream().filter(ineligibleCodes::contains).findFirst();
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
  public void updatePenStatusToNEW(SdcSchoolCollectionStudentEntity studentEntity) {
    var curStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(studentEntity.getSdcSchoolCollectionStudentID()).orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, studentEntity.getSdcSchoolCollectionStudentID().toString()));

    curStudentEntity.setPenMatchResult("NEW");
    curStudentEntity.setAssignedStudentId(null);
    curStudentEntity.setAssignedPen(null);

    sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(curStudentEntity);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void updatePenStatusToMATCH(SdcSchoolCollectionStudentEntity studentEntity) {
    var curStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(studentEntity.getSdcSchoolCollectionStudentID()).orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, studentEntity.getSdcSchoolCollectionStudentID().toString()));

    curStudentEntity.setPenMatchResult("MATCH");
    curStudentEntity.setAssignedStudentId(studentEntity.getAssignedStudentId());
    curStudentEntity.setAssignedPen(studentEntity.getAssignedPen());
    if(StringUtils.isBlank(curStudentEntity.getStudentPen())){
      curStudentEntity.setStudentPen(studentEntity.getAssignedPen());
    }
    var ruleData = createStudentRuleDataForValidation(curStudentEntity);
    sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(processStudentRecord(ruleData.getSchool(), curStudentEntity, false));
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

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcSchoolCollectionStudentEntity markStudentSoftDeletedOnly(SdcSchoolCollectionStudent studentToMarkDeleted) {
    var studentID = UUID.fromString(studentToMarkDeleted.getSdcSchoolCollectionStudentID());

    var sdcSchoolCollectionStudentEntity = sdcSchoolCollectionStudentRepository.findById(studentID).orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, studentID.toString()));

    this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(studentID);
    sdcSchoolCollectionStudentEntity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.toString());
    sdcSchoolCollectionStudentEntity.setUpdateUser(studentToMarkDeleted.getUpdateUser());

    return sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(sdcSchoolCollectionStudentEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<SdcSchoolCollectionStudentEntity> softDeleteSdcSchoolCollectionStudents(SoftDeleteRecordSet softDeleteRecordSet) {
    List<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntities = sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollectionStudentIDIn(softDeleteRecordSet.getSoftDeleteStudentIDs());

    sdcSchoolCollectionStudentEntities.forEach(student -> {
      this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(student.getSdcSchoolCollectionStudentID());
      student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.toString());
      student.setUpdateUser(softDeleteRecordSet.getUpdateUser());
      sdcDuplicatesService.resolveAllExistingDuplicatesForSoftDelete(student.getSdcSchoolCollectionStudentID());
    });

    return sdcSchoolCollectionStudentStorageService.saveAllSDCStudentsWithHistory(sdcSchoolCollectionStudentEntities);
  }

  public SdcSchoolCollectionStudentEntity validateAndProcessNewSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity, boolean checkForNewNonAllowableDups) {
    TransformUtil.uppercaseFields(sdcSchoolCollectionStudentEntity);
    var studentRuleData = createStudentRuleDataForValidation(sdcSchoolCollectionStudentEntity);

    var processedSdcSchoolCollectionStudent = processStudentRecord(studentRuleData.getSchool(), sdcSchoolCollectionStudentEntity, true);
    if (processedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString())) {
      log.debug("SdcSchoolCollectionStudent was not saved to the database because it has errors :: {}", processedSdcSchoolCollectionStudent);
      return processedSdcSchoolCollectionStudent;
    }

    sdcDuplicatesService.generateAllowableDuplicatesOrElseThrow(sdcSchoolCollectionStudentEntity, checkForNewNonAllowableDups);

    processedSdcSchoolCollectionStudent.setCurrentDemogHash(Integer.toString(processedSdcSchoolCollectionStudent.getUniqueObjectHash()));
    return sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(processedSdcSchoolCollectionStudent);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markPENForReview(SdcSchoolCollectionStudentEntity studentEntity) {
    var curStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(studentEntity.getSdcSchoolCollectionStudentID()).orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, studentEntity.getSdcSchoolCollectionStudentID().toString()));

    curStudentEntity.setPenMatchResult(IN_REVIEW);
    curStudentEntity.setAssignedStudentId(null);
    curStudentEntity.setAssignedPen(null);

    sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(curStudentEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity createSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID());
    if(sdcSchoolCollection.isPresent()) {
      studentEntity.setEnrolledProgramCodes(TransformUtil.sanitizeEnrolledProgramString(studentEntity.getEnrolledProgramCodes()));
      studentEntity.setSdcSchoolCollection(sdcSchoolCollection.get());
      studentEntity.setOriginalDemogHash(Integer.toString(studentEntity.getUniqueObjectHash()));
      return validateAndProcessNewSdcSchoolCollectionStudent(studentEntity, isCollectionInProvDupes(studentEntity.getSdcSchoolCollection().getCollectionEntity()));
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "SdcSchoolCollectionEntity", studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID().toString());
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity updateSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity){
    var activeCollection = collectionRepository.findActiveCollection().orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "collectionID", "activeCollection"));
    var isCollectionInProvDupes = isCollectionInProvDupes(activeCollection);
    return performUpdateSdcSchoolCollectionStudent(studentEntity, isCollectionInProvDupes);
  }

  private SdcSchoolCollectionStudentEntity performUpdateSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity, boolean checkForNewNonAllowableDups) {
    var currentStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(studentEntity.getSdcSchoolCollectionStudentID());
    if(currentStudentEntity.isPresent()) {
      SdcSchoolCollectionStudentEntity getCurStudentEntity = currentStudentEntity.get();
      final SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = new SdcSchoolCollectionStudentEntity();
      BeanUtils.copyProperties(studentEntity, sdcSchoolCollectionStudentEntity, "sdcSchoolCollection", "createUser", "createDate", "sdcStudentValidationIssueEntities", "sdcStudentEnrolledProgramEntities");

      sdcSchoolCollectionStudentEntity.setEnrolledProgramCodes(TransformUtil.sanitizeEnrolledProgramString(sdcSchoolCollectionStudentEntity.getEnrolledProgramCodes()));
      sdcSchoolCollectionStudentEntity.setSdcSchoolCollection(getCurStudentEntity.getSdcSchoolCollection());
      sdcSchoolCollectionStudentEntity.setOriginalDemogHash(getCurStudentEntity.getOriginalDemogHash());
      sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().clear();
      sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().addAll(getCurStudentEntity.getSDCStudentValidationIssueEntities());
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().clear();
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().addAll(getCurStudentEntity.getSdcStudentEnrolledProgramEntities());

      UUID originalAssignedStudentID = sdcSchoolCollectionStudentEntity.getAssignedStudentId();

      TransformUtil.uppercaseFields(sdcSchoolCollectionStudentEntity);
      var studentRuleData = createStudentRuleDataForValidation(sdcSchoolCollectionStudentEntity);

      var processedSdcSchoolCollectionStudent = processStudentRecord(studentRuleData.getSchool(), sdcSchoolCollectionStudentEntity, true);
      if (processedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString())) {
        log.debug("SdcSchoolCollectionStudent was not saved to the database because it has errors :: {}", processedSdcSchoolCollectionStudent);
        processedSdcSchoolCollectionStudent.setUpdateDate(getCurStudentEntity.getUpdateDate());
        processedSdcSchoolCollectionStudent.setUpdateUser(getCurStudentEntity.getUpdateUser());
        return processedSdcSchoolCollectionStudent;
      }

      if(originalAssignedStudentID != null && processedSdcSchoolCollectionStudent.getAssignedStudentId() != null && (!originalAssignedStudentID.equals(processedSdcSchoolCollectionStudent.getAssignedStudentId()))) {
        sdcDuplicatesService.deleteAllDuplicatesForStudent(originalAssignedStudentID);
        sdcDuplicatesService.generateAllowableDuplicatesOrElseThrow(processedSdcSchoolCollectionStudent, checkForNewNonAllowableDups);
      }else if (checkForNewNonAllowableDups){
        sdcDuplicatesService.resolveAllExistingDuplicatesForType(processedSdcSchoolCollectionStudent, DuplicateResolutionCode.RELEASED);
      }

      processedSdcSchoolCollectionStudent.setCurrentDemogHash(Integer.toString(processedSdcSchoolCollectionStudent.getUniqueObjectHash()));
      return sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(processedSdcSchoolCollectionStudent);
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, studentEntity.getSdcSchoolCollectionStudentID().toString());
    }
  }
}
