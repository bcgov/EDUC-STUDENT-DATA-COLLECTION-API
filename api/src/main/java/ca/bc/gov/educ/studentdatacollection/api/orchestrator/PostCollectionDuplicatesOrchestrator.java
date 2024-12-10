package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.DuplicatePostingSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.DUPLICATES_POSTED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.POST_DUPLICATES;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;

@Component
@Slf4j
public class PostCollectionDuplicatesOrchestrator extends BaseOrchestrator<DuplicatePostingSagaData> {
  private final SdcDuplicatesService sdcDuplicatesService;

  protected PostCollectionDuplicatesOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, SdcDuplicatesService sdcDuplicatesService) {
    super(sagaService, messagePublisher, DuplicatePostingSagaData.class, SagaEnum.POST_DUPLICATES_SAGA.toString(), TopicsEnum.POST_DUPLICATES_SAGA_TOPIC.toString());
    this.sdcDuplicatesService = sdcDuplicatesService;
  }

  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .begin(POST_DUPLICATES, this::postProvincialDuplicates)
      .end(POST_DUPLICATES, DUPLICATES_POSTED, this::completeSdcStudentSagaWithError);
  }

  public void postProvincialDuplicates(final Event event, final SdcSagaEntity saga, final DuplicatePostingSagaData duplicatePostingSagaData) {
    final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(POST_DUPLICATES.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    this.sdcDuplicatesService.generateAllProvincialDuplicates(duplicatePostingSagaData.getCollectionID());

    final Event.EventBuilder eventBuilder = Event.builder();
    eventBuilder.sagaId(saga.getSagaId()).eventType(POST_DUPLICATES);
    eventBuilder.eventOutcome(DUPLICATES_POSTED);

    val nextEvent = eventBuilder.build();
    this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
    log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
  }

  private void completeSdcStudentSagaWithError(final Event event, final SdcSagaEntity saga, final DuplicatePostingSagaData duplicatePostingSagaData) {
    //This is ok
  }

}
