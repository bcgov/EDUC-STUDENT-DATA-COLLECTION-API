package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchRecord;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.PEN_MATCH_PROCESSED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.PEN_MATCH_RESULTS_PROCESSED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.PROCESS_PEN_MATCH;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.PROCESS_PEN_MATCH_RESULTS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypeInitiated_shouldExecuteValidateStudentWithEventOutComeVALIDATION_SUCCESS_NO_ERROR() {
    var collection = collectionRepository.save(createMockCollectionEntity());
    var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,null, null));
    val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    this.sdcSchoolCollectionStudentRepository.save(entity);
    val saga = this.createMockSaga(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity));
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
    assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.VALIDATION_SUCCESS_NO_ERROR);
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypeInitiated_shouldExecuteCalculateAdditionalStudentAttributesADDITIONAL_STUDENT_ATTRIBUTES_CALCULATED() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionRepository
      .save(createMockSdcSchoolCollectionEntity(collection,null, null));
    SdcSchoolCollectionStudentEntity entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity.setIsAdult(null);

    this.sdcSchoolCollectionStudentRepository.save(entity);
    this.bandCodeRepository.save(bandCodeData());
    SdcSagaEntity saga = this.createMockSaga(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity));
    saga.setSagaId(null);
    this.sagaRepository.save(saga);

    final SdcStudentSagaData sagaData = SdcStudentSagaData.builder()
      .sdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity))
      .build();

    Event event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.PROCESS_GRAD_STATUS_RESULT)
      .eventOutcome(EventOutcome.GRAD_STATUS_RESULTS_PROCESSED)
      .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
    this.sdcStudentProcessingOrchestrator.handleEvent(event);

    Optional<SdcSagaEntity> savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
    assertThat(savedSagaInDB.get().getSagaState())
      .isEqualTo(EventType.CALCULATE_ADDITIONAL_STUDENT_ATTRIBUTES.toString());
    verify(this.messagePublisher, atMost(3))
      .dispatchMessage(eq(this.sdcStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final Event newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(EventType.CALCULATE_ADDITIONAL_STUDENT_ATTRIBUTES);
    assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.ADDITIONAL_STUDENT_ATTRIBUTES_CALCULATED);

    List<SdcSchoolCollectionStudentEntity> students = this.sdcSchoolCollectionStudentRepository
      .findAllBySdcSchoolCollection_SdcSchoolCollectionID(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
    assertThat(students).hasAtLeastOneElementOfType(SdcSchoolCollectionStudentEntity.class);

    SdcSchoolCollectionStudentEntity student = students.get(0);
    assertThat(student.getIsAdult()).isFalse();
    assertThat(student.getIsSchoolAged()).isTrue();
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypeInitiated_shouldExecuteFetchGradStatusGRAD_STATUS_FETCHED() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionRepository
      .save(createMockSdcSchoolCollectionEntity(collection,null, null));
    SdcSchoolCollectionStudentEntity entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);

    this.sdcSchoolCollectionStudentRepository.save(entity);
    SdcSagaEntity saga = this.createMockSaga(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity));
    saga.setSagaId(null);
    this.sagaRepository.save(saga);

    final SdcStudentSagaData sagaData = SdcStudentSagaData.builder()
      .sdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity))
      .build();

    Event event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.PROCESS_PEN_MATCH_RESULTS)
      .eventOutcome(EventOutcome.PEN_MATCH_RESULTS_PROCESSED)
      .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
    this.sdcStudentProcessingOrchestrator.handleEvent(event);

    Optional<SdcSagaEntity> savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
    assertThat(savedSagaInDB.get().getSagaState())
      .isEqualTo(EventType.FETCH_GRAD_STATUS.toString());
    verify(this.messagePublisher, atMost(3))
      .dispatchMessage(eq(this.sdcStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final Event newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(EventType.FETCH_GRAD_STATUS);
    assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.GRAD_STATUS_FETCHED);

    List<SdcSchoolCollectionStudentEntity> students = this.sdcSchoolCollectionStudentRepository
      .findAllBySdcSchoolCollection_SdcSchoolCollectionID(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
    assertThat(students).hasAtLeastOneElementOfType(SdcSchoolCollectionStudentEntity.class);

    savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypeInitiated_shouldExecuteProcessGradStatusGRAD_STATUS_PROCESSED() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionRepository
      .save(createMockSdcSchoolCollectionEntity(collection,null, null));
    SdcSchoolCollectionStudentEntity entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);

    this.sdcSchoolCollectionStudentRepository.save(entity);
    SdcSagaEntity saga = this.createMockSaga(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity));
    saga.setSagaId(null);
    this.sagaRepository.save(saga);

    final SdcStudentSagaData sagaData = SdcStudentSagaData.builder()
      .sdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity))
      .gradStatus("false")
      .build();

    Event event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.FETCH_GRAD_STATUS)
      .eventOutcome(EventOutcome.GRAD_STATUS_FETCHED)
      .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
    this.sdcStudentProcessingOrchestrator.handleEvent(event);

    Optional<SdcSagaEntity> savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
    assertThat(savedSagaInDB.get().getSagaState())
      .isEqualTo(EventType.PROCESS_GRAD_STATUS_RESULT.toString());
    verify(this.messagePublisher, atMost(3))
      .dispatchMessage(eq(this.sdcStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final Event newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(EventType.PROCESS_GRAD_STATUS_RESULT);
    assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.GRAD_STATUS_RESULTS_PROCESSED);

    List<SdcSchoolCollectionStudentEntity> students = this.sdcSchoolCollectionStudentRepository
      .findAllBySdcSchoolCollection_SdcSchoolCollectionID(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
    assertThat(students).hasAtLeastOneElementOfType(SdcSchoolCollectionStudentEntity.class);

    SdcSchoolCollectionStudentEntity student = students.get(0);
    assertThat(student.getIsGraduated()).isFalse();
  }
  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypeInitiated_shouldExecuteValidateStudentWithEventOutComeVALIDATION_SUCCESS_WITH_ERROR() {
    var collection = collectionRepository.save(createMockCollectionEntity());
    var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
    val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    entity.setGender("G");
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    this.sdcSchoolCollectionStudentRepository.save(entity);

    val saga = this.createMockSaga(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity));
    saga.setSagaId(null);
    this.sagaRepository.save(saga);

    final SdcStudentSagaData sagaData = createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool());
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
    assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.VALIDATION_SUCCESS_WITH_ERROR);
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypePROCESS_PEN_MATCHAndEventOutComePEN_MATCH_PROCESSEDAA_shouldExecutePROCESS_PEN_MATCH_RESULTS() {
    this.runBasedOnPenStatus("AA", SdcSchoolStudentStatus.VERIFIED.toString(), "120164447");
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypePROCESS_PEN_MATCHAndEventOutComePEN_MATCH_PROCESSEDB1_shouldExecutePROCESS_PEN_MATCH_RESULTS() {
    this.runBasedOnPenStatus("B1", SdcSchoolStudentStatus.VERIFIED.toString(), "120164447");
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypePROCESS_PEN_MATCHAndEventOutComePEN_MATCH_PROCESSEDC1_shouldExecutePROCESS_PEN_MATCH_RESULTS() {
    this.runBasedOnPenStatus("C1", SdcSchoolStudentStatus.VERIFIED.toString(), "120164447");
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypePROCESS_PEN_MATCHAndEventOutComePEN_MATCH_PROCESSEDD1_shouldExecutePROCESS_PEN_MATCH_RESULTS() {
    this.runBasedOnPenStatus("D1", SdcSchoolStudentStatus.VERIFIED.toString(), "120164447");
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypePROCESS_PEN_MATCHAndEventOutComePEN_MATCH_PROCESSEDOther_shouldExecutePROCESS_PEN_MATCH_RESULTS() {
    this.runBasedOnPenStatus("F1", SdcSchoolStudentStatus.FIXABLE.toString(), "120164447");
  }

  private void runBasedOnPenStatus(final String penStatus, final String status, final String pen) throws InterruptedException, IOException, TimeoutException {
    var collection = collectionRepository.save(createMockCollectionEntity());
    var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    SdcSchoolCollectionStudent sdcStudent = SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity);
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    entity = this.sdcSchoolCollectionStudentRepository.save(entity);
    sdcStudent.setSdcSchoolCollectionStudentID(entity.getSdcSchoolCollectionStudentID().toString());

    val saga = this.createMockSaga(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity));
    saga.setSagaId(null);
    saga.setStatus(IN_PROGRESS.toString());
    saga.setSdcSchoolCollectionStudentID(UUID.fromString(sdcStudent.getSdcSchoolCollectionStudentID()));
    this.sagaRepository.save(saga);
    final List<PenMatchRecord> matchRecords = new ArrayList<>();
    final PenMatchRecord record = new PenMatchRecord();
    record.setMatchingPEN(pen);
    record.setStudentID(UUID.randomUUID().toString());
    matchRecords.add(record);
    final var eventPayload = new PenMatchResult();
  eventPayload.setPenStatus(penStatus);
  eventPayload.setMatchingRecords(matchRecords);
    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(PROCESS_PEN_MATCH)
      .eventOutcome(PEN_MATCH_PROCESSED)
      .eventPayload(JsonUtil.getJsonStringFromObject(eventPayload))
      .build();
    this.sdcStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(PROCESS_PEN_MATCH_RESULTS.toString());
    val savedNominalRollStudent = this.sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
    assertThat(savedNominalRollStudent).isPresent();
    assertThat(savedNominalRollStudent.get().getStudentPen()).isEqualTo(pen);
    assertThat(savedNominalRollStudent.get().getSdcSchoolCollectionStudentStatusCode()).isEqualTo(status);
    verify(this.messagePublisher, atMost(1)).dispatchMessage(eq(this.sdcStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(PROCESS_PEN_MATCH_RESULTS);
    assertThat(newEvent.getEventOutcome()).isEqualTo(PEN_MATCH_RESULTS_PROCESSED);
    assertThat(newEvent.getEventPayload()).isEqualTo(penStatus);
  }

}
