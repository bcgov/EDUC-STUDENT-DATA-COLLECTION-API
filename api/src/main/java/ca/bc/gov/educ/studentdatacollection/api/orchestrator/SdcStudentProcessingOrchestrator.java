package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.INITIATE_SUCCESS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.STUDENT_PROCESSED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.INITIATED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.PROCESS_SDC_STUDENT;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;

@Component
@Slf4j
public class SdcStudentProcessingOrchestrator extends BaseOrchestrator<SdcStudentSagaData> {
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

  protected SdcStudentProcessingOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService) {
    super(sagaService, messagePublisher, SdcStudentSagaData.class, SagaEnum.STUDENT_DATA_COLLECTION_STUDENT_PROCESSING_SAGA.toString(), TopicsEnum.STUDENT_DATA_COLLECTION_PROCESS_STUDENT_SAGA_TOPIC.toString());
    this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
  }

  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .end(INITIATED, INITIATE_SUCCESS, this::processStudentRecord);
  }

  private void processStudentRecord(final Event event, final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) {
    final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_SDC_STUDENT.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    this.sdcSchoolCollectionStudentService.processStudentRecord(UUID.fromString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID()), sdcStudentSagaData.getSchool(), sdcStudentSagaData.getCollectionTypeCode(), Optional.empty());

    final Event.EventBuilder eventBuilder = Event.builder();
    eventBuilder.sagaId(saga.getSagaId()).eventType(PROCESS_SDC_STUDENT);
    eventBuilder.eventOutcome(STUDENT_PROCESSED);

    val nextEvent = eventBuilder.build();
    this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
    log.info("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
  }

}
