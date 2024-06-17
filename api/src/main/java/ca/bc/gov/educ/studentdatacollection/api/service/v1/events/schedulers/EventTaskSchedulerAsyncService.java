package ca.bc.gov.educ.studentdatacollection.api.service.v1.events.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.helpers.LogHelper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.studentdatacollection.api.properties.EmailProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ScheduleHandlerService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class EventTaskSchedulerAsyncService {

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

  @Getter(PRIVATE)
  private final EmailProperties emailProperties;

  private final ScheduleHandlerService scheduleHandlerService;

  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;

  @Getter(PRIVATE)
  private final SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository;

  @Getter(PRIVATE)
  private final Map<String, Orchestrator> sagaOrchestrators = new HashMap<>();

  @Getter(PRIVATE)
  private final SdcService sdcService;

  @Getter(PRIVATE)
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

  @Setter
  private List<String> statusFilters;

  @Value("${number.students.process.saga}")
  private String numberOfStudentsToProcess;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionService sdcSchoolCollectionService;

  public EventTaskSchedulerAsyncService(final List<Orchestrator> orchestrators, EmailProperties emailProperties, ScheduleHandlerService scheduleHandlerService, final SagaRepository sagaRepository, final SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository, final SdcService sdcService, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionService sdcSchoolCollectionService) {
    this.emailProperties = emailProperties;
    this.scheduleHandlerService = scheduleHandlerService;
    this.sagaRepository = sagaRepository;
    this.sdcSchoolStudentRepository = sdcSchoolStudentRepository;
    this.sdcService = sdcService;
    this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionService = sdcSchoolCollectionService;
    orchestrators.forEach(orchestrator -> this.sagaOrchestrators.put(orchestrator.getSagaName(), orchestrator));
  }

  @Async("taskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void findAndProcessUncompletedSagas() {
    final var sagas = this.getSagaRepository().findTop100ByStatusInOrderByCreateDate(this.getStatusFilters());
    if (!sagas.isEmpty()) {
      this.processUncompletedSagas(sagas);
    }
  }

  private void processUncompletedSagas(final List<SdcSagaEntity> sagas) {
    for (val saga : sagas) {
      if (saga  .getUpdateDate().isBefore(LocalDateTime.now().minusMinutes(2))
        && this.getSagaOrchestrators().containsKey(saga.getSagaName())) {
        try {
          this.setRetryCountAndLog(saga);
          this.getSagaOrchestrators().get(saga.getSagaName()).replaySaga(saga);
        } catch (final InterruptedException ex) {
          Thread.currentThread().interrupt();
          log.error("InterruptedException while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, ex);
        } catch (final IOException | TimeoutException e) {
          log.error("Exception while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, e);
        }
      }
    }
  }

  @Async("taskExecutor")
  public void findAndPublishLoadedStudentRecordsForProcessing() {
    log.debug("Querying for loaded students to process");
    if (this.getSagaRepository().countAllByStatusIn(this.getStatusFilters()) > 100) { // at max there will be 40 parallel sagas.
      log.info("Saga count is greater than 100, so not processing student records");
      return;
    }
    final var sdcSchoolStudentEntities = this.getSdcSchoolStudentRepository().findTopLoadedStudentForProcessing(numberOfStudentsToProcess);
    log.debug("Found :: {}  records in loaded status", sdcSchoolStudentEntities.size());
    if (!sdcSchoolStudentEntities.isEmpty()) {
      this.getSdcSchoolCollectionStudentService().prepareAndSendSdcStudentsForFurtherProcessing(sdcSchoolStudentEntities);
    }
  }

  @Async("taskExecutor")
  @Transactional
  public void findSchoolCollectionsForSubmission() {
    final List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.findSchoolCollectionsWithStudentsNotInLoadedStatus();
    log.debug("Found :: {}  school collection entities for processing", sdcSchoolCollectionEntity.size());
    if (!sdcSchoolCollectionEntity.isEmpty()) {
      sdcSchoolCollectionEntity.forEach(sdcSchoolCollection -> {
        List<SdcSchoolCollectionStudentEntity> schoolCollectionWithStudentInErrors = sdcSchoolCollection.getSDCSchoolStudentEntities().stream().filter(student -> student.getSdcSchoolCollectionStudentStatusCode().equals(SdcSchoolStudentStatus.ERROR.getCode())).toList();
        if(schoolCollectionWithStudentInErrors.isEmpty()) {
          List<SdcSchoolCollectionStudentEntity> duplicates = sdcSchoolCollectionService.getAllSchoolCollectionDuplicates(sdcSchoolCollection.getSdcSchoolCollectionID());
          if (duplicates.isEmpty()) {
            sdcSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());
          } else {
            sdcSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.VERIFIED.getCode());
          }
        } else {
          sdcSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.LOADED.getCode());
        }
      });
      sdcSchoolCollectionEntity.forEach(sdcSchoolCollectionService::updateSdcSchoolCollection);
    }
  }

  @Async("taskExecutor")
  @Transactional
  public void findAllUnsubmittedIndependentSchoolsInCurrentCollection() {
    final List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntities = sdcSchoolCollectionRepository.findAllUnsubmittedIndependentSchoolsInCurrentCollection();
    log.info("Found :: {} independent schools which have not yet submitted.", sdcSchoolCollectionEntities.size());
    if (!sdcSchoolCollectionEntities.isEmpty()) {
      scheduleHandlerService.createAndStartUnsubmittedEmailSagas(sdcSchoolCollectionEntities);
    }
  }

  public List<String> getStatusFilters() {
    if (this.statusFilters != null && !this.statusFilters.isEmpty()) {
      return this.statusFilters;
    } else {
      final var statuses = new ArrayList<String>();
      statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
      statuses.add(SagaStatusEnum.STARTED.toString());
      return statuses;
    }
  }

  private void setRetryCountAndLog(final SdcSagaEntity saga) {
    Integer retryCount = saga.getRetryCount();
    if (retryCount == null || retryCount == 0) {
      retryCount = 1;
    } else {
      retryCount += 1;
    }
    saga.setRetryCount(retryCount);
    this.getSagaRepository().save(saga);
    LogHelper.logSagaRetry(saga);
  }
}
