package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorChainProcessor;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.PenMatchSagaMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityRulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.rules.RulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.grad.v1.GradStatusResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.PEN_MATCH_API_TOPIC;

@Component
@Slf4j
public class SdcStudentProcessingOrchestrator extends BaseOrchestrator<SdcStudentSagaData> {
  private final RulesProcessor rulesProcessor;
  private final ProgramEligibilityRulesProcessor programEligibilityRulesProcessor;
  private final RestUtils restUtils;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;
  private final FteCalculatorChainProcessor fteCalculatorChainProcessor;

  protected SdcStudentProcessingOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final RulesProcessor rulesProcessor, final ProgramEligibilityRulesProcessor programEligibilityRulesProcessor, final RestUtils restUtils, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService, FteCalculatorChainProcessor fteCalculatorChainProcessor) {
    super(sagaService, messagePublisher, SdcStudentSagaData.class, SagaEnum.STUDENT_DATA_COLLECTION_STUDENT_PROCESSING_SAGA.toString(), TopicsEnum.STUDENT_DATA_COLLECTION_PROCESS_STUDENT_SAGA_TOPIC.toString());
    this.rulesProcessor = rulesProcessor;
    this.programEligibilityRulesProcessor = programEligibilityRulesProcessor;
    this.restUtils = restUtils;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
    this.fteCalculatorChainProcessor = fteCalculatorChainProcessor;
  }

  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .begin(VALIDATE_SDC_STUDENT, this::validateStudent)
      .step(VALIDATE_SDC_STUDENT, VALIDATION_SUCCESS_NO_ERROR_WARNING, PROCESS_PEN_MATCH, this::processPenMatch)
      .end(VALIDATE_SDC_STUDENT, VALIDATION_SUCCESS_WITH_ERROR, this::completeSdcStudentSagaWithError)
      .or()
      .step(PROCESS_PEN_MATCH, PEN_MATCH_PROCESSED, PROCESS_PEN_MATCH_RESULTS, this::processPenMatchResults)
      .step(PROCESS_PEN_MATCH_RESULTS, PEN_MATCH_RESULTS_PROCESSED, FETCH_GRAD_STATUS, this::fetchGradStatus)
      .step(FETCH_GRAD_STATUS, GRAD_STATUS_FETCHED, PROCESS_GRAD_STATUS_RESULT, this::processGradStatusResults)
      .step(PROCESS_GRAD_STATUS_RESULT, GRAD_STATUS_RESULTS_PROCESSED, CALCULATE_ADDITIONAL_STUDENT_ATTRIBUTES, this::calculateAdditionalStudentAttributes)
      .end(CALCULATE_ADDITIONAL_STUDENT_ATTRIBUTES, ADDITIONAL_STUDENT_ATTRIBUTES_CALCULATED);
  }

  protected void fetchGradStatus(final Event event, final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(FETCH_GRAD_STATUS.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    this.postToGradAPI(saga, sdcStudentSagaData);
  }

  protected void postToGradAPI(final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    //Uncomment this when the GRAD service is ready
//    val sdcSchoolStudent = sdcStudentSagaData.getSdcSchoolCollectionStudent();
//    val gradStatusRequest = JsonUtil.mapper.writeValueAsString(sdcSchoolStudent.getAssignedPen());
//    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
//      .eventType(FETCH_GRAD_STATUS)
//      .replyTo(this.getTopicToSubscribe())
//      .eventPayload(gradStatusRequest)
//      .build();
//    this.postMessageToTopic(GRAD_API_TOPIC.toString(), nextEvent);
    log.info("message sent to GRAD_API_TOPIC for FETCH_GRAD_STATUS Event. :: {}", saga.getSagaId());

    //Remove the following when the GRAD service is ready
    this.postMessageToTopic(this.getTopicToSubscribe(), Event.builder().sagaId(saga.getSagaId())
      .eventType(FETCH_GRAD_STATUS).eventOutcome(GRAD_STATUS_FETCHED)
      .build());
  }

  private void calculateAdditionalStudentAttributes(final Event event, final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) {
    final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(CALCULATE_ADDITIONAL_STUDENT_ATTRIBUTES.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    // Write Enrolled Program Codes
    if(StringUtils.isNotBlank(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes())) {
      List<String> enrolledProgramList = TransformUtil.splitIntoChunks(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes(), 2);

      this.sdcSchoolCollectionStudentService.deleteExistingAndWriteEnrolledProgramCodes(UUID.fromString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID()), enrolledProgramList);
    }

    // Update Student age columns
    UUID studentUUID = UUID.fromString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID());
    String studentDOB = sdcStudentSagaData.getSdcSchoolCollectionStudent().getDob();

    this.sdcSchoolCollectionStudentService.updateStudentAgeColumns(studentUUID, DOBUtil.isAdult(studentDOB), DOBUtil.isSchoolAged(studentDOB));

    // Update program eligibility
    this.sdcSchoolCollectionStudentService.clearSdcSchoolStudentProgramEligibilityColumns(studentUUID);
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> programEligibilityErrors =
      this.programEligibilityRulesProcessor.processRules(sdcStudentSagaData);

    this.sdcSchoolCollectionStudentService.updateProgramEligibilityColumns(programEligibilityErrors, studentUUID);

    // Calculate Fte
    var fteResults = this.fteCalculatorChainProcessor.processFteCalculator(sdcStudentSagaData);
    this.sdcSchoolCollectionStudentService.updateFteColumns(fteResults, studentUUID);

    this.postMessageToTopic(this.getTopicToSubscribe(), Event.builder().sagaId(saga.getSagaId())
      .eventType(CALCULATE_ADDITIONAL_STUDENT_ATTRIBUTES).eventOutcome(ADDITIONAL_STUDENT_ATTRIBUTES_CALCULATED)
      .build());
  }

  protected void processGradStatusResults(final Event event, final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_GRAD_STATUS_RESULT.toString());
//    final var gradStatusResult = JsonUtil.getJsonObjectFromString(GradStatusResult.class, event.getEventPayload());
//    sdcStudentSagaData.setGradStatus(gradStatusResult.getGradStatus());
    GradStatusResult gradStatusResult = new GradStatusResult();
    gradStatusResult.setGradStatus("false");
    saga.setPayload(JsonUtil.getJsonStringFromObject(sdcStudentSagaData));
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    val sdcStudOptional = this.sdcSchoolCollectionStudentService.findBySdcSchoolStudentID(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID());
    if (sdcStudOptional.isPresent()) {
      val sdcStud = sdcStudOptional.get();
      sdcStud.setIsGraduated(gradStatusResult.getGradStatus().equals("true"));
      this.sdcSchoolCollectionStudentService.saveSdcSchoolCollectionStudent(sdcStud);
    }
    this.postMessageToTopic(this.getTopicToSubscribe(), Event.builder().sagaId(saga.getSagaId())
      .eventType(PROCESS_GRAD_STATUS_RESULT).eventOutcome(GRAD_STATUS_RESULTS_PROCESSED)
      .eventPayload(gradStatusResult.getGradStatus()).build());
  }

  protected void processPenMatchResults(final Event event, final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
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
    val sdcStudOptional = this.sdcSchoolCollectionStudentService.findBySdcSchoolStudentID(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID());
    if (sdcStudOptional.isPresent()) {
      val sdcStud = sdcStudOptional.get();
      if (assignedPEN.isPresent()) {
        sdcStud.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.VERIFIED.toString());
      } else {
        sdcStud.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.FIXABLE.toString());
      }
      this.sdcSchoolCollectionStudentService.saveSdcSchoolCollectionStudent(sdcStud);
    }
    this.postMessageToTopic(this.getTopicToSubscribe(), Event.builder().sagaId(saga.getSagaId())
      .eventType(PROCESS_PEN_MATCH_RESULTS).eventOutcome(PEN_MATCH_RESULTS_PROCESSED)
      .eventPayload(penMatchResult.getPenStatus()).build());
  }

  private void completeSdcStudentSagaWithError(final Event event, final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final TypeReference<List<SdcSchoolCollectionStudentValidationIssue>> responseType = new TypeReference<>() {
    };
    val validationResults = JsonUtil.mapper.readValue(event.getEventPayload(), responseType);
    this.sdcSchoolCollectionStudentService.deleteSdcStudentValidationErrors(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID());
    this.sdcSchoolCollectionStudentService.saveSdcSchoolStudentValidationErrors(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID(), validationResults, null);
  }

  protected void processPenMatch(final Event event, final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    this.postToPenMatchAPI(saga, sdcStudentSagaData);
  }

  protected void postToPenMatchAPI(final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    val sdcSchoolStudent = sdcStudentSagaData.getSdcSchoolCollectionStudent();
    var sdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(UUID.fromString(sdcSchoolStudent.getSdcSchoolCollectionID()));
    var school = this.restUtils.getSchoolBySchoolID(sdcSchoolCollection.get().getSchoolID().toString());
    if(school.isPresent()) {
      final String mincode = school.get().getMincode();
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
    }else{
      throw new StudentDataCollectionAPIRuntimeException("School was not found for schoolID " + sdcSchoolCollection.get().getSchoolID() + " :: this should not have happened");
    }
  }

  protected void validateStudent(final Event event, final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(VALIDATE_SDC_STUDENT.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val validationErrors = this.rulesProcessor.processRules(sdcStudentSagaData);
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
