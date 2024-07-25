package ca.bc.gov.educ.studentdatacollection.api.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.service.v1.events.schedulers.EventTaskSchedulerAsyncService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Event task scheduler.
 */
@Component
@Slf4j
public class EventTaskScheduler {
  /**
   * The Task scheduler async service.
   */
  @Getter(PRIVATE)
  private final EventTaskSchedulerAsyncService taskSchedulerAsyncService;

  /**
   * Instantiates a new Event task scheduler.
   *
   * @param taskSchedulerAsyncService the task scheduler async service
   */
  @Autowired
  public EventTaskScheduler(final EventTaskSchedulerAsyncService taskSchedulerAsyncService) {
    this.taskSchedulerAsyncService = taskSchedulerAsyncService;
  }


  @Scheduled(cron = "${scheduled.jobs.extract.uncompleted.sagas.cron}") // 1 * * * * *
  @SchedulerLock(name = "EXTRACT_UNCOMPLETED_SAGAS",
    lockAtLeastFor = "${scheduled.jobs.extract.uncompleted.sagas.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.extract.uncompleted.sagas.cron.lockAtMostFor}")
  public void findAndProcessPendingSagaEvents() {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().findAndProcessUncompletedSagas();
  }


  @Scheduled(cron = "${scheduled.jobs.process.loaded.sdc.students.cron}")
  @SchedulerLock(name = "PROCESS_LOADED_STUDENTS", lockAtLeastFor = "${scheduled.jobs.process.loaded.sdc.students.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.process.loaded.sdc.students.cron.lockAtMostFor}")
  public void processLoadedStudents() {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().findAndPublishLoadedStudentRecordsForProcessing();
  }

  @Scheduled(cron = "${scheduled.jobs.process.school.collection.for.submission.cron}")
  @SchedulerLock(name = "SUBMIT_SCHOOL_COLLECTION", lockAtLeastFor = "${scheduled.jobs.process.school.collection.for.submission.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.process.school.collection.for.submission.cron.lockAtMostFor}")
  public void submitSchoolCollections() {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().findSchoolCollectionsForSubmission();
  }

  @Scheduled(cron = "${scheduled.jobs.process.tardy.collection.schools.cron}")
  @SchedulerLock(name = "NOTIFY_TARDY_SCHOOLS", lockAtLeastFor = "${scheduled.jobs.process.tardy.collection.schools.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.process.tardy.collection.schools.cron.lockAtMostFor}")
  public void notifyIndySchoolsToSubmit() {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().findAllUnsubmittedIndependentSchoolsInCurrentCollection();
  }

  @Scheduled(cron = "${scheduled.jobs.process.update.student.downstream.cron}")
  @SchedulerLock(name = "UPDATE_STUDENT_DEMOG", lockAtLeastFor = "${scheduled.jobs.process.update.student.downstream.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.process.update.student.downstream.cron.lockAtMostFor}")
  public void updateStudentDemogDownstream() {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().updateStudentDemogDownstream();
  }
}
