package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SdcStudentProcessingOrchestratorTest extends BaseStudentDataCollectionAPITest {

  @MockBean
  protected RestUtils restUtils;
  @Autowired
  SdcStudentProcessingOrchestrator sdcStudentProcessingOrchestrator;
  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  @Autowired
  SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  @Autowired
  SagaRepository sagaRepository;
  @Autowired
  MessagePublisher messagePublisher;
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  @BeforeEach
  public void setUp() throws Exception {
    Mockito.reset(this.messagePublisher);
    Mockito.reset(this.restUtils);
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypeInitiated_shouldExecuteValidateStudentWithEventOutComeVALIDATION_SUCCESS_NO_ERROR() {
    var collection = collectionRepository.save(createMockCollectionEntity());
    var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection));
    val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    this.sdcSchoolCollectionStudentRepository.save(entity);
    val saga = this.creatMockSaga(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity));
    saga.setSagaId(null);
    this.sagaRepository.save(saga);
    final SdcStudentSagaData sagaData = SdcStudentSagaData.builder().sdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)).build();
    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.INITIATED)
      .eventOutcome(EventOutcome.INITIATE_SUCCESS)
      .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
    this.sdcStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(EventType.VALIDATE_SDC_STUDENT.toString());
    verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.sdcStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(EventType.VALIDATE_SDC_STUDENT);
    assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.VALIDATION_SUCCESS_NO_ERROR_WARNING);
  }

}
