package ca.bc.gov.educ.studentdatacollection.api.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

  public PurgeOldSagaRecordsScheduler(final SagaRepository sagaRepository, final SagaEventRepository sagaEventRepository) {
    this.sagaRepository = sagaRepository;
    this.sagaEventRepository = sagaEventRepository;
  }


  /**
   * run the job based on configured scheduler(a cron expression) and purge old records from DB.
   */
  @Scheduled(cron = "${scheduled.jobs.purge.old.saga.records.cron}")
  @SchedulerLock(name = "PurgeOldSagaRecordsLock", lockAtLeastFor = "55s", lockAtMostFor = "57s")
  @Transactional
  public void pollSagaTableAndPurgeOldRecords() {
    LockAssert.assertLocked();
    final LocalDateTime createDateToCompare = this.calculateCreateDateBasedOnStaleSagaRecordInDays();
    this.sagaRepository.deleteByCreateDateBefore(createDateToCompare);
    log.info("Purged old saga and event records from EDUC-STUDENT-DATA-COLLECTION-SAGA-API");
  }

  private LocalDateTime calculateCreateDateBasedOnStaleSagaRecordInDays() {
    return LocalDateTime.now().minusDays(this.getSagaRecordStaleInDays());
  }
}
