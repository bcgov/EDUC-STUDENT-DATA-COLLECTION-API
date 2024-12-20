package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.PROCESS_SDC_STUDENT;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
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
  SdcSchoolCollectionStudentEnrolledProgramRepository sdcSchoolCollectionStudentEnrolledProgramRepository;
  @Autowired
  SagaRepository sagaRepository;
  @Autowired
  BandCodeRepository bandCodeRepository;
  @Autowired
  MessagePublisher messagePublisher;
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  @BeforeEach
  public void setUp() {
    Mockito.reset(this.messagePublisher);
    Mockito.reset(this.restUtils);
    JsonMapper.builder().findAndAddModules().build();
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypeInitiated_shouldExecuteValidateStudentWithEventOutComeVALIDATION_SUCCESS_WITH_ERROR() {
    var collection = collectionRepository.save(createMockCollectionEntity());
    var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
    val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    entity.setGender("G");
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    this.sdcSchoolCollectionStudentRepository.save(entity);

    val saga = this.createMockSaga(entity);
    saga.setSagaId(null);
    this.sagaRepository.save(saga);

    final SdcStudentSagaData sagaData = SdcStudentSagaData.builder()
      .sdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity))
      .school(createMockSchool()).build();
    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.PROCESS_SDC_STUDENT)
      .eventOutcome(EventOutcome.STUDENT_PROCESSED)
      .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
    this.sdcStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(COMPLETED.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.sdcStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(EventType.MARK_SAGA_COMPLETE);
    assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.SAGA_COMPLETED);
  }

  @SneakyThrows
  @Test
  void testStudentOrchestrator_givenEventAndSagaData_shouldProcessStudent() {
    var collection = collectionRepository.save(createMockCollectionEntity());
    var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
    val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    entity.setGender("G");
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    this.sdcSchoolCollectionStudentRepository.save(entity);

    val saga = this.createMockSaga(entity);
    this.sagaRepository.save(saga);

    when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());

    final SdcStudentSagaData sagaData = SdcStudentSagaData.builder()
            .sdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity))
            .school(createMockSchool())
            .collectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode())
            .build();
    val event = Event.builder()
            .sagaId(saga.getSagaId())
            .eventType(EventType.PROCESS_SDC_STUDENT)
            .eventOutcome(EventOutcome.STUDENT_PROCESSED)
            .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
    this.sdcStudentProcessingOrchestrator.processStudentRecord(event, saga, sagaData);

    verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.sdcStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(PROCESS_SDC_STUDENT);
  }

}
