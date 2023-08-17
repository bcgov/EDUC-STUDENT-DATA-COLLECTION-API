package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
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
import ca.bc.gov.educ.studentdatacollection.api.rules.RulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentService {

  private final MessagePublisher messagePublisher;

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  private final SdcSchoolCollectionStudentHistoryService sdcSchoolCollectionStudentHistoryService;

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  private final SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository;

  private final RestUtils restUtils;

  private final RulesProcessor rulesProcessor;

  public SdcSchoolCollectionStudentEntity getSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    return sdcSchoolCollectionStudentEntityOptional.orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionStudentId", sdcSchoolCollectionStudentID.toString()));
  }

  public void publishUnprocessedStudentRecordsForProcessing(final List<SdcStudentSagaData> sdcStudentSagaDatas) {
    sdcStudentSagaDatas.forEach(this::sendIndividualStudentAsMessageToTopic);
  }

  @Async("publisherExecutor")
  public void prepareAndSendSdcStudentsForFurtherProcessing(final List<SdcSchoolCollectionStudentEntity> sdcStudentEntities) {
    final List<SdcStudentSagaData> sdcStudentSagaDatas = sdcStudentEntities.stream()
      .map(el -> {
        val sdcStudentSagaData = new SdcStudentSagaData();
        Optional<SdcSchoolCollectionEntity> sdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(el.getSdcSchoolCollectionID());
        if(sdcSchoolCollection.isPresent()) {
          var school = this.restUtils.getSchoolBySchoolID(sdcSchoolCollection.get().getSchoolID().toString());
          sdcStudentSagaData.setCollectionTypeCode(sdcSchoolCollection.get().getCollectionEntity().getCollectionTypeCode());
          sdcStudentSagaData.setSchool(school.get());
        }
        sdcStudentSagaData.setSdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolCollectionStudentWithValidationIssues(el));
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

  public Optional<SdcSchoolCollectionStudentEntity> findBySdcSchoolStudentID(final String sdcSchoolStudentID) {
    return this.sdcSchoolCollectionStudentRepository.findById(UUID.fromString(sdcSchoolStudentID));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity saveSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity curSdcSchoolCollectionStudentEntity) {
    var entity = this.sdcSchoolCollectionStudentRepository.save(curSdcSchoolCollectionStudentEntity);
    this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(entity, curSdcSchoolCollectionStudentEntity.getUpdateUser());
    return entity;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteSdcStudentValidationErrors(final String sdcSchoolStudentID) {
    this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(UUID.fromString(sdcSchoolStudentID));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity saveSdcSchoolStudentValidationErrors(final String sdcSchoolCollectionStudentID, final List<SdcSchoolCollectionStudentValidationIssue> issues, SdcSchoolCollectionStudentEntity entity) {
    if(entity == null) {
      val sdcSchoolCollectionStudent = this.findBySdcSchoolStudentID(sdcSchoolCollectionStudentID);
      if (sdcSchoolCollectionStudent.isPresent()) {
        entity = sdcSchoolCollectionStudent.get();
      }else{
        throw new StudentDataCollectionAPIRuntimeException("Error while saving SDC school student with ValidationErrors - entity was null");
      }
    }
    entity.getSDCStudentValidationIssueEntities().addAll(SdcHelper.populateValidationErrors(issues, entity));
    entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
    return this.sdcSchoolCollectionStudentRepository.save(entity);
  }

  public SdcSchoolCollectionStudentEntity clearSdcSchoolStudentProgramEligibilityColumns(
    UUID sdcSchoolCollectionStudentID
  ) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional =
      sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    if (sdcSchoolCollectionStudentEntityOptional.isEmpty()) {
      throw new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionStudentId", sdcSchoolCollectionStudentID.toString());
    }

    SdcSchoolCollectionStudentEntity student = sdcSchoolCollectionStudentEntityOptional.get();
    student.setFrenchProgramNonEligReasonCode(null);
    student.setEllNonEligReasonCode(null);
    student.setIndigenousSupportProgramNonEligReasonCode(null);
    student.setCareerProgramNonEligReasonCode(null);
    student.setSpecialEducationNonEligReasonCode(null);
    return student;
  }

  public SdcSchoolCollectionStudentEntity updateStudentAgeColumns(UUID sdcSchoolCollectionStudentID, boolean isAdult, boolean isSchoolAged) throws EntityNotFoundException {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional =
      sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    if (sdcSchoolCollectionStudentEntityOptional.isEmpty()) {
      throw new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionStudentId", sdcSchoolCollectionStudentID.toString());
    }

    SdcSchoolCollectionStudentEntity student = sdcSchoolCollectionStudentEntityOptional.get();
    student.setIsAdult(isAdult);
    student.setIsSchoolAged(isSchoolAged);
    return student;
  }

  public SdcSchoolCollectionStudentEntity deleteExistingAndWriteEnrolledProgramCodes(UUID sdcSchoolCollectionStudentID, List<String> enrolledProgramCodes) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    var student = sdcSchoolCollectionStudentEntityOptional.orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionStudentId", sdcSchoolCollectionStudentID.toString()));

    student.getSdcStudentEnrolledProgramEntities().clear();

    enrolledProgramCodes.forEach(enrolledProgramCode -> {
      var enrolledProgramEntity = new SdcSchoolCollectionStudentEnrolledProgramEntity();
      enrolledProgramEntity.setSdcSchoolCollectionStudentEntity(student);
      enrolledProgramEntity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      enrolledProgramEntity.setUpdateDate(LocalDateTime.now());
      enrolledProgramEntity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      enrolledProgramEntity.setCreateDate(LocalDateTime.now());
      enrolledProgramEntity.setEnrolledProgramCode(enrolledProgramCode);

      student.getSdcStudentEnrolledProgramEntities().add(enrolledProgramEntity);
    });

    return student;
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateAndValidateSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity) {
    Optional<SdcSchoolCollectionStudentEntity> curStudentEntity = sdcSchoolCollectionStudentRepository.findBySdcSchoolCollectionStudentIDAndSdcSchoolCollectionID(studentEntity.getSdcSchoolCollectionStudentID(), studentEntity.getSdcSchoolCollectionID());

    if(curStudentEntity.isEmpty()) {
      throw new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, "SdcSchoolCollectionStudentEntity", studentEntity.getSdcSchoolCollectionStudentID().toString());
    }

    //update student record
    SdcSchoolCollectionStudentEntity getCurStudentEntity = curStudentEntity.get();
    getCurStudentEntity.setEnrolledProgramCodes(TransformUtil.sanitizeEnrolledProgramString(getCurStudentEntity.getEnrolledProgramCodes()));
    BeanUtils.copyProperties(studentEntity, getCurStudentEntity, "sdcSchoolCollectionStudentID, sdcSchoolCollectionID, sdcSchoolCollectionStudentStatusCode, createUser, createDate", "sdcStudentValidationIssueEntities", "sdcStudentEnrolledProgramEntities");
    TransformUtil.uppercaseFields(getCurStudentEntity);

    SdcSchoolCollectionStudentEntity updatedStudentEntity = this.sdcSchoolCollectionStudentRepository.save(getCurStudentEntity);
    this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(updatedStudentEntity, getCurStudentEntity.getUpdateUser());

    //run validation rules on updated student entity
    List<SdcSchoolCollectionStudentValidationIssue> validationErrors = this.rulesProcessor.processRules(createSagaDataForValidation(updatedStudentEntity));

    if (validationErrors.stream().noneMatch(issueValue -> issueValue.getValidationIssueSeverityCode().equalsIgnoreCase("ERROR"))) {
      // if no validation errors, mark the record status as LOADED
      updatedStudentEntity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.LOADED.getCode());
    } else {
      //save new validation errors for student record and mark the record as ERROR
      updatedStudentEntity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
    }

    //delete student validation errors
    this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(updatedStudentEntity.getSdcSchoolCollectionStudentID());
    //save the updates to the student record
    updatedStudentEntity.getSDCStudentValidationIssueEntities().addAll(SdcHelper.populateValidationErrors(validationErrors, updatedStudentEntity));
    SdcSchoolCollectionStudentEntity updatedStatusStudentEntity = this.sdcSchoolCollectionStudentRepository.save(updatedStudentEntity);


//    saveSdcSchoolStudentValidationErrors(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID(), validationErrors, null);

    //write student history
    this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(updatedStatusStudentEntity, updatedStudentEntity.getUpdateUser());
  }

  public SdcSchoolCollectionStudentEntity updateProgramEligibilityColumns(
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors,
    UUID studentId
  ) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional =
      sdcSchoolCollectionStudentRepository.findById(studentId);

    SdcSchoolCollectionStudentEntity student = sdcSchoolCollectionStudentEntityOptional.orElseThrow(() ->
      new EntityNotFoundException(
        SdcSchoolCollectionStudent.class,
        "sdcSchoolCollectionStudentId",
        studentId.toString()
      ));

    Optional<SdcSchoolCollectionStudentProgramEligibilityIssueCode> reasonForNoEligiblility =
      SdcSchoolCollectionStudentProgramEligibilityIssueCode.getBaseProgramEligibilityFailure(errors);

    if (reasonForNoEligiblility.isPresent()) {
      String reasonCode = reasonForNoEligiblility.get().getCode();
      student.setFrenchProgramNonEligReasonCode(reasonCode);
      student.setEllNonEligReasonCode(reasonCode);
      student.setIndigenousSupportProgramNonEligReasonCode(reasonCode);
      student.setCareerProgramNonEligReasonCode(reasonCode);
      student.setSpecialEducationNonEligReasonCode(reasonCode);
    }

    if (errors.contains(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH)) {
      student.setFrenchProgramNonEligReasonCode(
        SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH.getCode()
      );
    }

    if (errors.contains(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_CAREER)) {
      student.setCareerProgramNonEligReasonCode(
        SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_CAREER.getCode()
      );
    }

    return student;
  }

  public SdcSchoolCollectionStudentEntity updateFteColumns(FteCalculationResult fteCalculationResult, UUID sdcSchoolCollectionStudentID) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    var student = sdcSchoolCollectionStudentEntityOptional.orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionStudentId", sdcSchoolCollectionStudentID.toString()));

    student.setFte(fteCalculationResult.getFte());
    student.setFteZeroReasonCode(fteCalculationResult.getFteZeroReason());

    return student;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity softDeleteSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    SdcSchoolCollectionStudentEntity student = sdcSchoolCollectionStudentEntityOptional.orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionStudentId", sdcSchoolCollectionStudentID.toString()));

    student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.toString());

    return sdcSchoolCollectionStudentRepository.save(student);
  }

  private SdcStudentSagaData createSagaDataForValidation(SdcSchoolCollectionStudentEntity studentEntity) {
    SdcStudentSagaData sdcStudentSagaData = new SdcStudentSagaData();
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(studentEntity.getSdcSchoolCollectionID());
    if(sdcSchoolCollection.isPresent()) {
      var school = this.restUtils.getSchoolBySchoolID(sdcSchoolCollection.get().getSchoolID().toString());
      sdcStudentSagaData.setCollectionTypeCode(sdcSchoolCollection.get().getCollectionEntity().getCollectionTypeCode());
      school.ifPresent(sdcStudentSagaData::setSchool);
      sdcStudentSagaData.setSdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolCollectionStudentWithValidationIssues(studentEntity));
      return sdcStudentSagaData;
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "SdcSchoolCollectionEntity", studentEntity.getSdcSchoolCollectionID().toString());
    }
  }
}
