
package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EllHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = StudentDataCollectionApiApplication.class)
class EllHeadcountHelperTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private SdcSchoolCollectionStudentRepository studentRepository;

    private EllHeadcountHelper helper;

    private SdcDistrictCollectionEntity mockDistrictCollectionEntity;

    @Autowired
    private SdcSchoolCollectionRepository schoolCollectionRepository;

    @Autowired
    RestUtils restUtils;

    @Autowired
    SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    @Autowired
    SdcStudentEllRepository sdcStudentEllRepository;
    @Autowired
    SdcSchoolCollectionStudentEnrolledProgramRepository sdcSchoolCollectionStudentEnrolledProgramRepository;

    @BeforeEach
    void setUp() throws IOException {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

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

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        secondSchool.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchool, secondSchool));

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = IntStream.range(0, models.size())
                .mapToObj(i -> {
                    var student = models.get(i);
                    var studentId = UUID.randomUUID();
                    var ellEntity = new SdcStudentEllEntity();

                    student.setAssignedStudentId(studentId);
                    //Even students go to the previous year; odd students to the current year.
                    if (i % 2 == 0) {
                        student.setSdcSchoolCollection(secondSchool);
                        ellEntity.setYearsInEll(4);
                    } else {
                        student.setSdcSchoolCollection(firstSchool);
                    }
                    if (i == 1) {
                        ellEntity.setYearsInEll(0);
                        student.setEnrolledProgramCodes("9876543210");
                        student.setEllNonEligReasonCode(ProgramEligibilityIssueCode.NOT_ENROLLED_ELL.getCode());
                    }
                    if (i == 3) {
                        student.setEnrolledProgramCodes("9876543217");
                        student.setEllNonEligReasonCode(ProgramEligibilityIssueCode.HOMESCHOOL.getCode());
                    }
                    if (i == 5) {
                        student.setEnrolledProgramCodes("9876543217");
                        ellEntity.setYearsInEll(6);
                    }
                    if (i == 7) {
                        student.setEnrolledProgramCodes("9876543217");
                        ellEntity.setYearsInEll(4);
                    }

                    ellEntity.setCreateUser("ABC");
                    ellEntity.setUpdateUser("ABC");
                    ellEntity.setCreateDate(LocalDateTime.now());
                    ellEntity.setUpdateDate(LocalDateTime.now());
                    ellEntity.setStudentID(student.getAssignedStudentId());
                    sdcStudentEllRepository.save(ellEntity);

                    return student;
                })
                .toList();

        var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        //All of the previous year students will be reported to an ELL program.
        savedStudents.forEach(student -> {
            if (!StringUtils.equals(
                    ProgramEligibilityIssueCode.NOT_ENROLLED_ELL.getCode(),
                    student.getEllNonEligReasonCode()
            )) {
                var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
                enrolledProg.setEnrolledProgramCode("17");
                enrolledProg.setSdcSchoolCollectionStudentEntity(student);
                enrolledProg.setCreateUser("ABC");
                enrolledProg.setUpdateUser("ABC");
                enrolledProg.setCreateDate(LocalDateTime.now());
                enrolledProg.setUpdateDate(LocalDateTime.now());
                enrolledPrograms.add(enrolledProg);
            }
        });
        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);
    }

    @AfterEach
    void cleanup(){
        studentRepository.deleteAll();
        schoolCollectionRepository.deleteAll();
        sdcDistrictCollectionRepository.deleteAll();
        collectionRepository.deleteAll();
        sdcStudentEllRepository.deleteAll();
        sdcSchoolCollectionStudentEnrolledProgramRepository.deleteAll();
    }

    @Test
    void testConvertCareerBySchoolHeadcountResults_ShouldReturnTableContents(){

        helper = new EllHeadcountHelper(schoolCollectionRepository, studentRepository, sdcDistrictCollectionRepository, restUtils);
        helper.setGradeCodesForDistricts();
        List<EllHeadcountResult> result = sdcSchoolCollectionStudentRepository.getEllHeadcountsByBySchoolIdAndSdcDistrictCollectionId(mockDistrictCollectionEntity.getSdcDistrictCollectionID());

        HeadcountResultsTable actualResultsTable = helper.convertEllBySchoolHeadcountResults(mockDistrictCollectionEntity.getSdcDistrictCollectionID(), result);

        var titles = actualResultsTable.getRows().stream().filter(row ->
                row.get("title").getCurrentValue().equals("0000002 - School2")).findAny();

        assert(titles.isPresent());
        assertEquals("4", titles.get().get("Total").getCurrentValue());
        assertEquals("1", titles.get().get("01").getCurrentValue());
    }

}
