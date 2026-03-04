package ca.bc.gov.educ.studentdatacollection.api.orchestrator.email;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.EmailService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.struct.EmailSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.EMAIL_SENT;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.SEND_EMAIL;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;

@Component
@Slf4j
public class DistrictSignoffNotificationEmailOrchestrator extends BaseOrchestrator<EmailSagaData> {

    private final EmailService emailService;

    protected DistrictSignoffNotificationEmailOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, EmailService emailService) {
        super(sagaService, messagePublisher, EmailSagaData.class, SagaEnum.DISTRICT_SIGNOFF_NOTIFICATION_EMAIL_SAGA.toString(), TopicsEnum.DISTRICT_SIGNOFF_NOTIFICATION_EMAIL_SAGA_TOPIC.toString());
        this.emailService = emailService;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(SEND_EMAIL, this::sendEmail)
                .end(SEND_EMAIL, EMAIL_SENT, this::completeSendEmail);
    }

    public void sendEmail(final Event event, final SdcSagaEntity saga, final EmailSagaData emailSagaData) {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(SEND_EMAIL.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        this.emailService.sendEmail(emailSagaData);

        final Event.EventBuilder eventBuilder = Event.builder();
        eventBuilder.sagaId(saga.getSagaId()).eventType(SEND_EMAIL);
        eventBuilder.eventOutcome(EMAIL_SENT);

        val nextEvent = eventBuilder.build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
    }

    private void completeSendEmail(final Event event, final SdcSagaEntity saga, final EmailSagaData emailSagaData) {
        //This is ok
    }
}

