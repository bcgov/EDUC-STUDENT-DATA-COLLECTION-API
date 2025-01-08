package ca.bc.gov.educ.studentdatacollection.api.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Component
@Slf4j
public class PurgeOldSagaRecordsScheduler {
  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;

  @Getter(PRIVATE)
  private final SagaEventRepository sagaEventRepository;

  private static final int BATCH_SIZE = 10000;

  public PurgeOldSagaRecordsScheduler(final SagaRepository sagaRepository, final SagaEventRepository sagaEventRepository) {
    this.sagaRepository = sagaRepository;
    this.sagaEventRepository = sagaEventRepository;
  }


  /**
   * run the job based on configured scheduler(a cron expression) and purge old records from DB.
   */
  @Scheduled(cron = "${scheduled.jobs.purge.old.saga.records.cron}")
  @SchedulerLock(name = "PurgeOldSagaRecordsLock", lockAtLeastFor = "${scheduled.jobs.purge.old.saga.records.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.purge.old.saga.records.cron.lockAtMostFor}")
  @Transactional
  public void pollSagaTableAndPurgeOldRecords() {
    LockAssert.assertLocked();
    final List<String> cleanupStatus = Collections.singletonList("COMPLETED");
    final long cleanupRecordsCount = this.sagaRepository.countAllByStatusIn(cleanupStatus);
    for (int i = 0; i < cleanupRecordsCount; i += BATCH_SIZE) {
        List<UUID> sagaIDsToDelete = this.sagaRepository.findByStatusIn(cleanupStatus, BATCH_SIZE);
        this.sagaRepository.deleteBySagaIdIn(sagaIDsToDelete);
     }
    log.info("Purged old saga and event records from EDUC-STUDENT-DATA-COLLECTION-SAGA-API");
  }
}
