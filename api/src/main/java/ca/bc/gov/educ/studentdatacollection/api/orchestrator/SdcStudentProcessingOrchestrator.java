package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorChainProcessor;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.helpers.SdcHelper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityRulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.rules.RulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.STUDENT_PROCESSED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.PROCESS_SDC_STUDENT;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;

@Component
@Slf4j
public class SdcStudentProcessingOrchestrator extends BaseOrchestrator<SdcStudentSagaData> {
  private final RulesProcessor rulesProcessor;
  private final ProgramEligibilityRulesProcessor programEligibilityRulesProcessor;
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;
  private final FteCalculatorChainProcessor fteCalculatorChainProcessor;

  protected SdcStudentProcessingOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final RulesProcessor rulesProcessor, final ProgramEligibilityRulesProcessor programEligibilityRulesProcessor, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService, FteCalculatorChainProcessor fteCalculatorChainProcessor) {
    super(sagaService, messagePublisher, SdcStudentSagaData.class, SagaEnum.STUDENT_DATA_COLLECTION_STUDENT_PROCESSING_SAGA.toString(), TopicsEnum.STUDENT_DATA_COLLECTION_PROCESS_STUDENT_SAGA_TOPIC.toString());
    this.rulesProcessor = rulesProcessor;
    this.programEligibilityRulesProcessor = programEligibilityRulesProcessor;
    this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
    this.fteCalculatorChainProcessor = fteCalculatorChainProcessor;
  }

  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .begin(PROCESS_SDC_STUDENT, this::processStudentRecord)
      .end(PROCESS_SDC_STUDENT, STUDENT_PROCESSED, this::completeSdcStudentSagaWithError);
  }

//  @Transactional(propagation = Propagation.REQUIRES_NEW)
  private void processStudentRecord(final Event event, final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) throws JsonProcessingException {
    final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_SDC_STUDENT.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = this.sdcSchoolCollectionStudentService.getSdcSchoolCollectionStudent(UUID.fromString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionID()));

    StudentRuleData ruleStudent = new StudentRuleData();
    ruleStudent.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentEntity);
    ruleStudent.setSchool(sdcStudentSagaData.getSchool());

    validateStudent(ruleStudent);
    calculateAdditionalStudentAttributes(ruleStudent);

    final Event.EventBuilder eventBuilder = Event.builder();
    eventBuilder.sagaId(saga.getSagaId()).eventType(PROCESS_SDC_STUDENT);
    eventBuilder.eventOutcome(STUDENT_PROCESSED);

    val nextEvent = eventBuilder.build();
    this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
    log.info("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
  }

  private void calculateAdditionalStudentAttributes(final StudentRuleData studentRuleData) {
    var sdcSchoolCollectionStudentEntity = studentRuleData.getSdcSchoolCollectionStudentEntity();
    sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().clear();

    // Write Enrolled Program Codes
    if(StringUtils.isNotBlank(sdcSchoolCollectionStudentEntity.getEnrolledProgramCodes())) {
      List<String> enrolledProgramList = TransformUtil.splitIntoChunks(sdcSchoolCollectionStudentEntity.getEnrolledProgramCodes(), 2);
      this.sdcSchoolCollectionStudentService.writeEnrolledProgramCodes(sdcSchoolCollectionStudentEntity, enrolledProgramList);
    }

    // Update Student age columns
    String studentDOB = sdcSchoolCollectionStudentEntity.getDob();
    sdcSchoolCollectionStudentEntity.setIsAdult(DOBUtil.isAdult(studentDOB));
    sdcSchoolCollectionStudentEntity.setIsSchoolAged(DOBUtil.isSchoolAged(studentDOB));

    // Update program eligibility
    this.sdcSchoolCollectionStudentService.clearSdcSchoolStudentProgramEligibilityColumns(sdcSchoolCollectionStudentEntity);
    List<ProgramEligibilityIssueCode> programEligibilityErrors = this.programEligibilityRulesProcessor.processRules(studentRuleData);
    this.sdcSchoolCollectionStudentService.updateProgramEligibilityColumns(programEligibilityErrors, studentRuleData.getSdcSchoolCollectionStudentEntity());

    // Calculate Fte
    var fteResults = this.fteCalculatorChainProcessor.processFteCalculator(studentRuleData);
    this.sdcSchoolCollectionStudentService.updateFteColumns(fteResults, sdcSchoolCollectionStudentEntity);

    this.sdcSchoolCollectionStudentService.saveSdcSchoolCollectionStudent(sdcSchoolCollectionStudentEntity);
  }

  private void validateStudent(final StudentRuleData studentRuleData){
    val validationErrors = this.rulesProcessor.processRules(studentRuleData);
    var entity = studentRuleData.getSdcSchoolCollectionStudentEntity();
    entity.getSDCStudentValidationIssueEntities().clear();
    entity.getSDCStudentValidationIssueEntities().addAll(SdcHelper.populateValidationErrors(validationErrors, entity));
    if(!validationErrors.isEmpty()) {
      entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.HAS_ISSUES.toString());
    }else{
      entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.VERIFIED.toString());
    }
  }

  private void completeSdcStudentSagaWithError(final Event event, final SdcSagaEntity saga, final SdcStudentSagaData sdcStudentSagaData) {
  }

}
