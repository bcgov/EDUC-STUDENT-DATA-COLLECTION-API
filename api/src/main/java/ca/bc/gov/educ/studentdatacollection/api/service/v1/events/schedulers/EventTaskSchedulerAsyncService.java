package ca.bc.gov.educ.studentdatacollection.api.service.v1.events.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.helpers.LogHelper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.Saga;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class EventTaskSchedulerAsyncService {
  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;

  @Getter(PRIVATE)
  private final SdcSchoolStudentRepository sdcSchoolStudentRepository;

  @Getter(PRIVATE)
  private final Map<String, Orchestrator> sagaOrchestrators = new HashMap<>();

  @Getter(PRIVATE)
  private final SdcService sdcService;
  @Setter
  private List<String> statusFilters;

  public EventTaskSchedulerAsyncService(final List<Orchestrator> orchestrators, final SagaRepository sagaRepository, final SdcSchoolStudentRepository sdcSchoolStudentRepository, final SdcService sdcService) {
    this.sagaRepository = sagaRepository;
    this.sdcSchoolStudentRepository = sdcSchoolStudentRepository;
    this.sdcService = sdcService;
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

  private void processUncompletedSagas(final List<Saga> sagas) {
    for (val saga : sagas) {
      if (saga.getUpdateDate().isBefore(LocalDateTime.now().minusMinutes(2))
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
    if (this.getSagaRepository().countAllByStatusIn(this.getStatusFilters()) > 20) { // at max there will be 40 parallel sagas.
      log.debug("Saga count is greater than 20, so not processing student records");
      return;
    }
    final List<SdcSchoolStudentEntity> studentEntities = new ArrayList<>();
    final var sdcSchoolStudentEntities = this.getSdcSchoolStudentRepository().findTop100ByStatusOrderByCreateDate(SdcSchoolStudentStatus.LOADED.toString());
    log.debug("found :: {}  records in loaded status", sdcSchoolStudentEntities.size());
    if (!sdcSchoolStudentEntities.isEmpty()) {
      for (val entity : sdcSchoolStudentEntities) {
        if (this.getSagaRepository().findBySdcStudentIDAndSagaName(entity.getSdcSchoolStudentID(), SagaEnum.STUDENT_DATA_COLLECTION_STUDENT_PROCESSING_SAGA.toString()).isEmpty()) {
          studentEntities.add(entity);
        }
      }
    }
    if (!studentEntities.isEmpty()) {
      this.getSdcService().prepareAndSendSdcStudentsForFurtherProcessing(studentEntities);
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

  private void setRetryCountAndLog(final Saga saga) {
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
