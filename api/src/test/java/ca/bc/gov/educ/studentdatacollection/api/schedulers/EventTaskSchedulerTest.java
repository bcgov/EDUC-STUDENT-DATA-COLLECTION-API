package ca.bc.gov.educ.studentdatacollection.api.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.events.schedulers.EventTaskSchedulerAsyncService;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionsForAutoSubmit;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventTaskSchedulerTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    EventTaskScheduler eventTaskScheduler;

    @Autowired
    EventTaskSchedulerAsyncService eventTaskSchedulerAsyncService;

    @Autowired
    SagaRepository sagaRepository;

    @Autowired
    SagaEventRepository sagaEventRepository;

    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    @Autowired
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

    @Autowired
    SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

    @Autowired
    RestUtils restUtils;
    @Autowired
    CollectionCodeCriteriaRepository collectionCodeCriteriaRepository;
    @Autowired
    CollectionTypeCodeRepository collectionTypeCodeRepository;
    private SdcSchoolCollectionEntity firstSchoolCollection;
    private SdcSchoolCollectionEntity secondSchoolCollection;
    @Autowired
    MessagePublisher messagePublisher;
    @Captor
    ArgumentCaptor<byte[]> eventCaptor;

    @AfterEach
    void cleanup(){
        sagaEventRepository.deleteAll();
        sagaRepository.deleteAll();
        sdcDistrictCollectionRepository.deleteAll();
        collectionRepository.deleteAll();
        sdcSchoolCollectionStudentRepository.deleteAll();
        sdcSchoolCollectionRepository.deleteAll();
    }

    @Test
    void testFindSchoolCollectionsForSubmission_HasErrorHasDuplicates_shouldSetCollectionStatusToVerified() {
        setMockDataForSchoolCollectionsForSubmissionFn();

        var studentID = UUID.randomUUID();
        var student1 = createMockSchoolStudentEntity(firstSchoolCollection);
        student1.setAssignedStudentId(studentID);
        student1.setSdcSchoolCollectionStudentStatusCode("INFOWARN");
        sdcSchoolCollectionStudentRepository.save(student1);
        var student2 = createMockSchoolStudentEntity(firstSchoolCollection);
        student2.setSdcSchoolCollectionStudentStatusCode("INFOWARN");
        student2.setAssignedStudentId(studentID);
        sdcSchoolCollectionStudentRepository.save(student2);

        var studentInSecondSchool = createMockSchoolStudentEntity(secondSchoolCollection);
        studentInSecondSchool.setAssignedStudentId(UUID.randomUUID());
        studentInSecondSchool.setSdcSchoolCollectionStudentStatusCode("ERROR");
        sdcSchoolCollectionStudentRepository.save(studentInSecondSchool);

        final List<SdcSchoolCollectionsForAutoSubmit> sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.findSchoolCollectionsWithStudentsNotInLoadedStatus("10");
        assertThat(sdcSchoolCollectionEntity).hasSize(2);
        eventTaskScheduler.submitSchoolCollections();

        var sdcSchoolCollectionEntityAfterSubmit = sdcSchoolCollectionRepository.findSchoolCollectionsWithStudentsNotInLoadedStatus("10");
        assertThat(sdcSchoolCollectionEntityAfterSubmit).isEmpty();

        var firstSchoolEntity = sdcSchoolCollectionRepository.findById(firstSchoolCollection.getSdcSchoolCollectionID());
        assertThat(firstSchoolEntity.get().getSdcSchoolCollectionStatusCode()).isEqualTo(SdcSchoolCollectionStatus.VERIFIED.getCode());

        var secondSchoolEntity = sdcSchoolCollectionRepository.findById(secondSchoolCollection.getSdcSchoolCollectionID());
        assertThat(secondSchoolEntity.get().getSdcSchoolCollectionStatusCode()).isEqualTo(SdcSchoolCollectionStatus.LOADED.getCode());
    }

    @Test
    void testFindSchoolCollectionsForSubmission_WithStatusCode_LOADEDAndERROR_shouldReturn1SchoolForSubmission() {
        setMockDataForSchoolCollectionsForSubmissionFn();

        var studentID = UUID.randomUUID();
        var student1 = createMockSchoolStudentEntity(firstSchoolCollection);
        student1.setAssignedStudentId(studentID);
        student1.setSdcSchoolCollectionStudentStatusCode("LOADED");
        sdcSchoolCollectionStudentRepository.save(student1);
        var student2 = createMockSchoolStudentEntity(firstSchoolCollection);
        student2.setSdcSchoolCollectionStudentStatusCode("ERROR");
        student2.setAssignedStudentId(studentID);
        sdcSchoolCollectionStudentRepository.save(student2);


        var studentInSecondSchool = createMockSchoolStudentEntity(secondSchoolCollection);
        studentInSecondSchool.setAssignedStudentId(UUID.randomUUID());
        studentInSecondSchool.setSdcSchoolCollectionStudentStatusCode("ERROR");
        sdcSchoolCollectionStudentRepository.save(studentInSecondSchool);

        final List<SdcSchoolCollectionsForAutoSubmit> sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.findSchoolCollectionsWithStudentsNotInLoadedStatus("10");
        assertThat(sdcSchoolCollectionEntity).hasSize(2);
    }

    @Test
    void testFindIndySchoolSubmissions_WithStatusCode_LOADEDAndNEW_shouldReturnOk() {

        var collection = createMockCollectionEntity();
        collection.setCollectionStatusCode("INPROGRESS");
        collectionRepository.save(collection);

        var schoolDetail1 = createMockSchoolDetail();
        schoolDetail1.setDisplayName("School1");
        schoolDetail1.setMincode("0000001");
        var schoolDetail2 = createMockSchoolDetail();
        schoolDetail2.setDisplayName("School2");
        schoolDetail2.setMincode("0000002");

        when(this.restUtils.getSchoolDetails(UUID.fromString(schoolDetail1.getSchoolId()))).thenReturn(schoolDetail1);
        when(this.restUtils.getSchoolDetails(UUID.fromString(schoolDetail2.getSchoolId()))).thenReturn(schoolDetail2);

        firstSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolDetail1.getSchoolId()));
        firstSchoolCollection.setUploadDate(null);
        firstSchoolCollection.setUploadFileName(null);
        firstSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());

        secondSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolDetail2.getSchoolId()));
        secondSchoolCollection.setUploadDate(null);
        secondSchoolCollection.setUploadFileName(null);
        secondSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.LOADED.getCode());
        secondSchoolCollection.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchoolCollection, secondSchoolCollection));

        eventTaskScheduler.notifyIndySchoolsToSubmit();

        var sagas = sagaRepository.findAll();
        assertThat(sagas).hasSize(2);
    }

    @Test
    void testFindIndySchoolSubmissions_WithStatusCode_LOADEDAndNEW_ExistingSaga_shouldReturnOk() throws JsonProcessingException {

        var collection = createMockCollectionEntity();
        collection.setCollectionStatusCode("INPROGRESS");
        collectionRepository.save(collection);

        var schoolDetail1 = createMockSchoolDetail();
        schoolDetail1.setDisplayName("School1");
        schoolDetail1.setMincode("0000001");
        var schoolDetail2 = createMockSchoolDetail();
        schoolDetail2.setDisplayName("School2");
        schoolDetail2.setMincode("0000002");

        when(this.restUtils.getSchoolDetails(UUID.fromString(schoolDetail1.getSchoolId()))).thenReturn(schoolDetail1);
        when(this.restUtils.getSchoolDetails(UUID.fromString(schoolDetail2.getSchoolId()))).thenReturn(schoolDetail2);

        firstSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolDetail1.getSchoolId()));
        firstSchoolCollection.setUploadDate(null);
        firstSchoolCollection.setUploadFileName(null);
        firstSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());
        var savedCollection1 = sdcSchoolCollectionRepository.save(firstSchoolCollection);

        secondSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolDetail2.getSchoolId()));
        secondSchoolCollection.setUploadDate(null);
        secondSchoolCollection.setUploadFileName(null);
        secondSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.LOADED.getCode());
        secondSchoolCollection.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        sdcSchoolCollectionRepository.save(secondSchoolCollection);

        var saga = SdcSagaEntity.builder()
                .updateDate(LocalDateTime.now().minusMinutes(15))
                .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
                .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
                .createDate(LocalDateTime.now().minusMinutes(15))
                .sagaName(SagaEnum.INDY_SCHOOLS_NO_ACTIVITY_EMAIL_SAGA.toString())
                .sdcSchoolCollectionID(savedCollection1.getSdcSchoolCollectionID())
                .status(SagaStatusEnum.COMPLETED.toString())
                .sagaState(EventType.MARK_SAGA_COMPLETE.toString())
                .payload(JsonUtil.getJsonStringFromObject(schoolDetail1))
                .build();
        sagaRepository.save(saga);

        eventTaskSchedulerAsyncService.findAllUnsubmittedIndependentSchoolsInCurrentCollection();

        var sagas = sagaRepository.findAll();
        assertThat(sagas).hasSize(2);
    }

    @Test
    void testFindIndySchoolSubmissions_InJulyCollection_shouldNotSendNotifications() throws JsonProcessingException {

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode("JULY");
        collection.setCollectionStatusCode("INPROGRESS");
        collectionRepository.save(collection);

        var schoolDetail1 = createMockSchoolDetail();
        schoolDetail1.setDisplayName("School1");
        schoolDetail1.setMincode("0000001");
        var schoolDetail2 = createMockSchoolDetail();
        schoolDetail2.setDisplayName("School2");
        schoolDetail2.setMincode("0000002");

        when(this.restUtils.getSchoolDetails(UUID.fromString(schoolDetail1.getSchoolId()))).thenReturn(schoolDetail1);
        when(this.restUtils.getSchoolDetails(UUID.fromString(schoolDetail2.getSchoolId()))).thenReturn(schoolDetail2);

        firstSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolDetail1.getSchoolId()));
        firstSchoolCollection.setUploadDate(null);
        firstSchoolCollection.setUploadFileName(null);
        firstSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());
        var savedCollection1 = sdcSchoolCollectionRepository.save(firstSchoolCollection);

        secondSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolDetail2.getSchoolId()));
        secondSchoolCollection.setUploadDate(null);
        secondSchoolCollection.setUploadFileName(null);
        secondSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.LOADED.getCode());
        secondSchoolCollection.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        sdcSchoolCollectionRepository.save(secondSchoolCollection);

        var saga = SdcSagaEntity.builder()
                .updateDate(LocalDateTime.now().minusMinutes(15))
                .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
                .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
                .createDate(LocalDateTime.now().minusMinutes(15))
                .sagaName(SagaEnum.INDY_SCHOOLS_NO_ACTIVITY_EMAIL_SAGA.toString())
                .sdcSchoolCollectionID(savedCollection1.getSdcSchoolCollectionID())
                .status(SagaStatusEnum.COMPLETED.toString())
                .sagaState(EventType.MARK_SAGA_COMPLETE.toString())
                .payload(JsonUtil.getJsonStringFromObject(schoolDetail1))
                .build();
        sagaRepository.save(saga);

        eventTaskSchedulerAsyncService.findAllUnsubmittedIndependentSchoolsInCurrentCollection();

        var sagas = sagaRepository.findAll();
        assertThat(sagas).hasSize(1);
    }

    @Test
    void testFindsNewSchoolAndAddsSdcSchoolCollection() {
        setMockDataForSchoolCollectionsForSubmissionFn();

        SchoolTombstone newSchool = createMockSchoolTombstone();
        UUID newSchoolUUID = UUID.randomUUID();
        newSchool.setSchoolId(newSchoolUUID.toString());
        List<SchoolTombstone> mockSchools = List.of(newSchool);
        when(restUtils.getSchools()).thenReturn(mockSchools);

        eventTaskSchedulerAsyncService.findNewSchoolsAndAddSdcSchoolCollection();

        List<SdcSchoolCollectionEntity> savedSchoolCollections = sdcSchoolCollectionRepository.findAllBySchoolID(newSchoolUUID);

        assertThat(savedSchoolCollections).isNotEmpty();
        assertThat(savedSchoolCollections.get(0).getSchoolID()).isEqualTo(newSchoolUUID);
    }

    @Test
    void testDoesNotFindsNewSchoolAndDoesNotAddsSdcSchoolCollection() {
        setMockDataForSchoolCollectionsForSubmissionFn();

        SchoolTombstone newSchool = createMockSchoolTombstone();
        UUID notNewSchoolUUID = firstSchoolCollection.getSchoolID();
        newSchool.setSchoolId(notNewSchoolUUID.toString());
        List<SchoolTombstone> mockSchools = List.of(newSchool);
        when(restUtils.getSchools()).thenReturn(mockSchools);

        eventTaskSchedulerAsyncService.findNewSchoolsAndAddSdcSchoolCollection();

        List<SdcSchoolCollectionEntity> savedSchoolCollections = sdcSchoolCollectionRepository.findAllBySchoolID(notNewSchoolUUID);

        assertThat(savedSchoolCollections).hasSize(1);
        assertThat(savedSchoolCollections.get(0).getSchoolID()).isEqualTo(notNewSchoolUUID);
        assertThat(savedSchoolCollections.get(0).getCreateUser()).isEqualTo("ABC");
        assertThat(savedSchoolCollections.get(0).getCreateUser()).isNotEqualTo("NEW_SCHOOLS_CRON");
    }

    @Test
    void testFindsNewSchoolFutureOpenDateAndDoesNotAddSdcSchoolCollection() {
        setMockDataForSchoolCollectionsForSubmissionFn();

        SchoolTombstone newSchool = createMockSchoolTombstone();
        newSchool.setOpenedDate(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now().plusDays(10)));
        UUID newSchoolUUID = UUID.randomUUID();
        newSchool.setSchoolId(newSchoolUUID.toString());
        List<SchoolTombstone> mockSchools = List.of(newSchool);
        when(restUtils.getSchools()).thenReturn(mockSchools);

        eventTaskSchedulerAsyncService.findNewSchoolsAndAddSdcSchoolCollection();

        List<SdcSchoolCollectionEntity> savedSchoolCollections = sdcSchoolCollectionRepository.findAllBySchoolID(newSchoolUUID);

        assertThat(savedSchoolCollections).isEmpty();
    }

    @Test
    void testFindsNewSchoolWithClosedDateAndDoesNotAddSdcSchoolCollection() {
        setMockDataForSchoolCollectionsForSubmissionFn();

        SchoolTombstone newSchool = createMockSchoolTombstone();
        newSchool.setClosedDate(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now().minusDays(10)));
        UUID newSchoolUUID = UUID.randomUUID();
        newSchool.setSchoolId(newSchoolUUID.toString());
        List<SchoolTombstone> mockSchools = List.of(newSchool);
        when(restUtils.getSchools()).thenReturn(mockSchools);

        eventTaskSchedulerAsyncService.findNewSchoolsAndAddSdcSchoolCollection();

        List<SdcSchoolCollectionEntity> savedSchoolCollections = sdcSchoolCollectionRepository.findAllBySchoolID(newSchoolUUID);

        assertThat(savedSchoolCollections).isEmpty();
    }

    @Test
    void testFindsNewSchoolColInProvDupsAndDoesNotAddSdcSchoolCollection() {
        setMockDataForSchoolCollectionsForSubmissionFn();

        CollectionEntity col = firstSchoolCollection.getCollectionEntity();
        col.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());
        collectionRepository.save(col);

        SchoolTombstone newSchool = createMockSchoolTombstone();
        UUID newSchoolUUID = UUID.randomUUID();
        newSchool.setSchoolId(newSchoolUUID.toString());
        List<SchoolTombstone> mockSchools = List.of(newSchool);
        when(restUtils.getSchools()).thenReturn(mockSchools);

        eventTaskSchedulerAsyncService.findNewSchoolsAndAddSdcSchoolCollection();

        List<SdcSchoolCollectionEntity> savedSchoolCollections = sdcSchoolCollectionRepository.findAllBySchoolID(newSchoolUUID);

        assertThat(savedSchoolCollections).isEmpty();
    }

    public void setMockDataForSchoolCollectionsForSubmissionFn() {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        firstSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchoolCollection.setUploadDate(null);
        firstSchoolCollection.setUploadFileName(null);
        firstSchoolCollection.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        firstSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.DISTRICT_UPLOAD.getCode());

        secondSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchoolCollection.setUploadDate(null);
        secondSchoolCollection.setUploadFileName(null);
        secondSchoolCollection.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        secondSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.DISTRICT_UPLOAD.getCode());
        secondSchoolCollection.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchoolCollection, secondSchoolCollection));
    }

}
