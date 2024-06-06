package ca.bc.gov.educ.studentdatacollection.api.schedulers;


import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import java.util.UUID;

class PurgeOldSagaRecordsSchedulerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  SagaRepository repository;

  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  PurgeOldSagaRecordsScheduler purgeOldSagaRecordsScheduler;

  @Test
  void pollSagaTableAndPurgeOldRecords_givenOldRecordsPresent_shouldBeDeleted() {
    final var payload = "{\"createUser\": \"ADMIN\", \"updateUser\": \"ADMIN\"}";
    final var saga = this.getSaga(payload);
    this.repository.save(saga);
    this.sagaEventRepository.save(this.getSagaEvent(saga,payload));
    this.purgeOldSagaRecordsScheduler.setSagaRecordStaleInDays(0);
    this.purgeOldSagaRecordsScheduler.pollSagaTableAndPurgeOldRecords();
    final var sagas = this.repository.findAll();
    assertThat(sagas).isEmpty();
  }


  private SdcSagaEntity getSaga(final String payload) {
    return SdcSagaEntity
        .builder()
        .payload(payload)
        .sagaName("PEN_REQUEST_RETURN_SAGA")
        .status(SagaStatusEnum.STARTED.toString())
        .sagaState(EventType.INITIATED.toString())
        .createDate(LocalDateTime.now())
        .createUser("SDC_API")
        .updateUser("SDC_API")
        .updateDate(LocalDateTime.now())
        .build();
  }
  private SagaEventStatesEntity getSagaEvent(final SdcSagaEntity saga, final String payload) {
    return SagaEventStatesEntity
        .builder()
        .sagaEventResponse(payload)
        .saga(saga)
        .sagaEventState("NOTIFY_STUDENT_PEN_REQUEST_RETURN")
        .sagaStepNumber(4)
        .sagaEventOutcome("STUDENT_NOTIFIED")
        .createDate(LocalDateTime.now())
        .createUser("SDC_API")
        .updateUser("SDC_API")
        .updateDate(LocalDateTime.now())
        .build();
  }
}
