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
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentValidationIssueRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityRulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.rules.RulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentService {

  private final MessagePublisher messagePublisher;

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  private final SdcSchoolCollectionStudentHistoryService sdcSchoolCollectionStudentHistoryService;

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  private final SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository;

  private final FteCalculatorChainProcessor fteCalculatorChainProcessor;

  private final ProgramEligibilityRulesProcessor programEligibilityRulesProcessor;

  private final RestUtils restUtils;

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
  public void updateAndValidateSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity) {
    var studentRuleData = createStudentRuleDataForValidation(studentEntity);
    processStudentRecord(studentEntity.getSdcSchoolCollectionStudentID(), studentRuleData.getSchool(), studentRuleData.getCollectionTypeCode(), Optional.of(studentEntity));
  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public void processStudentRecord(final UUID sdcSchoolCollectionStudentID, School school, String collectionTypeCode, Optional<SdcSchoolCollectionStudentEntity> incomingStudentEntity) {
    var currentStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);
    if(incomingStudentEntity.isPresent() && currentStudentEntity.isPresent()) {
      SdcSchoolCollectionStudentEntity getCurStudentEntity = currentStudentEntity.get();
      getCurStudentEntity.setEnrolledProgramCodes(TransformUtil.sanitizeEnrolledProgramString(getCurStudentEntity.getEnrolledProgramCodes()));
      BeanUtils.copyProperties(incomingStudentEntity, getCurStudentEntity, "sdcSchoolCollectionStudentID, sdcSchoolCollection, sdcSchoolCollectionStudentStatusCode, createUser, createDate", "sdcStudentValidationIssueEntities", "sdcStudentEnrolledProgramEntities");
      TransformUtil.uppercaseFields(getCurStudentEntity);
    }else if (currentStudentEntity.isEmpty()) {
      throw new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, "SdcSchoolCollectionStudentEntity", sdcSchoolCollectionStudentID.toString());
    }

    StudentRuleData studentRuleData = new StudentRuleData();
    studentRuleData.setSdcSchoolCollectionStudentEntity(currentStudentEntity.get());
    studentRuleData.setSchool(school);
    studentRuleData.setCollectionTypeCode(collectionTypeCode);

    // Update Student age columns
    String studentDOB = studentRuleData.getSdcSchoolCollectionStudentEntity().getDob();
    studentRuleData.getSdcSchoolCollectionStudentEntity().setIsAdult(DOBUtil.isAdult(studentDOB));
    studentRuleData.getSdcSchoolCollectionStudentEntity().setIsSchoolAged(DOBUtil.isSchoolAged(studentDOB));

    var validationErrors = validateStudent(studentRuleData);
    if(validationErrors.stream().noneMatch(issueValue -> issueValue.getValidationIssueSeverityCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString()))){
      calculateAdditionalStudentAttributes(studentRuleData);
    }
    var entity = this.sdcSchoolCollectionStudentRepository.save(studentRuleData.getSdcSchoolCollectionStudentEntity());
    this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(entity, studentRuleData.getSdcSchoolCollectionStudentEntity().getUpdateUser());
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

  public SdcSchoolCollectionStudentValidationIssueErrorWarningCount errorAndWarningCountBySdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
    SdcSchoolCollectionStudentValidationIssueErrorWarningCount sdcSchoolCollectionStudentValidationIssueErrorWarningCount = new SdcSchoolCollectionStudentValidationIssueErrorWarningCount();
    sdcSchoolCollectionStudentValidationIssueErrorWarningCount.setError(
        sdcSchoolCollectionStudentRepository.getCountByValidationIssueSeverityCodeAndSdcSchoolCollectionID("ERROR", sdcSchoolCollectionID));
    sdcSchoolCollectionStudentValidationIssueErrorWarningCount.setFundingWarning(
        sdcSchoolCollectionStudentRepository.getCountByValidationIssueSeverityCodeAndSdcSchoolCollectionID("FUNDING_WARNING", sdcSchoolCollectionID));
    sdcSchoolCollectionStudentValidationIssueErrorWarningCount.setInfoWarning(
            sdcSchoolCollectionStudentRepository.getCountByValidationIssueSeverityCodeAndSdcSchoolCollectionID("INFO_WARNING", sdcSchoolCollectionID));

    return sdcSchoolCollectionStudentValidationIssueErrorWarningCount;
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
      student.setCareerProgramNonEligReasonCode(getReasonCode(errors, Arrays.asList(NOT_ENROLLED_CAREER)));
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity softDeleteSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    SdcSchoolCollectionStudentEntity student = sdcSchoolCollectionStudentEntityOptional.orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudent.class, SDC_SCHOOL_COLLECTION_STUDENT_ID, sdcSchoolCollectionStudentID.toString()));

    //delete student validation errors
    this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(student.getSdcSchoolCollectionStudentID());
    student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.toString());

    return sdcSchoolCollectionStudentRepository.save(student);
  }

  private StudentRuleData createStudentRuleDataForValidation(SdcSchoolCollectionStudentEntity studentEntity) {
    StudentRuleData sdcStudentSagaData = new StudentRuleData();
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID());
    if(sdcSchoolCollection.isPresent()) {
      var school = this.restUtils.getSchoolBySchoolID(sdcSchoolCollection.get().getSchoolID().toString());
      sdcStudentSagaData.setCollectionTypeCode(sdcSchoolCollection.get().getCollectionEntity().getCollectionTypeCode());
      school.ifPresent(sdcStudentSagaData::setSchool);
      sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(studentEntity);
      return sdcStudentSagaData;
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "SdcSchoolCollectionEntity", studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID().toString());
    }
  }
}
