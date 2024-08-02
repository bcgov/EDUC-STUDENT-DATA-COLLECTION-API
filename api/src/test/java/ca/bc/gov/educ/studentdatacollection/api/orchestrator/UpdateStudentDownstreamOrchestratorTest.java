package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.UpdateStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
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

import java.io.File;
import java.io.IOException;
import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class UpdateStudentDownstreamOrchestratorTest extends BaseStudentDataCollectionAPITest {

    @MockBean
    RestUtils restUtils;
    @Autowired
    CollectionRepository collectionRepository;
    @Autowired
    CollectionTypeCodeRepository collectionTypeCodeRepository;
    @Autowired
    UpdateStudentDownstreamOrchestrator updateStudentDownstreamOrchestrator;
    @Autowired
    SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    @Autowired
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    @Autowired
    CollectionCodeCriteriaRepository collectionCodeCriteriaRepository;
    @Autowired
    SagaRepository sagaRepository;
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
    void testHandleEvent_givenEventTypeUPDATE_STUDENT_shouldPostEventToSTUDENT_API_TOPIC() {
        final var studentPayload = Student.builder().studentID(UUID.randomUUID().toString()).pen("123456789").legalFirstName("Test").build();
        when(this.restUtils.getStudentByPEN(any(), any())).thenReturn(studentPayload);

        var student = setMockDataForSaga();
        UpdateStudentSagaData sagaData = createSagaData(student);
        val saga = this.createMockUpdateStudentDownstreamSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        val event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(INITIATED)
                .eventOutcome(INITIATE_SUCCESS)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
        this.updateStudentDownstreamOrchestrator.handleEvent(event);

        verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(UPDATE_STUDENT);

        final var eventPayload = JsonUtil.getJsonObjectFromString(Student.class, newEvent.getEventPayload());
        assertThat(eventPayload.getLocalID()).isEqualTo("B22222222");

        val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
        assertThat(savedSagaInDB).isPresent();
        assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(UPDATE_STUDENT.toString());
    }

    @SneakyThrows
    @Test
    void testHandleEvent_givenEventTypeUPDATE_SDC_STUDENT_STATUS_shouldExecuteUpdateSdcStudentStatus() {
        var student = setMockDataForSaga();
        UpdateStudentSagaData sagaData = createSagaData(student);
        val saga = this.createMockUpdateStudentDownstreamSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        val event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(UPDATE_STUDENT)
                .eventOutcome(STUDENT_UPDATED)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
        this.updateStudentDownstreamOrchestrator.handleEvent(event);

        verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(UPDATE_STUDENT_DOWNSTREAM_SAGA_TOPIC.toString()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(UPDATE_SDC_STUDENT_STATUS);

        val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
        assertThat(savedSagaInDB).isPresent();
        assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(UPDATE_SDC_STUDENT_STATUS.toString());

        val updatedStudent = sdcSchoolCollectionStudentRepository.findById(UUID.fromString(sagaData.getSdcSchoolCollectionStudentID()));
        assertThat(updatedStudent).isPresent();
        assertThat(updatedStudent.get().getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.COMPLETED.toString());
    }

    public UpdateStudentSagaData createSagaData(SdcSchoolCollectionStudentEntity entity) {
      return  UpdateStudentSagaData.builder()
              .dob(entity.getDob())
              .assignedPEN(entity.getAssignedPen())
              .localID(entity.getLocalID())
              .genderCode(entity.getGender())
              .gradeCode(entity.getEnrolledGradeCode())
              .mincode("0000001")
              .postalCode(entity.getPostalCode())
              .sdcSchoolCollectionStudentID(String.valueOf(entity.getSdcSchoolCollectionStudentID()))
              .sexCode(entity.getGender())
              .usualFirstName(entity.getUsualFirstName())
              .usualLastName(entity.getUsualLastName())
              .usualMiddleNames(entity.getUsualMiddleNames()).build();
    }

    public SdcSchoolCollectionStudentEntity setMockDataForSaga() throws IOException {
        var typeCode = this.collectionTypeCodeRepository.save(this.createMockCollectionCodeEntityForFeb());
        this.collectionCodeCriteriaRepository.save(this.createMockCollectionCodeCriteriaEntity(typeCode));
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school = createMockSchool();
        school.setDisplayName("School1");
        school.setMincode("0000001");
        school.setDistrictId(districtID.toString());
        when(this.restUtils.getSchoolBySchoolID(school.getSchoolId())).thenReturn(Optional.of(school));
        when(this.restUtils.getSchoolListGivenCriteria(anyList(), any())).thenReturn(List.of(school));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var savedSchoolColl = sdcSchoolCollectionRepository.save(firstSchool);

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = models.stream().peek(model -> model.setSdcSchoolCollection(firstSchool)).toList();

        var savedStudent = sdcSchoolCollectionStudentRepository.saveAll(students);
        return savedStudent.get(0);
    }

}
