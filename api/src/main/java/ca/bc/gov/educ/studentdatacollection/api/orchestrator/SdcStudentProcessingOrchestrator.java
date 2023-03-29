package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.PenMatchSagaMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStates;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSaga;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.rules.RulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcService;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.PEN_MATCH_API_TOPIC;

@Component
@Slf4j
public class SdcStudentProcessingOrchestrator extends BaseOrchestrator<SdcStudentSagaData> {
  private final RulesProcessor rulesProcessor;
  private final SdcService sdcService;
  private final RestUtils restUtils;
  private final SdcSchoolCollectionService sdcSchoolCollectionService;

  protected SdcStudentProcessingOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final RulesProcessor rulesProcessor, final SdcService sdcService, final RestUtils restUtils, SdcSchoolCollectionService sdcSchoolCollectionService) {
    super(sagaService, messagePublisher, SdcStudentSagaData.class, SagaEnum.STUDENT_DATA_COLLECTION_STUDENT_PROCESSING_SAGA.toString(), TopicsEnum.STUDENT_DATA_COLLECTION_PROCESS_STUDENT_SAGA_TOPIC.toString());
    this.rulesProcessor = rulesProcessor;
    this.sdcService = sdcService;
    this.restUtils = restUtils;
    this.sdcSchoolCollectionService = sdcSchoolCollectionService;
  }

  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .begin(VALIDATE_SDC_STUDENT, this::validateStudent)
      .step(VALIDATE_SDC_STUDENT, VALIDATION_SUCCESS_NO_ERROR_WARNING, PROCESS_PEN_MATCH, this::processPenMatch)
      .end(VALIDATE_SDC_STUDENT, VALIDATION_SUCCESS_WITH_ERROR, this::completeSdcStudentSagaWithError)
      .or()
      .step(PROCESS_PEN_MATCH, PEN_MATCH_PROCESSED, PROCESS_PEN_MATCH_RESULTS, this::processPenMatchResults)
      .end(PROCESS_PEN_MATCH_RESULTS, PEN_MATCH_RESULTS_PROCESSED);
  }

  protected void processPenMatchResults(final Event event, final SdcSaga saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final SagaEventStates eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH_RESULTS.toString());
    final var penMatchResult = JsonUtil.getJsonObjectFromString(PenMatchResult.class, event.getEventPayload());
    sdcStudentSagaData.setPenMatchResult(penMatchResult); // update the original payload with response from PEN_MATCH_API
    saga.setPayload(JsonUtil.getJsonStringFromObject(sdcStudentSagaData)); // save the updated payload to DB...
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val algorithmStatusCode = penMatchResult.getPenStatus();
    Optional<String> assignedPEN = Optional.empty();
    //system matched status.
    if (StringUtils.equalsIgnoreCase(algorithmStatusCode, "AA")
      || StringUtils.equalsIgnoreCase(algorithmStatusCode, "B1")
      || StringUtils.equalsIgnoreCase(algorithmStatusCode, "C1")
      || StringUtils.equalsIgnoreCase(algorithmStatusCode, "D1")) {
      final var penMatchRecordOptional = penMatchResult.getMatchingRecords().stream().findFirst();
      if (penMatchRecordOptional.isPresent()) {
        assignedPEN = Optional.of(penMatchRecordOptional.get().getMatchingPEN());
      } else {
        log.error("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
        throw new StudentDataCollectionAPIRuntimeException("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
      }
    }
    val sdcStudOptional = this.sdcService.findBySdcSchoolStudentID(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID());
    if (sdcStudOptional.isPresent()) {
      val sdcStud = sdcStudOptional.get();
      if (assignedPEN.isPresent()) {
        sdcStud.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.VERIFIED.toString());
      } else {
        sdcStud.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.FIXABLE.toString());
      }
      this.sdcSchoolCollectionService.saveSdcSchoolCollectionStudent(sdcStud);
    }
    this.postMessageToTopic(this.getTopicToSubscribe(), Event.builder().sagaId(saga.getSagaId())
      .eventType(PROCESS_PEN_MATCH_RESULTS).eventOutcome(PEN_MATCH_RESULTS_PROCESSED)
      .eventPayload(penMatchResult.getPenStatus()).build());
  }

  private void completeSdcStudentSagaWithError(final Event event, final SdcSaga saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final TypeReference<List<SdcSchoolCollectionStudentValidationIssue>> responseType = new TypeReference<>() {
    };
    val validationResults = JsonUtil.mapper.readValue(event.getEventPayload(), responseType);
    this.sdcService.deleteSdcStudentValidationErrors(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID());
    this.sdcService.saveSdcSchoolStudentValidationErrors(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID(), validationResults, null);
  }

  protected void processPenMatch(final Event event, final SdcSaga saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final SagaEventStates eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    this.postToPenMatchAPI(saga, sdcStudentSagaData);
  }

  protected void postToPenMatchAPI(final SdcSaga saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    val sdcSchoolStudent = sdcStudentSagaData.getSdcSchoolCollectionStudent();
    final String mincode = this.restUtils.getSchoolBySchoolID(sdcStudentSagaData.getSchoolID()).get().getMincode();
    val penMatchRequest = PenMatchSagaMapper.mapper.toPenMatchStudent(sdcSchoolStudent, mincode);
    penMatchRequest.setDob(StringUtils.replace(penMatchRequest.getDob(), "-", "")); // pen-match api expects yyyymmdd
    val penMatchRequestJson = JsonUtil.mapper.writeValueAsString(penMatchRequest);
    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(PROCESS_PEN_MATCH)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(penMatchRequestJson)
      .build();
    this.postMessageToTopic(PEN_MATCH_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_MATCH_API_TOPIC for PROCESS_PEN_MATCH Event. :: {}", saga.getSagaId());
  }

  protected void validateStudent(final Event event, final SdcSaga saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final SagaEventStates eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(VALIDATE_SDC_STUDENT.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val validationErrors = this.rulesProcessor.processRules(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudentEntity(sdcStudentSagaData.getSdcSchoolCollectionStudent()));
    final Event.EventBuilder eventBuilder = Event.builder();
    eventBuilder.sagaId(saga.getSagaId()).eventType(VALIDATE_SDC_STUDENT);
    if (validationErrors.isEmpty()) {
      eventBuilder.eventOutcome(VALIDATION_SUCCESS_NO_ERROR_WARNING);
      eventBuilder.eventPayload("");
    } else {
      eventBuilder.eventOutcome(VALIDATION_SUCCESS_WITH_ERROR);
      eventBuilder.eventPayload(JsonUtil.getJsonStringFromObject(validationErrors));
    }
    val nextEvent = eventBuilder.build();
    this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
    log.info("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
  }
}
