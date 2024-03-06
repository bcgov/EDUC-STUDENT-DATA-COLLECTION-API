package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorChainProcessor;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.helpers.SdcHelper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcStudentEllMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentService {

  private final MessagePublisher messagePublisher;

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  private final SdcStudentEllRepository sdcStudentEllRepository;

  private final SdcSchoolCollectionStudentHistoryService sdcSchoolCollectionStudentHistoryService;

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  private final SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository;

  private final FteCalculatorChainProcessor fteCalculatorChainProcessor;

  private final ProgramEligibilityRulesProcessor programEligibilityRulesProcessor;

  private final RestUtils restUtils;
  private static SdcStudentEllMapper sdcStudentEllMapper = SdcStudentEllMapper.mapper;

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
  public SdcSchoolCollectionStudentEntity createSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID());
    if(sdcSchoolCollection.isPresent()) {
      studentEntity.setEnrolledProgramCodes(TransformUtil.sanitizeEnrolledProgramString(studentEntity.getEnrolledProgramCodes()));
      studentEntity.setSdcSchoolCollection(sdcSchoolCollection.get());
      return validateAndProcessSdcSchoolCollectionStudent(studentEntity);
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "SdcSchoolCollectionEntity", studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID().toString());
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity updateSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity) {
    var currentStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(studentEntity.getSdcSchoolCollectionStudentID());
    if(currentStudentEntity.isPresent()) {
      SdcSchoolCollectionStudentEntity getCurStudentEntity = currentStudentEntity.get();
      final SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = new SdcSchoolCollectionStudentEntity();
      BeanUtils.copyProperties(studentEntity, sdcSchoolCollectionStudentEntity, "sdcSchoolCollectionStudentID, sdcSchoolCollection, sdcSchoolCollectionStudentStatusCode, createUser, createDate, numberOfCoursesDec", "sdcStudentValidationIssueEntities", "sdcStudentEnrolledProgramEntities");

      sdcSchoolCollectionStudentEntity.setEnrolledProgramCodes(TransformUtil.sanitizeEnrolledProgramString(sdcSchoolCollectionStudentEntity.getEnrolledProgramCodes()));
      sdcSchoolCollectionStudentEntity.setSdcSchoolCollection(getCurStudentEntity.getSdcSchoolCollection());
      sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().clear();
      sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().addAll(getCurStudentEntity.getSDCStudentValidationIssueEntities());
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().clear();
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().addAll(getCurStudentEntity.getSdcStudentEnrolledProgramEntities());

      return validateAndProcessSdcSchoolCollectionStudent(sdcSchoolCollectionStudentEntity);
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, "SdcSchoolCollectionStudentEntity", studentEntity.getSdcSchoolCollectionStudentID().toString());
    }
  }

  public SdcSchoolCollectionStudentEntity validateAndProcessSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity) {
      TransformUtil.uppercaseFields(sdcSchoolCollectionStudentEntity);
      var studentRuleData = createStudentRuleDataForValidation(sdcSchoolCollectionStudentEntity);

      var processedSdcSchoolCollectionStudent = processStudentRecord(studentRuleData.getSchool(), sdcSchoolCollectionStudentEntity);
      if (processedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString())) {
        log.debug("SdcSchoolCollectionStudent was not saved to the database because it has errors :: {}", processedSdcSchoolCollectionStudent);
        return processedSdcSchoolCollectionStudent;
      } else {
        return saveSdcStudentWithHistory(processedSdcSchoolCollectionStudent);
      }
  }

  public SdcSchoolCollectionStudentEntity saveSdcStudentWithHistory(SdcSchoolCollectionStudentEntity studentEntity) {
    var entity = this.sdcSchoolCollectionStudentRepository.save(studentEntity);
    this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(entity, studentEntity.getUpdateUser());
    return entity;
  }

  public List<SdcSchoolCollectionStudentEntity> saveAllSdcStudentWithHistory(List<SdcSchoolCollectionStudentEntity> studentEntities) {
    var entities = this.sdcSchoolCollectionStudentRepository.saveAll(studentEntities);
    entities.forEach(entity -> this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(entity, entity.getUpdateUser()));

    return entities;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processSagaStudentRecord(final UUID sdcSchoolCollectionStudentID, School school) {
    var currentStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    if(currentStudentEntity.isPresent()) {
      saveSdcStudentWithHistory(processStudentRecord(school, currentStudentEntity.get()));
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, "SdcSchoolCollectionStudentEntity", sdcSchoolCollectionStudentID.toString());
    }
  }

  private SdcSchoolCollectionStudentEntity processStudentRecord(School school, SdcSchoolCollectionStudentEntity incomingStudentEntity) {
    StudentRuleData studentRuleData = new StudentRuleData();
    studentRuleData.setSdcSchoolCollectionStudentEntity(incomingStudentEntity);
    studentRuleData.setSchool(school);

    var validationErrors = validateStudent(studentRuleData);
    if(validationErrors.stream().noneMatch(issueValue -> issueValue.getValidationIssueSeverityCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString()))){

      // Update Student age columns
      String studentDOB = studentRuleData.getSdcSchoolCollectionStudentEntity().getDob();
      studentRuleData.getSdcSchoolCollectionStudentEntity().setIsAdult(DOBUtil.isAdult(studentDOB));
      studentRuleData.getSdcSchoolCollectionStudentEntity().setIsSchoolAged(DOBUtil.isSchoolAged(studentDOB));

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
    return issues.stream().map(issue -> new SdcSchoolCollectionStudentValidationIssueErrorWarningCount(issue.getSeverityCode(), issue.getTotal())).collect(Collectors.toList());
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
      student.setEllNonEligReasonCode(getReasonCode(errors, Arrays.asList(NOT_ENROLLED_ELL, YEARS_IN_ELL)));
      student.setIndigenousSupportProgramNonEligReasonCode(getReasonCode(errors, Arrays.asList(NOT_ENROLLED_INDIGENOUS, INDIGENOUS_ADULT, NO_INDIGENOUS_ANCESTRY)));
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity softDeleteSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    SdcSchoolCollectionStudentEntity student = sdcSchoolCollectionStudentEntityOptional.orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudent.class, SDC_SCHOOL_COLLECTION_STUDENT_ID, sdcSchoolCollectionStudentID.toString()));

    //delete student validation errors
    this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(student.getSdcSchoolCollectionStudentID());
    student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.toString());

    return saveSdcStudentWithHistory(student);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<SdcSchoolCollectionStudentEntity> softDeleteSdcSchoolCollectionStudents(List<UUID> sdcSchoolCollectionStudentIDs) {
    List<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntities = sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollectionStudentIDIn(sdcSchoolCollectionStudentIDs);

    sdcSchoolCollectionStudentEntities.forEach(student -> {
      this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(student.getSdcSchoolCollectionStudentID());
      student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.toString());
    });

    return saveAllSdcStudentWithHistory(sdcSchoolCollectionStudentEntities);
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

  private StudentRuleData createStudentRuleDataForValidation(SdcSchoolCollectionStudentEntity studentEntity) {
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
}
