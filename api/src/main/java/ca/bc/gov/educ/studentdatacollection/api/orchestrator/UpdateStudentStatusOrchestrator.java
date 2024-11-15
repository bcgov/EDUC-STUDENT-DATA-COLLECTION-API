package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CloseCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.UpdateStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.SDC_STUDENT_STATUS_UPDATED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.UPDATE_SDC_STUDENT_STATUS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum.UPDATE_STUDENT_STATUS_SAGA;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.UPDATE_STUDENT_STATUS_SAGA_TOPIC;

@Component
@Slf4j
public class UpdateStudentStatusOrchestrator extends BaseOrchestrator<UpdateStudentSagaData> {

    private final CloseCollectionService closeCollectionService;

    protected UpdateStudentStatusOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, CloseCollectionService closeCollectionService) {
        super(sagaService, messagePublisher, UpdateStudentSagaData.class, UPDATE_STUDENT_STATUS_SAGA.toString(), UPDATE_STUDENT_STATUS_SAGA_TOPIC.toString());
        this.closeCollectionService = closeCollectionService;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(UPDATE_SDC_STUDENT_STATUS, this::updateSdcStudentStatus)
                .end(UPDATE_SDC_STUDENT_STATUS, SDC_STUDENT_STATUS_UPDATED);
    }

    public void updateSdcStudentStatus(final Event event, final SdcSagaEntity saga, final UpdateStudentSagaData updateStudentSagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_SDC_STUDENT_STATUS.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        //service call
        closeCollectionService.markStudentAsCompleted(updateStudentSagaData);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(UPDATE_SDC_STUDENT_STATUS)
                .eventOutcome(SDC_STUDENT_STATUS_UPDATED)
                .eventPayload(JsonUtil.getJsonStringFromObject(updateStudentSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("message sent to UPDATE_STUDENT_DOWNSTREAM_SAGA_TOPIC for UPDATE_SDC_STUDENT_STATUS Event.");
    }
}
