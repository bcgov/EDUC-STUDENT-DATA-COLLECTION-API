package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CloseCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.UpdateStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum.UPDATE_STUDENT_DOWNSTREAM_SAGA;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.*;

@Component
@Slf4j
public class UpdateStudentDownstreamOrchestrator extends BaseOrchestrator<UpdateStudentSagaData> {

    private final CloseCollectionService closeCollectionService;
    private final RestUtils restUtils;

    protected UpdateStudentDownstreamOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, CloseCollectionService closeCollectionService, RestUtils restUtils) {
        super(sagaService, messagePublisher, UpdateStudentSagaData.class, UPDATE_STUDENT_DOWNSTREAM_SAGA.toString(), UPDATE_STUDENT_DOWNSTREAM_SAGA_TOPIC.toString());
        this.closeCollectionService = closeCollectionService;
        this.restUtils = restUtils;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(UPDATE_STUDENT, this::updateStudent)
                .step(UPDATE_STUDENT, STUDENT_UPDATED, UPDATE_SDC_STUDENT_STATUS, this::updateSdcStudentStatus)
                .end(UPDATE_SDC_STUDENT_STATUS, SDC_STUDENT_STATUS_UPDATED);
    }

    public void updateStudent(final Event event, final SdcSagaEntity saga, final UpdateStudentSagaData updateStudentSagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_STUDENT.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        final Student studentDataFromEventResponse = this.restUtils.getStudentByPEN(UUID.randomUUID(), updateStudentSagaData.getAssignedPEN());
        log.debug("Student from studentApi" + studentDataFromEventResponse);
        if(studentDataFromEventResponse != null) {
            studentDataFromEventResponse.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
            studentDataFromEventResponse.setMincode(updateStudentSagaData.getMincode());
            studentDataFromEventResponse.setLocalID(updateStudentSagaData.getLocalID());
            studentDataFromEventResponse.setGradeCode(updateStudentSagaData.getGradeCode());
            updateGradeYear(studentDataFromEventResponse, updateStudentSagaData);
            updateUsualNameFields(studentDataFromEventResponse, updateStudentSagaData);
            studentDataFromEventResponse.setPostalCode(updateStudentSagaData.getPostalCode());
            studentDataFromEventResponse.setHistoryActivityCode("REQMATCH");

            final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                    .eventType(UPDATE_STUDENT)
                    .replyTo(this.getTopicToSubscribe())
                    .eventPayload(JsonUtil.getJsonStringFromObject(studentDataFromEventResponse))
                    .build();
            this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
        } else {
            final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                    .eventType(UPDATE_STUDENT)
                    .eventOutcome(STUDENT_UPDATED)
                    .eventPayload(JsonUtil.getJsonStringFromObject(updateStudentSagaData))
                    .build();
            this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        }
        log.info("message sent to STUDENT_API_TOPIC for UPDATE_STUDENT Event.");
    }

    public void updateSdcStudentStatus(final Event event, final SdcSagaEntity saga, final UpdateStudentSagaData updateStudentSagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_SDC_STUDENT_STATUS.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        log.debug("Updating student status in sdc service");
        //service call
        closeCollectionService.markStudentAsCompleted(updateStudentSagaData);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(UPDATE_SDC_STUDENT_STATUS)
                .eventOutcome(SDC_STUDENT_STATUS_UPDATED)
                .eventPayload(JsonUtil.getJsonStringFromObject(updateStudentSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);

    }

    protected void updateGradeYear(final Student studentDataFromEventResponse, final UpdateStudentSagaData updateStudentSagaData) {
        var incomingGradeCode = updateStudentSagaData.getGradeCode();
        if (StringUtils.isBlank(studentDataFromEventResponse.getGradeCode())) {
            studentDataFromEventResponse.setGradeCode(incomingGradeCode);
            val localDateTime = LocalDateTime.now();
            if (localDateTime.getMonthValue() > 6) {
                studentDataFromEventResponse.setGradeYear(String.valueOf(localDateTime.getYear()));
            } else {
                studentDataFromEventResponse.setGradeYear(String.valueOf(localDateTime.getYear() - 1));
            }
        }
    }

    protected void updateUsualNameFields(final Student studentFromStudentAPI, final UpdateStudentSagaData updateStudentSagaData) {
        studentFromStudentAPI.setUsualFirstName(updateStudentSagaData.getUsualFirstName());
        studentFromStudentAPI.setUsualLastName(updateStudentSagaData.getUsualLastName());
        studentFromStudentAPI.setUsualMiddleNames(updateStudentSagaData.getUsualMiddleNames());
    }
}
