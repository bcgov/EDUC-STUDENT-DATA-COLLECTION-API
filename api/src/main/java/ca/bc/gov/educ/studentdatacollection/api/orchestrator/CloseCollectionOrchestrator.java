package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
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

    protected CloseCollectionOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, CloseCollectionService closeCollectionService) {
        super(sagaService, messagePublisher, CollectionSagaData.class, SagaEnum.CLOSE_COLLECTION_SAGA.toString(), TopicsEnum.CLOSE_COLLECTION_TOPIC.toString());
        this.closeCollectionService = closeCollectionService;
    }
    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION, this::closeCurrentCollAndOpenNewColl)
                .step(CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION, NEW_COLLECTION_CREATED, SEND_CLOSURE_NOTIFICATIONS, this::sendClosureNotifications)
                .end(SEND_CLOSURE_NOTIFICATIONS, CLOSURE_NOTIFICATIONS_DISPATCHED);
    }

    public void closeCurrentCollAndOpenNewColl(final Event event, final SdcSagaEntity saga, final CollectionSagaData collectionSagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        //service call
        closeCollectionService.closeCurrentCollAndOpenNewCollection(collectionSagaData);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION)
                .eventOutcome(NEW_COLLECTION_CREATED)
                .eventPayload(JsonUtil.getJsonStringFromObject(collectionSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
    }

    public void sendClosureNotifications(final Event event, final SdcSagaEntity saga, final CollectionSagaData collectionSagaData) {

    }
}
