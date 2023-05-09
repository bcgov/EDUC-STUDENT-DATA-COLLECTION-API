package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
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
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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


  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteExistingAndWriteEnrolledProgramCodes(UUID sdcSchoolCollectionStudentID, List<String> enrolledProgramCodes) {
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

    sdcSchoolCollectionStudentRepository.save(student);
  }

}
