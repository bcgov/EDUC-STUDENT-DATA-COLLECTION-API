package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchRecord;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;

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
    ObjectMapper mapper = JsonMapper.builder()
    .findAndAddModules()
    .build();
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

    val saga = this.createMockSaga(entity);
    saga.setSagaId(null);
    this.sagaRepository.save(saga);

    final SdcStudentSagaData sagaData = SdcStudentSagaData.builder()
      .sdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity))
      .school(createMockSchool()).build();
    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.INITIATED)
      .eventOutcome(EventOutcome.INITIATE_SUCCESS)
      .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
    this.sdcStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(EventType.PROCESS_SDC_STUDENT.toString());
    verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.sdcStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(EventType.PROCESS_SDC_STUDENT);
    assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_PROCESSED);
  }

}
