package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CloseCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.struct.CollectionSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;

@Component
@Slf4j
public class CloseCollectionOrchestrator extends BaseOrchestrator<CollectionSagaData> {

    private final CloseCollectionService closeCollectionService;
    private final Publisher publisher;

    protected CloseCollectionOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, CloseCollectionService closeCollectionService, Publisher publisher) {
        super(sagaService, messagePublisher, CollectionSagaData.class, SagaEnum.CLOSE_COLLECTION_SAGA.toString(), TopicsEnum.CLOSE_COLLECTION_TOPIC.toString());
        this.closeCollectionService = closeCollectionService;
        this.publisher = publisher;
    }
    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION, this::closeCurrentCollAndOpenNewColl)
                .step(CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION, NEW_COLLECTION_CREATED, SAVE_FUNDING_GROUP_SNAPSHOT, this::saveFundingGroupSnapshot)
                .step(SAVE_FUNDING_GROUP_SNAPSHOT, FUNDING_GROUP_SNAPSHOT_SAVED, SEND_CLOSURE_NOTIFICATIONS, this::sendClosureNotifications)
                .end(SEND_CLOSURE_NOTIFICATIONS, CLOSURE_NOTIFICATIONS_DISPATCHED);
    }

    public void closeCurrentCollAndOpenNewColl(final Event event, final SdcSagaEntity saga, final CollectionSagaData collectionSagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        //service call
        closeCollectionService.closeCurrentCollAndOpenNewCollection(collectionSagaData);

        postEvent(saga, collectionSagaData, CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION, NEW_COLLECTION_CREATED);
    }

    public void saveFundingGroupSnapshot(final Event event, final SdcSagaEntity saga, final CollectionSagaData collectionSagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SAVE_FUNDING_GROUP_SNAPSHOT.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        //service call
        closeCollectionService.saveIndependentSchoolFundingGroupSnapshot(collectionSagaData);

        postEvent(saga, collectionSagaData, SAVE_FUNDING_GROUP_SNAPSHOT, FUNDING_GROUP_SNAPSHOT_SAVED);
    }

    public void sendClosureNotifications(final Event event, final SdcSagaEntity saga, final CollectionSagaData collectionSagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SEND_CLOSURE_NOTIFICATIONS.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        //service call
        closeCollectionService.sendClosureNotification(collectionSagaData);

        postEvent(saga, collectionSagaData, SEND_CLOSURE_NOTIFICATIONS, CLOSURE_NOTIFICATIONS_DISPATCHED);
    }

    private void publishToJetStream(final Event event, SdcSagaEntity saga) {
        publisher.dispatchChoreographyEvent(event, saga);
    }

    private void postEvent(final SdcSagaEntity saga, final CollectionSagaData collectionSagaData, EventType eventType, EventOutcome eventOutcome) throws JsonProcessingException{
        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(eventType)
                .eventOutcome(eventOutcome)
                .eventPayload(JsonUtil.getJsonStringFromObject(collectionSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        publishToJetStream(nextEvent, saga);
        log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
    }
}
