package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SchoolListService;
import ca.bc.gov.educ.studentdatacollection.api.struct.CollectionSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
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
import java.time.LocalDate;
import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.FUNDING_GROUP_SNAPSHOT_SAVED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.NEW_COLLECTION_CREATED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.COMPLETED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class CloseCollectionOrchestratorTest extends BaseStudentDataCollectionAPITest {

    @MockBean
    protected RestUtils restUtils;
    @MockBean
    protected SchoolListService schoolListService;
    @Autowired
    CollectionRepository collectionRepository;
    @Autowired
    CollectionTypeCodeRepository collectionTypeCodeRepository;
    @Autowired
    CloseCollectionOrchestrator closeCollectionOrchestrator;
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
    IndependentSchoolFundingGroupSnapshotRepository independentSchoolFundingGroupSnapshotRepository;
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
    void testHandleEvent_givenEventTypeInitiated_shouldExecuteCloseCurrentCollAndOpenNewCollWithEventOutComeNEW_COLLECTION_CREATED() {
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
        firstSchool.setSdcSchoolCollectionStatusCode("COMPLETED");
        var savedSchoolColl = sdcSchoolCollectionRepository.save(firstSchool);

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = models.stream().peek(model -> model.setSdcSchoolCollection(firstSchool)).toList();

        sdcSchoolCollectionStudentRepository.saveAll(students);

        CollectionSagaData sagaData = new CollectionSagaData();
        sagaData.setExistingCollectionID(collection.getCollectionID().toString());
        sagaData.setNewCollectionSignOffDueDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionDuplicationResolutionDueDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionSnapshotDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionSubmissionDueDate(String.valueOf(LocalDate.now()));

        val saga = this.createMockCloseCollectionSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        val event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
        this.closeCollectionOrchestrator.handleEvent(event);

        verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.closeCollectionOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(EventType.CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION);
        assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.NEW_COLLECTION_CREATED);

        val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
        assertThat(savedSagaInDB).isPresent();
        assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
        assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION.toString());

        val updatedCollection = collectionRepository.findById(collection.getCollectionID());
        assertThat(updatedCollection).isPresent();
        assertThat(updatedCollection.get().getCollectionStatusCode()).isEqualTo(CollectionStatus.COMPLETED.toString());

        val updatedStudents = sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(savedSchoolColl.getSdcSchoolCollectionID());
        assertThat(updatedStudents).isNotEmpty();
        val notDeletedStudents = updatedStudents.stream().filter(student -> student.getSdcSchoolCollectionStudentStatusCode().equals(SdcSchoolStudentStatus.DEMOG_UPD.toString())).toList();
        val deletedStudents = updatedStudents.stream().filter(student -> student.getSdcSchoolCollectionStudentStatusCode().equals(SdcSchoolStudentStatus.DELETED.toString())).toList();
        assertThat(deletedStudents.size()).isEqualTo(2);
        assertThat(notDeletedStudents.size()).isEqualTo(6);
    }

    @SneakyThrows
    @Test
    void testHandleEvent_givenEventTypeSAVE_FUNDING_GROUP_SNAPSHOT_shouldExecuteSaveFundingGroupSnapshotNotifications() {
        var typeCode = this.collectionTypeCodeRepository.save(this.createMockCollectionCodeEntityForFeb());
        this.collectionCodeCriteriaRepository.save(this.createMockCollectionCodeCriteriaEntity(typeCode));
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();

        var school = createMockSchool();
        school.setDisplayName("School1");
        school.setMincode("0000001");
        school.setDistrictId(districtID.toString());
        when(this.restUtils.getSchoolBySchoolID(school.getSchoolId())).thenReturn(Optional.of(school));
        when(this.restUtils.getSchoolListGivenCriteria(anyList(), any())).thenReturn(List.of(school));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(null);
        firstSchool.setSdcSchoolCollectionStatusCode("COMPLETED");
        sdcSchoolCollectionRepository.save(firstSchool);

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = models.stream().peek(model -> model.setSdcSchoolCollection(firstSchool)).toList();

        sdcSchoolCollectionStudentRepository.saveAll(students);

        CollectionSagaData sagaData = new CollectionSagaData();
        sagaData.setExistingCollectionID(collection.getCollectionID().toString());
        sagaData.setNewCollectionSignOffDueDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionDuplicationResolutionDueDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionSnapshotDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionSubmissionDueDate(String.valueOf(LocalDate.now()));
        when(this.restUtils.getSchoolFundingGroupsBySchoolID(school.getSchoolId())).thenReturn(Arrays.asList(getIndependentSchoolFundingGroup(school.getSchoolId(), "08"), getIndependentSchoolFundingGroup(school.getSchoolId(), "09"), getIndependentSchoolFundingGroup(school.getSchoolId(), "10")));
        doReturn(List.of(school)).when(restUtils).getAllSchools();

        val saga = this.createMockCloseCollectionSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        val event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION)
                .eventOutcome(NEW_COLLECTION_CREATED)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
        this.closeCollectionOrchestrator.handleEvent(event);

        verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.closeCollectionOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(SAVE_FUNDING_GROUP_SNAPSHOT);
        assertThat(newEvent.getEventOutcome()).isEqualTo(FUNDING_GROUP_SNAPSHOT_SAVED);

        val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
        val fundingSnapshots = this.independentSchoolFundingGroupSnapshotRepository.findAll();
        assertThat(savedSagaInDB).isPresent();
        assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
        assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(SAVE_FUNDING_GROUP_SNAPSHOT.toString());
        assertThat(fundingSnapshots).hasSize(3);
    }

    @SneakyThrows
    @Test
    void testHandleEvent_givenEventTypeSEND_CLOSURE_NOTIFICATIONS_shouldExecuteSendClosureNotifications() {
        var typeCode = this.collectionTypeCodeRepository.save(this.createMockCollectionCodeEntityForFeb());
        this.collectionCodeCriteriaRepository.save(this.createMockCollectionCodeCriteriaEntity(typeCode));
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());

        CollectionSagaData sagaData = new CollectionSagaData();
        sagaData.setExistingCollectionID(collection.getCollectionID().toString());
        sagaData.setNewCollectionSignOffDueDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionDuplicationResolutionDueDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionSnapshotDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionSubmissionDueDate(String.valueOf(LocalDate.now()));

        val saga = this.createMockCloseCollectionSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        val event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(SAVE_FUNDING_GROUP_SNAPSHOT)
                .eventOutcome(FUNDING_GROUP_SNAPSHOT_SAVED)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
        this.closeCollectionOrchestrator.handleEvent(event);

        verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.closeCollectionOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(EventType.SEND_CLOSURE_NOTIFICATIONS);
        assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.CLOSURE_NOTIFICATIONS_DISPATCHED);

        val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
        assertThat(savedSagaInDB).isPresent();
        assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
        assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(SEND_CLOSURE_NOTIFICATIONS.toString());
    }

    @SneakyThrows
    @Test
    void testHandleEvent_givenEventTypeInitiatedAndSchoolCollectionNOTCompleted_shouldExecuteCloseCurrentCollAndOpenNewCollWithEventOutComeNEW_COLLECTION_CREATED() {
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

        sdcSchoolCollectionStudentRepository.saveAll(students);

        CollectionSagaData sagaData = new CollectionSagaData();
        sagaData.setExistingCollectionID(collection.getCollectionID().toString());
        sagaData.setNewCollectionSignOffDueDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionDuplicationResolutionDueDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionSnapshotDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionSubmissionDueDate(String.valueOf(LocalDate.now()));

        val saga = this.createMockCloseCollectionSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        val event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.INITIATED)
                .eventOutcome(EventOutcome.INITIATE_SUCCESS)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
        this.closeCollectionOrchestrator.handleEvent(event);

        verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.closeCollectionOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(EventType.CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION);
        assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.NEW_COLLECTION_CREATED);

        val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
        assertThat(savedSagaInDB).isPresent();
        assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
        assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(CLOSE_CURRENT_COLLECTION_AND_OPEN_NEW_COLLECTION.toString());

        val updatedCollection = collectionRepository.findById(collection.getCollectionID());
        assertThat(updatedCollection).isPresent();
        assertThat(updatedCollection.get().getCollectionStatusCode()).isEqualTo(CollectionStatus.COMPLETED.toString());

        val updatedSchoolCollection = sdcSchoolCollectionRepository.findAllByCollectionEntityCollectionID(collection.getCollectionID());
        assertThat(updatedSchoolCollection).isNotEmpty();
        assertThat(updatedSchoolCollection.get(0).getSdcSchoolCollectionStatusCode()).isEqualTo(CollectionStatus.COMPLETED.toString());

        val updatedStudents = sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(savedSchoolColl.getSdcSchoolCollectionID());
        assertThat(updatedStudents).isEmpty();
    }

    @SneakyThrows
    @Test
    void testHandleEvent_givenEventTypeSEND_CLOSURE_NOTIFICATIONS_shouldCOMPLETESaga() {
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
        firstSchool.setSdcSchoolCollectionStatusCode("COMPLETED");
        sdcSchoolCollectionRepository.save(firstSchool);

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = models.stream().peek(model -> model.setSdcSchoolCollection(firstSchool)).toList();

        sdcSchoolCollectionStudentRepository.saveAll(students);

        CollectionSagaData sagaData = new CollectionSagaData();
        sagaData.setExistingCollectionID(collection.getCollectionID().toString());
        sagaData.setNewCollectionSignOffDueDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionDuplicationResolutionDueDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionSnapshotDate(String.valueOf(LocalDate.now()));
        sagaData.setNewCollectionSubmissionDueDate(String.valueOf(LocalDate.now()));

        val saga = this.createMockCloseCollectionSaga(sagaData);
        saga.setSagaId(null);
        this.sagaRepository.save(saga);

        val event = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.SEND_CLOSURE_NOTIFICATIONS)
                .eventOutcome(EventOutcome.CLOSURE_NOTIFICATIONS_DISPATCHED)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
        this.closeCollectionOrchestrator.handleEvent(event);

        val savedSagaInDB = this.sagaRepository.findById(saga.getSagaId());
        assertThat(savedSagaInDB).isPresent();
        assertThat(savedSagaInDB.get().getStatus()).isEqualTo(COMPLETED.toString());
        assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
        verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.closeCollectionOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
        final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
        assertThat(newEvent.getEventType()).isEqualTo(EventType.MARK_SAGA_COMPLETE);
        assertThat(newEvent.getEventOutcome()).isEqualTo(EventOutcome.SAGA_COMPLETED);
    }

}
