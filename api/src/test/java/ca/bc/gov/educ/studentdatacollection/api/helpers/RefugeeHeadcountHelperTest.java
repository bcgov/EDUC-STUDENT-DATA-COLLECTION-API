package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolFundingCodes;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.RefugeeHeadcountHeaderResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = StudentDataCollectionApiApplication.class)
class RefugeeHeadcountHelperTest extends BaseStudentDataCollectionAPITest {
    @Autowired
    private SdcSchoolCollectionStudentRepository studentRepository;

    private RefugeeHeadcountHelper helper;

    private SdcDistrictCollectionEntity mockDistrictCollectionEntityFeb;
    private SdcSchoolCollectionEntity sdcSchoolCollectionEntityFeb;
    private SdcSchoolCollectionEntity sdcSchoolCollectionEntitySept;

    @Autowired
    private SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    @Autowired
    SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

    @Autowired
    RestUtils restUtils;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentSnapshotDate = now.toLocalDate();

        CollectionEntity collectionSept = createMockCollectionEntity();
        collectionSept.setCloseDate(now.minusDays(5));
        collectionSept.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collectionSept.setSnapshotDate(currentSnapshotDate.minusYears(1).withMonth(9).withDayOfMonth(30));
        collectionRepository.save(collectionSept);

        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(now.plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionFeb.setSnapshotDate(currentSnapshotDate);
        collectionRepository.save(collectionFeb);

        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());

        SdcDistrictCollectionEntity sdcDistrictCollectionSept = createMockSdcDistrictCollectionEntity(collectionSept, UUID.fromString(district.getDistrictId()));
        SdcDistrictCollectionEntity sdcDistrictCollectionFeb = createMockSdcDistrictCollectionEntity(collectionFeb, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionSept);
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionFeb);

        mockDistrictCollectionEntityFeb = sdcDistrictCollectionFeb;

        SdcSchoolCollectionEntity sdcMockSchoolSept = createMockSdcSchoolCollectionEntity(collectionSept, schoolId);
        sdcMockSchoolSept.setUploadDate(null);
        sdcMockSchoolSept.setUploadFileName(null);
        sdcMockSchoolSept.setSdcDistrictCollectionID(sdcDistrictCollectionSept.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolSept);

        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, schoolId);
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcMockSchoolFeb.setSdcDistrictCollectionID(sdcDistrictCollectionFeb.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        sdcSchoolCollectionEntityFeb = sdcMockSchoolFeb;
        sdcSchoolCollectionEntitySept = sdcMockSchoolSept;
    }

    @AfterEach
    void cleanup(){
        studentRepository.deleteAll();
        sdcSchoolCollectionRepository.deleteAll();
        sdcDistrictCollectionRepository.deleteAll();
        collectionRepository.deleteAll();
    }

    @Test
    void testGetRefugeeHeadersBySdcDistrictCollectionIdAllElig() {
        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities;
        try {
            entities = new ObjectMapper().readValue(file, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = models.stream().peek(model -> {
                    var studentId = UUID.randomUUID();
                    model.setAssignedStudentId(studentId);
                    model.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
                    model.setSdcSchoolCollection(sdcSchoolCollectionEntityFeb);
                })
                .toList();

        sdcSchoolCollectionStudentRepository.saveAll(students);

        helper = new RefugeeHeadcountHelper(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository);
        helper.getHeaders(mockDistrictCollectionEntityFeb.getSdcDistrictCollectionID(), true);
        RefugeeHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getRefugeeHeadersBySdcDistrictCollectionId(mockDistrictCollectionEntityFeb.getSdcDistrictCollectionID());
        assertEquals("8", result.getAllStudents());
        assertEquals("8", result.getReportedStudents());
        assertEquals("8", result.getEligibleStudents());
    }

    @Test
    void testGetRefugeeHeadersBySdcDistrictCollectionIdNotEligMockValidationIssue() {
        UUID assignedStudentId = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity studFeb = createMockSchoolStudentEntity(sdcSchoolCollectionEntityFeb);
        studFeb.setAssignedStudentId(assignedStudentId);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        sdcSchoolCollectionStudentRepository.save(studFeb);

        SdcSchoolCollectionStudentValidationIssueEntity refugeeInSept = new SdcSchoolCollectionStudentValidationIssueEntity();

        refugeeInSept.setSdcSchoolCollectionStudentValidationIssueID(UUID.randomUUID());
        refugeeInSept.setSdcSchoolCollectionStudentEntity(studFeb);
        refugeeInSept.setValidationIssueCode("REFUGEEINSEPTCOL");
        refugeeInSept.setValidationIssueSeverityCode("TEST");
        refugeeInSept.setValidationIssueFieldCode("TEST");
        refugeeInSept.setCreateUser("ABC");
        refugeeInSept.setCreateDate(LocalDateTime.now());
        refugeeInSept.setUpdateUser("ABC");
        refugeeInSept.setUpdateDate(LocalDateTime.now());

        studFeb.setSdcStudentValidationIssueEntities(Collections.singleton(refugeeInSept));
        sdcSchoolCollectionStudentRepository.save(studFeb);

        RefugeeHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getRefugeeHeadersBySdcDistrictCollectionId(mockDistrictCollectionEntityFeb.getSdcDistrictCollectionID());
        assertEquals("1", result.getAllStudents());
        assertEquals("1", result.getReportedStudents());
        assertEquals("0", result.getEligibleStudents());
    }
}
