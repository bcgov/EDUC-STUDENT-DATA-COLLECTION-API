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

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum.UPDATE_STUDENT_DOWNSTREAM_SAGA;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.STUDENT_API_TOPIC;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.STUDENT_DATA_COLLECTION_API_TOPIC;

@Component
@Slf4j
public class UpdateStudentDownstreamOrchestrator extends BaseOrchestrator<UpdateStudentSagaData> {

    private final RestUtils restUtils;
    private final CloseCollectionService closeCollectionService;

    protected UpdateStudentDownstreamOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, RestUtils restUtils, CloseCollectionService closeCollectionService) {
        super(sagaService, messagePublisher, UpdateStudentSagaData.class, UPDATE_STUDENT_DOWNSTREAM_SAGA.toString(), STUDENT_DATA_COLLECTION_API_TOPIC.toString());
        this.restUtils = restUtils;
        this.closeCollectionService = closeCollectionService;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(GET_STUDENT, this::getStudentByPen)
                .step(GET_STUDENT, STUDENT_FOUND, UPDATE_STUDENT, this::updateStudent)
                .step(UPDATE_STUDENT, STUDENT_UPDATED, UPDATE_SDC_STUDENT_STATUS, this::updateSdcStudentStatus)
                .end(UPDATE_SDC_STUDENT_STATUS, SDC_STUDENT_STATUS_UPDATED);
    }

    public void getStudentByPen(final Event event, final SdcSagaEntity saga, final UpdateStudentSagaData updateStudentSagaData) {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GET_STUDENT.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(GET_STUDENT)
                .replyTo(this.getTopicToSubscribe())
                .eventPayload(updateStudentSagaData.getAssignedPEN())
                .build();
        this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
        log.info("message sent to STUDENT_API_TOPIC for GET_STUDENT Event.");
    }

    public void updateStudent(final Event event, final SdcSagaEntity saga, final UpdateStudentSagaData updateStudentSagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_STUDENT.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        final Student studentDataFromEventResponse = JsonUtil.getJsonObjectFromString(Student.class, event.getEventPayload());
        studentDataFromEventResponse.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        studentDataFromEventResponse.setMincode(updateStudentSagaData.getMincode());
        studentDataFromEventResponse.setLocalID(updateStudentSagaData.getLocalID());
        updateGradeCodeAndGradeYear(studentDataFromEventResponse, updateStudentSagaData);
        updateUsualNameFields(studentDataFromEventResponse, updateStudentSagaData);
        studentDataFromEventResponse.setPostalCode(updateStudentSagaData.getPostalCode());

        final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(UPDATE_STUDENT)
                .replyTo(this.getTopicToSubscribe())
                .eventPayload(JsonUtil.getJsonStringFromObject(updateStudentSagaData))
                .build();
        this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
        log.info("message sent to STUDENT_API_TOPIC for UPDATE_STUDENT Event.");
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

    }

    protected void updateGradeCodeAndGradeYear(final Student studentDataFromEventResponse, final UpdateStudentSagaData updateStudentSagaData) {
        final var gradeCodes = this.restUtils.getGradeCodes();
        var incomingGradeCode = updateStudentSagaData.getGradeCode();
        val isGradeCodeValid = StringUtils.isNotBlank(incomingGradeCode) && gradeCodes.stream().anyMatch(gradeCode1 -> LocalDateTime.now().isAfter(gradeCode1.getEffectiveDate())
                && LocalDateTime.now().isBefore(gradeCode1.getExpiryDate())
                && StringUtils.equalsIgnoreCase(incomingGradeCode, gradeCode1.getGradeCode()));

        if (isGradeCodeValid && StringUtils.isBlank(studentDataFromEventResponse.getGradeCode())) {
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
