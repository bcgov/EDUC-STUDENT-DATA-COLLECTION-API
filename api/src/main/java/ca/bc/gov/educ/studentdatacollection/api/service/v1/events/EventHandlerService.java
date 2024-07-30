package ca.bc.gov.educ.studentdatacollection.api.service.v1.events;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.SdcStudentProcessingOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.UpdateStudentDownstreamOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.UpdateStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * The type Event handler service.
 */
@Service
@Slf4j
public class EventHandlerService {

  @Getter(PRIVATE)
  private final SagaService sagaService;

  private final SdcStudentProcessingOrchestrator studentProcessingOrchestrator;
  private final UpdateStudentDownstreamOrchestrator updateStudentDownstreamOrchestrator;

  @Autowired
  public EventHandlerService(final SagaService sagaService, final SdcStudentProcessingOrchestrator studentProcessingOrchestrator, UpdateStudentDownstreamOrchestrator updateStudentDownstreamOrchestrator) {
    this.sagaService = sagaService;
    this.studentProcessingOrchestrator = studentProcessingOrchestrator;
    this.updateStudentDownstreamOrchestrator = updateStudentDownstreamOrchestrator;
  }

  @Transactional(propagation = REQUIRES_NEW)
  public void handleReadFromTopicEvent(final Event event) throws JsonProcessingException {
    if (event.getEventOutcome() == EventOutcome.READ_FROM_TOPIC_SUCCESS) {
      final SdcStudentSagaData sagaData = JsonUtil.getJsonObjectFromString(SdcStudentSagaData.class, event.getEventPayload());
      final var sagaOptional = this.getSagaService().findBySdcSchoolCollectionStudentIDAndSagaNameAndStatusNot(UUID.fromString(sagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID()), SagaEnum.STUDENT_DATA_COLLECTION_STUDENT_PROCESSING_SAGA.toString(), SagaStatusEnum.COMPLETED.toString());
      if (sagaOptional.isPresent()) { // possible duplicate message.
        log.trace("Execution is not required for this message returning EVENT is :: {}", event);
        return;
      }
      val saga = this.studentProcessingOrchestrator.createSaga(event.getEventPayload(), UUID.fromString(sagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID()), UUID.fromString(sagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionID()), ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      log.info("Starting student processing orchestrator :: {}", saga);
      this.studentProcessingOrchestrator.startSaga(saga);
    }
  }

  @Transactional(propagation = REQUIRES_NEW)
  public void handleUpdateDemogEvent(final Event event) throws JsonProcessingException {
    if (event.getEventOutcome() == EventOutcome.STUDENTS_DEMOG_UPDATED) {
      final UpdateStudentSagaData updateStudentSagaData = JsonUtil.getJsonObjectFromString(UpdateStudentSagaData.class, event.getEventPayload());
      final var sagaOptional = this.getSagaService().findBySdcSchoolCollectionStudentIDAndSagaNameAndStatusNot(UUID.fromString(updateStudentSagaData.getSdcSchoolCollectionStudentID()), SagaEnum.UPDATE_STUDENT_DOWNSTREAM_SAGA.toString(), SagaStatusEnum.COMPLETED.toString());
      if (sagaOptional.isPresent()) { // possible duplicate message.
        log.trace("Execution is not required for this message returning EVENT is :: {}", event);
        return;
      }
      val saga = this.updateStudentDownstreamOrchestrator.createSaga(event.getEventPayload(), UUID.fromString(updateStudentSagaData.getSdcSchoolCollectionStudentID()), null, ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      log.info("Starting updateStudentDownstreamOrchestrator orchestrator :: {}", saga);
      this.updateStudentDownstreamOrchestrator.startSaga(saga);
    }
  }
}
