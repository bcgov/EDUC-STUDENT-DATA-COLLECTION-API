package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.UpdateStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.INITIATE_SUCCESS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.STUDENT_UPDATED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.STUDENT_API_TOPIC;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.UPDATE_STUDENT_DOWNSTREAM_SAGA_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
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
    @Autowired
    SdcStudentEllRepository sdcStudentEllRepository;
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
        var school = createMockSchool();
        school.setDisplayName("School1");
        school.setMincode("0000001");
        when(this.restUtils.getSchoolByMincode(any())).thenReturn(Optional.of(school));

        var student = setMockDataForSaga();
        final var studentPayload = Student.builder().studentID(student.getAssignedStudentId().toString()).pen(student.getAssignedPen()).legalFirstName(student.getLegalLastName()).build();
        when(this.restUtils.getStudentByPEN(any(), any())).thenReturn(studentPayload);

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
    void testHandleEvent_givenEventTypeUPDATE_SDC_STUDENT_STATUS_WithNoEllAsEnrolledProgram_shouldExecuteUpdateSdcStudentStatus() {
        this.collectionTypeCodeRepository.save(this.createMockCollectionCodeEntity());

        var school = createMockSchool();
        school.setDisplayName("School1");
        school.setMincode("0000001");
        when(this.restUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(school));

        var student = setMockDataForSaga();
        UpdateStudentSagaData sagaData = createSagaData(student);
        sagaData.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        val saga = this.createMockUpdateStudentDownstreamSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        var existingEll = sdcStudentEllRepository.findByStudentID(student.getAssignedStudentId());
        assertThat(existingEll).isNotPresent();

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
        assertThat(updatedStudent.get().getYearsInEll()).isNull();
        assertThat(updatedStudent.get().getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.COMPLETED.toString());

        var updatedEll = sdcStudentEllRepository.findByStudentID(student.getAssignedStudentId());
        assertThat(updatedEll).isNotPresent();
    }

    @SneakyThrows
    @Test
    void testHandleEvent_givenEventTypeUPDATE_SDC_STUDENT_STATUS_WithEllAsEnrolledProgram_shouldExecuteUpdateSdcStudentStatus() {
        this.collectionTypeCodeRepository.save(this.createMockCollectionCodeEntity());

        var school = createMockSchool();
        school.setDisplayName("School1");
        school.setMincode("0000001");
        when(this.restUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(school));

        var student = setMockDataForSaga();
        student.setYearsInEll(0);
        student.setEnrolledProgramCodes("17");
        sdcSchoolCollectionStudentRepository.save(student);

        UpdateStudentSagaData sagaData = createSagaData(student);
        sagaData.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        val saga = this.createMockUpdateStudentDownstreamSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        var existingEll = sdcStudentEllRepository.findByStudentID(student.getAssignedStudentId());
        assertThat(existingEll).isNotPresent();

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
        assertThat(updatedStudent.get().getYearsInEll()).isEqualTo(1);
        assertThat(updatedStudent.get().getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.COMPLETED.toString());

        var updatedEll = sdcStudentEllRepository.findByStudentID(student.getAssignedStudentId());
        assertThat(updatedEll).isPresent();
        assertThat(updatedEll.get().getStudentID()).isEqualTo(updatedStudent.get().getAssignedStudentId());
        assertThat(updatedEll.get().getYearsInEll()).isEqualTo(1);
    }

    public UpdateStudentSagaData createSagaData(SdcSchoolCollectionStudentEntity entity) {
      return  UpdateStudentSagaData.builder()
              .dob(entity.getDob())
              .assignedPEN(entity.getAssignedPen())
              .assignedStudentID(entity.getAssignedStudentId().toString())
              .collectionID(entity.getSdcSchoolCollection().getCollectionEntity().getCollectionID().toString())
              .localID(entity.getLocalID())
              .genderCode(entity.getGender())
              .gradeCode(entity.getEnrolledGradeCode())
              .mincode("0000001")
              .postalCode(entity.getPostalCode())
              .sdcSchoolCollectionStudentID(String.valueOf(entity.getSdcSchoolCollectionStudentID()))
              .sexCode(entity.getGender())
              .usualFirstName(entity.getUsualFirstName())
              .usualLastName(entity.getUsualLastName())
              .collectionTypeCode(entity.getSdcSchoolCollection().getCollectionEntity().getCollectionTypeCode())
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
        when(this.restUtils.getSchoolByMincode(school.getSchoolId())).thenReturn(Optional.of(school));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(firstSchool);

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = models.stream().peek(model -> model.setSdcSchoolCollection(firstSchool)).toList();

        var savedStudent = sdcSchoolCollectionStudentRepository.saveAll(students);
        var firstStuToUpdate = sdcSchoolCollectionStudentRepository.findById(savedStudent.get(0).getSdcSchoolCollectionStudentID());
        firstStuToUpdate.get().setAssignedStudentId(UUID.randomUUID());
        firstStuToUpdate.get().setAssignedPen("123456789");
        return sdcSchoolCollectionStudentRepository.save(firstStuToUpdate.get());
    }

    @Test
    void isStudentAttendingSchoolOfRecord_shouldReturnTrue_whenCurrentStudentHasMostCourses() {
        UpdateStudentSagaData currStudent = createUpdateStudentSagaDataWithCourses("5.0");
        currStudent.setMincode("1234567");
        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity1 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(2.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");

        SdcSchoolCollectionStudentEntity studentEntity2 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity2.setNumberOfCourses(String.valueOf(2.0));
        studentEntity2.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity2.setAssignedPen("123456789");

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity1, studentEntity2);
        SchoolTombstone currentSchool = createSchoolTombstoneWithCategoryAndFacilityCode(SchoolCategoryCodes.PUBLIC.getCode(), "STANDARD");
        when(restUtils.getSchoolByMincode(currStudent.getMincode())).thenReturn(Optional.of(currentSchool));

        boolean result = updateStudentDownstreamOrchestrator.isStudentAttendingSchoolOfRecord(currStudent, otherStudents);
        assertTrue(result);
    }

    @Test
    void isStudentAttendingSchoolOfRecord_shouldReturnFalse_whenAnotherStudentHasMostCourses() {
        SchoolTombstone currentSchool = createSchoolTombstoneWithCategoryAndFacilityCode(SchoolCategoryCodes.PUBLIC.getCode(), "STANDARD");
        UpdateStudentSagaData currStudent = createUpdateStudentSagaDataWithCourses("2.0");
        currStudent.setMincode("1234567");
        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity1 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(3.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");

        SdcSchoolCollectionStudentEntity studentEntity2 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(4.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity1, studentEntity2);
        when(restUtils.getSchoolByMincode(currStudent.getMincode())).thenReturn(Optional.of(currentSchool));
        boolean result = updateStudentDownstreamOrchestrator.isStudentAttendingSchoolOfRecord(currStudent, otherStudents);
        assertFalse(result);
    }

    @Test
    void isStudentAttendingSchoolOfRecord_shouldReturnTrue_whenCourseNumbersAreEqual_andCurrentStudentHasPublicSchool() {
        UpdateStudentSagaData currStudent = createUpdateStudentSagaDataWithCourses("3.0");
        currStudent.setMincode("1234567");

        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity1 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(3.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity1);

        SchoolTombstone currentSchool = createSchoolTombstoneWithCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        when(restUtils.getSchoolByMincode(currStudent.getMincode())).thenReturn(Optional.of(currentSchool));

        SchoolTombstone otherSchool = createSchoolTombstoneWithCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(otherSchool));

        boolean result = updateStudentDownstreamOrchestrator.isStudentAttendingSchoolOfRecord(currStudent, otherStudents);
        assertTrue(result);
    }

    @Test
    void isStudentAttendingSchoolOfRecord_shouldReturnFalse_whenCourseNumbersAreEqual_andAnotherStudentHasPublicSchool() {
        UpdateStudentSagaData currStudent = createUpdateStudentSagaDataWithCourses("3.0");
        currStudent.setMincode("1234567");

        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity1 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(3.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity1);

        SchoolTombstone currentSchool = createSchoolTombstoneWithCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
        when(restUtils.getSchoolByMincode(currStudent.getMincode())).thenReturn(Optional.of(currentSchool));

        SchoolTombstone otherSchool = createSchoolTombstoneWithCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(otherSchool));

        boolean result = updateStudentDownstreamOrchestrator.isStudentAttendingSchoolOfRecord(currStudent, otherStudents);
        assertFalse(result);
    }

    @Test
    void getMaxCourseNumber_shouldReturnZero_whenListIsEmpty() {
        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of();
        Float result = updateStudentDownstreamOrchestrator.getMaxCourseNumber(otherStudents);
        assertEquals(0f, result);
    }

    @Test
    void getMaxCourseNumber_shouldReturnMax_whenListHasValues() {
        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity1 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(2.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");

        SdcSchoolCollectionStudentEntity studentEntity2 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity2.setNumberOfCourses(String.valueOf(4.0));
        studentEntity2.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity2.setAssignedPen("123456789");

        SdcSchoolCollectionStudentEntity studentEntity3 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity3.setNumberOfCourses(String.valueOf(3.0));
        studentEntity3.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity3.setAssignedPen("123456789");

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity1, studentEntity2, studentEntity3);

        Float result = updateStudentDownstreamOrchestrator.getMaxCourseNumber(otherStudents);
        assertEquals(4.0f, result);
    }

    @Test
    void retrieveSchoolTombstones_shouldReturnTombstones() {
        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity.setNumberOfCourses(String.valueOf(3.0));
        studentEntity.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity.setAssignedPen("123456789");

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity);
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(createMockSchoolTombstone()));
        List<Optional<SchoolTombstone>> result = updateStudentDownstreamOrchestrator.retrieveSchoolTombstones(3.0f, otherStudents);
        assertEquals(1, result.size());
    }

    @Test
    void extractRelevantMincode_shouldReturnSchoolMincode_whenSchoolCategoryIsIndependent() {
        SchoolTombstone school = createSchoolTombstoneWithCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
        school.setMincode("1234567");
        Integer result = updateStudentDownstreamOrchestrator.extractRelevantMincode(school);
        assertEquals(1234567, result);
    }

    @Test
    void extractRelevantMincode_shouldReturnDistrictNumber_whenSchoolCategoryIsNotIndependent() {
        SchoolTombstone school = createSchoolTombstoneWithCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        school.setDistrictId("DistrictID");
        District district = createMockDistrict();
        district.setDistrictNumber("987");
        when(restUtils.getDistrictByDistrictID(school.getDistrictId())).thenReturn(Optional.of(district));
        Integer result = updateStudentDownstreamOrchestrator.extractRelevantMincode(school);
        assertEquals(987, result);
    }

    @Test
    void isStudentAttendingSchoolOfRecord_shouldReturnFalse_whenCurrentStudentHasNullCourses_andAnotherStudentHasCourses() {
        SchoolTombstone currentSchool = createSchoolTombstoneWithCategoryAndFacilityCode(SchoolCategoryCodes.PUBLIC.getCode(), "STANDARD");
        UpdateStudentSagaData currStudent = createUpdateStudentSagaDataWithCourses(null);
        currStudent.setMincode("1234567");
        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity1 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(3.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");
        when(restUtils.getSchoolByMincode(currStudent.getMincode())).thenReturn(Optional.of(currentSchool));

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity1);

        boolean result = updateStudentDownstreamOrchestrator.isStudentAttendingSchoolOfRecord(currStudent, otherStudents);
        assertFalse(result);
    }

    @Test
    void isStudentAttendingSchoolOfRecord_shouldReturnTrue_whenCourseNumbersAreEqual_andBothSchoolsArePublic_andCurrentStudentSchoolIsStandard() {
        UpdateStudentSagaData currStudent = createUpdateStudentSagaDataWithCourses("3.0");
        currStudent.setMincode("1234567");

        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity1 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(3.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity1);

        SchoolTombstone currentSchool = createSchoolTombstoneWithCategoryAndFacilityCode(SchoolCategoryCodes.PUBLIC.getCode(), "STANDARD");
        when(restUtils.getSchoolByMincode(currStudent.getMincode())).thenReturn(Optional.of(currentSchool));

        SchoolTombstone otherSchool = createSchoolTombstoneWithCategoryAndFacilityCode(SchoolCategoryCodes.PUBLIC.getCode(), "NOT_STANDARD");
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(otherSchool));

        boolean result = updateStudentDownstreamOrchestrator.isStudentAttendingSchoolOfRecord(currStudent, otherStudents);
        assertTrue(result);
    }

    @Test
    void isStudentAttendingSchoolOfRecord_shouldReturnFalse_whenCourseNumbersAreEqual_andBothSchoolsArePublic_andOtherStudentSchoolIsStandard() {
        UpdateStudentSagaData currStudent = createUpdateStudentSagaDataWithCourses("3.0");
        currStudent.setMincode("1234567");

        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity1 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(3.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity1);

        SchoolTombstone currentSchool = createSchoolTombstoneWithCategoryAndFacilityCode(SchoolCategoryCodes.PUBLIC.getCode(), "NOT_STANDARD");
        when(restUtils.getSchoolByMincode(currStudent.getMincode())).thenReturn(Optional.of(currentSchool));

        SchoolTombstone otherSchool = createSchoolTombstoneWithCategoryAndFacilityCode(SchoolCategoryCodes.PUBLIC.getCode(), "STANDARD");
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(otherSchool));

        District currentDistrict = createMockDistrict();
        currentDistrict.setDistrictNumber("987");
        when(restUtils.getDistrictByDistrictID(currentSchool.getDistrictId())).thenReturn(Optional.of(currentDistrict));

        District otherDistrict = createMockDistrict();
        otherDistrict.setDistrictNumber("654");
        when(restUtils.getDistrictByDistrictID(otherSchool.getDistrictId())).thenReturn(Optional.of(otherDistrict));


        boolean result = updateStudentDownstreamOrchestrator.isStudentAttendingSchoolOfRecord(currStudent, otherStudents);
        assertFalse(result);
    }

    @Test
    void isStudentAttendingSchoolOfRecord_shouldReturnFalse_whenCourseNumbersAreEqual_andBothSchoolsArePublic_andBothSchoolsAreStandard_andOtherSchoolHasLowerMincode() {
        UpdateStudentSagaData currStudent = createUpdateStudentSagaDataWithCourses("3.0");
        currStudent.setMincode("1234567");

        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity1 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(3.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity1);

        SchoolTombstone currentSchool = createSchoolTombstoneWithCategoryAndFacilityCode(SchoolCategoryCodes.PUBLIC.getCode(), "STANDARD");
        currentSchool.setDistrictId("DistrictID");
        District currentDistrict = createMockDistrict();
        currentDistrict.setDistrictNumber("987");
        when(restUtils.getSchoolByMincode(currStudent.getMincode())).thenReturn(Optional.of(currentSchool));
        when(restUtils.getDistrictByDistrictID(currentSchool.getDistrictId())).thenReturn(Optional.of(currentDistrict));


        SchoolTombstone otherSchool = createSchoolTombstoneWithCategoryAndFacilityCode(SchoolCategoryCodes.PUBLIC.getCode(), "STANDARD");
        otherSchool.setDistrictId("DistrictID2");
        District otherDistrict = createMockDistrict();
        otherDistrict.setDistrictNumber("654");
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(otherSchool));
        when(restUtils.getDistrictByDistrictID(otherSchool.getDistrictId())).thenReturn(Optional.of(otherDistrict));

        boolean result = updateStudentDownstreamOrchestrator.isStudentAttendingSchoolOfRecord(currStudent, otherStudents);
        assertFalse(result);
    }

    @Test
    void isStudentAttendingSchoolOfRecord_shouldThrowError_whenCurrentStudentSchoolTombstoneIsMissing() {
        UpdateStudentSagaData currStudent = createUpdateStudentSagaDataWithCourses("3.0");
        currStudent.setMincode("1234567");

        CollectionEntity collectionEntity = createMockCollectionEntity();
        SdcSchoolCollectionEntity schoolCollectionEntity = createMockSdcSchoolCollectionEntity(collectionEntity, UUID.randomUUID());

        SdcSchoolCollectionStudentEntity studentEntity1 = createMockSchoolStudentEntity(schoolCollectionEntity);
        studentEntity1.setNumberOfCourses(String.valueOf(3.0));
        studentEntity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        studentEntity1.setAssignedPen("123456789");

        List<SdcSchoolCollectionStudentEntity> otherStudents = List.of(studentEntity1);

        when(restUtils.getSchoolByMincode(currStudent.getMincode())).thenReturn(Optional.empty());
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(createMockSchoolTombstone()));

        assertThrows(EntityNotFoundException.class, () -> {
            updateStudentDownstreamOrchestrator.isStudentAttendingSchoolOfRecord(currStudent, otherStudents);
        });
    }

    private SchoolTombstone createSchoolTombstoneWithCategoryAndFacilityCode(String categoryCode, String facilityCode) {
        SchoolTombstone tombstone = createMockSchoolTombstone();
        tombstone.setSchoolCategoryCode(categoryCode);
        tombstone.setFacilityTypeCode(facilityCode);
        return tombstone;
    }

    private UpdateStudentSagaData createUpdateStudentSagaDataWithCourses(String numberOfCourses) {
        UpdateStudentSagaData sagaData = new UpdateStudentSagaData();
        sagaData.setNumberOfCourses(numberOfCourses);
        return sagaData;
    }

    private SchoolTombstone createSchoolTombstoneWithCategoryCode(String categoryCode) {
        SchoolTombstone tombstone = createMockSchoolTombstone();
        tombstone.setSchoolCategoryCode(categoryCode);
        return tombstone;
    }

}
