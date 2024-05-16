package ca.bc.gov.educ.studentdatacollection.api.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;

import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;

import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.when;

public class EventTaskSchedulerTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    EventTaskScheduler eventTaskScheduler;

    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    @Autowired
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

    @Autowired
    SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

    @Autowired
    RestUtils restUtils;
    private SdcSchoolCollectionEntity firstSchoolCollection;
    private SdcSchoolCollectionEntity secondSchoolCollection;

    @BeforeEach
    public void setUp() throws Exception {
        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @AfterEach
    void cleanup(){
        sdcDistrictCollectionRepository.deleteAll();
        collectionRepository.deleteAll();
        sdcSchoolCollectionStudentRepository.deleteAll();
        sdcSchoolCollectionRepository.deleteAll();
    }



    @Test
    void testFindSchoolCollectionsForSubmission_HasErrorHasDuplicates_shouldSetCollectionStatusToVerified() throws IOException, InterruptedException {
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

        final List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.findSchoolCollectionsWithStudentsNotInLoadedStatus();
        assertThat(sdcSchoolCollectionEntity).hasSize(2);
        eventTaskScheduler.submitSchoolCollections();

        var sdcSchoolCollectionEntityAfterSubmit = sdcSchoolCollectionRepository.findSchoolCollectionsWithStudentsNotInLoadedStatus();
        assertThat(sdcSchoolCollectionEntityAfterSubmit).isEmpty();

        var firstSchoolEntity = sdcSchoolCollectionRepository.findById(firstSchoolCollection.getSdcSchoolCollectionID());
        assertThat(firstSchoolEntity.get().getSdcSchoolCollectionStatusCode()).isEqualTo(SdcSchoolCollectionStatus.VERIFIED.getCode());

        var secondSchoolEntity = sdcSchoolCollectionRepository.findById(secondSchoolCollection.getSdcSchoolCollectionID());
        assertThat(secondSchoolEntity.get().getSdcSchoolCollectionStatusCode()).isEqualTo(SdcSchoolCollectionStatus.LOADED.getCode());
    }



    public void setMockDataForSchoolCollectionsForSubmissionFn() throws IOException {
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
