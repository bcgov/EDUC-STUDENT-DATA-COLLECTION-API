package ca.bc.gov.educ.studentdatacollection.api.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

  @Value("${purge.records.saga.after.days}")
  @Setter
  @Getter
  Integer sagaRecordStaleInDays;

  private static final int BATCHSIZE = 10000;

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
    final LocalDateTime createDateToCompare = this.calculateCreateDateBasedOnStaleSagaRecordInDays();
    final List<String> cleanupStatus = Collections.singletonList("COMPLETED");
    final long cleanupRecordsCount = this.sagaRepository.countAllByStatusInAndCreateDateBefore(cleanupStatus, createDateToCompare);
    for (int i = 0; i < cleanupRecordsCount; i += BATCHSIZE) {
        List<UUID> sagaIDsToDelete = this.sagaRepository.findByStatusInAndCreateDateBefore(cleanupStatus, createDateToCompare, BATCHSIZE);
        if (CollectionUtils.isNotEmpty(sagaIDsToDelete)) {
          this.sagaEventRepository.deleteByStatusAndCreateDateBefore(cleanupStatus, createDateToCompare, sagaIDsToDelete);
          this.sagaRepository.deleteByStatusAndCreateDateBefore(cleanupStatus, createDateToCompare, sagaIDsToDelete);
        }
     }
    log.info("Purged old saga and event records from EDUC-STUDENT-DATA-COLLECTION-SAGA-API");
  }

  private LocalDateTime calculateCreateDateBasedOnStaleSagaRecordInDays() {
    return LocalDateTime.now().minusDays(this.getSagaRecordStaleInDays());
  }
}
