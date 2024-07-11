package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolFundingCodes;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentEnrolledProgramRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
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
    SdcSchoolCollectionStudentEnrolledProgramRepository sdcSchoolCollectionStudentEnrolledProgramRepository;

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
    void testGetHeadersDistrictScope() {
        setupNoneEligibleOneReported(false);

        helper = new RefugeeHeadcountHelper(sdcSchoolCollectionRepository, studentRepository, sdcDistrictCollectionRepository, restUtils);
        List<HeadcountHeader> headers = helper.getHeaders(mockDistrictCollectionEntityFeb.getSdcDistrictCollectionID(), true);

        assertEquals(1, headers.size());
        HeadcountHeader header = headers.get(0);
        assertEquals("Newcomer Refugees", header.getTitle());
        Map<String, HeadcountHeaderColumn> columns = header.getColumns();
        assertEquals("0", columns.get("Eligible").getCurrentValue());
        assertEquals("1", columns.get("Reported").getCurrentValue());
    }

    @Test
    void testGetHeadersSchoolScope() {
        saveRefugeeStudents();
        helper = new RefugeeHeadcountHelper(sdcSchoolCollectionRepository, studentRepository, sdcDistrictCollectionRepository, restUtils);
        List<HeadcountHeader> headers = helper.getHeaders(sdcSchoolCollectionEntityFeb.getSdcSchoolCollectionID(), false);

        assertEquals(1, headers.size());
        HeadcountHeader header = headers.get(0);
        assertEquals("Newcomer Refugees", header.getTitle());
        Map<String, HeadcountHeaderColumn> columns = header.getColumns();
        assertEquals("3", columns.get("Eligible").getCurrentValue());
        assertEquals("3", columns.get("Reported").getCurrentValue());
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

        RefugeeHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getRefugeeHeadersBySdcDistrictCollectionId(mockDistrictCollectionEntityFeb.getSdcDistrictCollectionID());
        assertEquals("6", result.getReportedStudents());
        assertEquals("6", result.getEligibleStudents());

        helper = new RefugeeHeadcountHelper(sdcSchoolCollectionRepository, studentRepository, sdcDistrictCollectionRepository, restUtils);
        List<HeadcountHeader> headers = helper.getHeaders(mockDistrictCollectionEntityFeb.getSdcDistrictCollectionID(), true);
        assertEquals(1, headers.size());
        HeadcountHeader header = headers.get(0);
        assertEquals("Newcomer Refugees", header.getTitle());
        assertEquals("6", header.getColumns().get("Eligible").getCurrentValue());
        assertEquals("6", header.getColumns().get("Reported").getCurrentValue());
    }

    @Test
    void testGetRefugeeHeadersBySdcDistrictCollectionIdNotEligMockValidationIssueInPrevCol() {
        setupNoneEligibleOneReported(false);

        RefugeeHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getRefugeeHeadersBySdcDistrictCollectionId(mockDistrictCollectionEntityFeb.getSdcDistrictCollectionID());
        assertEquals("1", result.getReportedStudents());
        assertEquals("0", result.getEligibleStudents());

        helper = new RefugeeHeadcountHelper(sdcSchoolCollectionRepository, studentRepository, sdcDistrictCollectionRepository, restUtils);
        List<HeadcountHeader> headers = helper.getHeaders(sdcSchoolCollectionEntityFeb.getSdcSchoolCollectionID(), false);
        assertEquals(1, headers.size());
        HeadcountHeader header = headers.get(0);
        assertEquals("Newcomer Refugees", header.getTitle());
        assertEquals("0", header.getColumns().get("Eligible").getCurrentValue());
        assertEquals("1", header.getColumns().get("Reported").getCurrentValue());
    }

    @Test
    void testGetRefugeeHeadersBySdcDistrictCollectionIdNotEligMockValidationIssueRefugeeAdult() {
        setupNoneEligibleOneReported(true);

        RefugeeHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getRefugeeHeadersBySdcDistrictCollectionId(mockDistrictCollectionEntityFeb.getSdcDistrictCollectionID());
        assertEquals("1", result.getReportedStudents());
        assertEquals("0", result.getEligibleStudents());

        helper = new RefugeeHeadcountHelper(sdcSchoolCollectionRepository, studentRepository, sdcDistrictCollectionRepository, restUtils);
        List<HeadcountHeader> headers = helper.getHeaders(sdcSchoolCollectionEntityFeb.getSdcSchoolCollectionID(), false);
        assertEquals(1, headers.size());
        HeadcountHeader header = headers.get(0);
        assertEquals("Newcomer Refugees", header.getTitle());
        assertEquals("0", header.getColumns().get("Eligible").getCurrentValue());
        assertEquals("1", header.getColumns().get("Reported").getCurrentValue());
    }

    void saveRefugeeStudents(){
        SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntityFeb);
        student1.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        student1.setFte(new BigDecimal("0.5"));
        studentRepository.save(student1);

        SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntityFeb);
        student2.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        student2.setFte(new BigDecimal("1.0"));
        studentRepository.save(student2);

        SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcSchoolCollectionEntityFeb);
        student3.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        student3.setFte(new BigDecimal("0.5"));
        studentRepository.save(student3);

        var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg.setEnrolledProgramCode("17");
        enrolledProg.setSdcSchoolCollectionStudentEntity(student3);
        enrolledProg.setCreateUser("ABC");
        enrolledProg.setUpdateUser("ABC");
        enrolledProg.setCreateDate(LocalDateTime.now());
        enrolledProg.setUpdateDate(LocalDateTime.now());
        student3.setSdcStudentEnrolledProgramEntities(Collections.singleton(enrolledProg));
        studentRepository.save(student3);
    }

    void setupNoneEligibleOneReported(boolean isAdult) {
        UUID assignedStudentId = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity studFeb = createMockSchoolStudentEntity(sdcSchoolCollectionEntityFeb);
        studFeb.setAssignedStudentId(assignedStudentId);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        sdcSchoolCollectionStudentRepository.save(studFeb);

        SdcSchoolCollectionStudentValidationIssueEntity refugeeInSept = new SdcSchoolCollectionStudentValidationIssueEntity();

        refugeeInSept.setSdcSchoolCollectionStudentValidationIssueID(UUID.randomUUID());
        refugeeInSept.setSdcSchoolCollectionStudentEntity(studFeb);
        if (Boolean.TRUE.equals(isAdult)) {
            refugeeInSept.setValidationIssueCode("REFUGEEISADULT");
        } else {
            refugeeInSept.setValidationIssueCode("REFUGEEINPREVCOL");
        }
        refugeeInSept.setValidationIssueSeverityCode("TEST");
        refugeeInSept.setValidationIssueFieldCode("TEST");
        refugeeInSept.setCreateUser("ABC");
        refugeeInSept.setCreateDate(LocalDateTime.now());
        refugeeInSept.setUpdateUser("ABC");
        refugeeInSept.setUpdateDate(LocalDateTime.now());

        studFeb.setSdcStudentValidationIssueEntities(Collections.singleton(refugeeInSept));
        sdcSchoolCollectionStudentRepository.save(studFeb);
    }
}
