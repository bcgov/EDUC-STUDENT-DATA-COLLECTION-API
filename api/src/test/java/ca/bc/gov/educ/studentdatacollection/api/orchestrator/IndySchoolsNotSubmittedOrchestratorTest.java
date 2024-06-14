package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.email.IndySchoolNotSubmittedEmailOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.EmailSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;

import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class IndySchoolsNotSubmittedOrchestratorTest extends BaseStudentDataCollectionAPITest {

  @MockBean
  protected RestUtils restUtils;
  @Autowired
  IndySchoolNotSubmittedEmailOrchestrator indySchoolNotSubmittedEmailOrchestrator;
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
    JsonMapper.builder()
    .findAndAddModules()
    .build();
  }

  @SneakyThrows
  @Test
  void testHandleEvent_givenEventTypeInitiated_shouldExecuteValidateStudentWithEventOutComeVALIDATION_SUCCESS_WITH_ERROR() {
    var collection = collectionRepository.save(createMockCollectionEntity());
    sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
    var emailFields = new HashMap<String, String>();
    emailFields.put("submissionDueDate", LocalDate.now().toString());
    emailFields.put("dataCollectionMonth", "september");
    final EmailSagaData sagaData = EmailSagaData
            .builder()
            .fromEmail("abc@acb.com")
            .toEmails(Arrays.asList("someone@somewhere.com"))
            .subject("Test")
            .templateName("collection.independent.school.no.activity.notification")
            .emailFields(emailFields)
            .build();

    val saga = this.createMockIndyNotSubmittedEmailSaga(sagaData);
    saga.setSagaId(null);
    this.sagaRepository.save(saga);

    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.SEND_EMAIL)
      .eventOutcome(EventOutcome.EMAIL_SENT)
      .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
    this.indySchoolNotSubmittedEmailOrchestrator.handleEvent(event);
    val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(COMPLETED.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.indySchoolNotSubmittedEmailOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(EventType.MARK_SAGA_COMPLETE);
    assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.SAGA_COMPLETED);
  }

}
