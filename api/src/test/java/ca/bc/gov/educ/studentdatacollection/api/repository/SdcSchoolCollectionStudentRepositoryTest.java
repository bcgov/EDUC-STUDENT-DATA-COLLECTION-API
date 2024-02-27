package ca.bc.gov.educ.studentdatacollection.api.repository;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentEnrolledProgramRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(classes = {StudentDataCollectionApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SdcSchoolCollectionStudentRepositoryTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    @Autowired
    private SdcSchoolCollectionStudentEnrolledProgramRepository enrolledProgramRepository;
    private UUID schoolCollectionId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCareerHeadcountsBySchoolId_givenMultipleEnrolledCodesAndInvalidStudents_shouldIncludeCorrectAndDistinctStudentInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given eligible career program students, non eligible career program students, and non career program students
        var students = getSdcStudentEntities(schoolCollection, 9);
        students.get(1).setCareerProgramCode("XB");
        students.get(2).setCareerProgramCode("XC");
        students.get(3).setCareerProgramCode("XD");
        students.get(4).setCareerProgramCode("XE");
        students.get(5).setCareerProgramCode("XF");
        students.get(6).setCareerProgramCode("XG");
        students.get(7).setCareerProgramCode("XH");
        students.get(8).setCareerProgramCode(null);
        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "40");
        setEnrolledProgramCode(students.get(0), "14");
        setEnrolledProgramCode(students.get(1), "40");
        setEnrolledProgramCode(students.get(2), "41");
        setEnrolledProgramCode(students.get(3), "41");
        setEnrolledProgramCode(students.get(4), "42");
        setEnrolledProgramCode(students.get(5), "42");
        setEnrolledProgramCode(students.get(6), "43");
        setEnrolledProgramCode(students.get(7), "43");

        var headcounts = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
        //then only the eligible career program students are included in the headcounts
        assertEquals("1", headcounts.get(0).getPreparationXA());
        assertEquals("1", headcounts.get(0).getPreparationXB());
        assertEquals("1", headcounts.get(0).getCoopXC());
        assertEquals("1", headcounts.get(0).getCoopXD());
        assertEquals("1", headcounts.get(0).getApprenticeXE());
        assertEquals("1", headcounts.get(0).getApprenticeXF());
        assertEquals("1", headcounts.get(0).getTechYouthXG());
        assertEquals("1", headcounts.get(0).getTechYouthXH());
        assertEquals("1", headcounts.get(0).getAllXA());
        assertEquals("1", headcounts.get(0).getAllXB());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("2", headcounts.get(0).getPreparationTotal());
        assertEquals("2", headcounts.get(0).getCoopTotal());
        assertEquals("2", headcounts.get(0).getApprenticeTotal());
        assertEquals("2", headcounts.get(0).getTechYouthTotal());
        assertEquals("8", headcounts.get(0).getAllTotal());
    }

    @Test
    void testGetCareerHeadcountsBySchoolId_givenStudentInErrorStatus_shouldNotReturnInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two career program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "40");
        setEnrolledProgramCode(students.get(1), "40");

        var headcounts = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getAllTotal());
    }

    @Test
    void testGetFrenchHeadcountsBySchoolId_givenEligibleAndNonEligibleFrenchStudents_shouldHaveCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given eligible french students, non eligible french students, and non french students
        var students = getSdcStudentEntities(schoolCollection, 8);
        students.get(5).setFrenchProgramNonEligReasonCode("NONELIG");
        students.get(6).setFrenchProgramNonEligReasonCode("NONELIG");
        students.get(7).setFrenchProgramNonEligReasonCode("NONELIG");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "08");
        setEnrolledProgramCode(students.get(0), "42");
        setEnrolledProgramCode(students.get(1), "11");
        setEnrolledProgramCode(students.get(2), "14");
        setEnrolledProgramCode(students.get(3), "05");
        setEnrolledProgramCode(students.get(4), "41");
        setEnrolledProgramCode(students.get(5), "08");
        setEnrolledProgramCode(students.get(6), "42");
        setEnrolledProgramCode(students.get(7), "11");

        var headcounts = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySchoolId(schoolCollectionId);
        //then only the eligible french students are included in the headcounts
        assertEquals("1", headcounts.get(0).getSchoolAgedCoreFrench());
        assertEquals("1", headcounts.get(0).getSchoolAgedEarlyFrench());
        assertEquals("1", headcounts.get(0).getSchoolAgedLateFrench());
        assertEquals("3", headcounts.get(0).getSchoolAgedTotals());
        assertEquals("0", headcounts.get(0).getAdultTotals());
        assertEquals("3", headcounts.get(0).getTotalTotals());
    }

    @Test
    void testGetFrenchHeadcountsBySchoolId_givenNonSchoolOrAdultAgedStudents_shouldIncludeInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given 3 french students that are neither adult nor school aged
        var students = getSdcStudentEntities(schoolCollection, 3);
        students.get(0).setIsSchoolAged(false);
        students.get(1).setIsSchoolAged(false);
        students.get(2).setIsSchoolAged(false);

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "08");
        setEnrolledProgramCode(students.get(1), "11");
        setEnrolledProgramCode(students.get(2), "14");

        var headcounts = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySchoolId(schoolCollectionId);
        //then the counts are only included in the totals
        assertEquals("0", headcounts.get(0).getSchoolAgedCoreFrench());
        assertEquals("0", headcounts.get(0).getSchoolAgedEarlyFrench());
        assertEquals("0", headcounts.get(0).getSchoolAgedLateFrench());
        assertEquals("0", headcounts.get(0).getSchoolAgedTotals());
        assertEquals("0", headcounts.get(0).getAdultTotals());
        assertEquals("3", headcounts.get(0).getTotalTotals());
    }

    @Test
    void testGetFrenchHeadcountsBySchoolId_givenStudentInErrorStatus_shouldNotReturnInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two french program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "14");
        setEnrolledProgramCode(students.get(1), "14");

        var headcounts = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySchoolId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getTotalTotals());
    }

    @Test
    void testGetCsfFrenchHeadcountsBySchoolId_givenEligibleAndNonEligibleCsfFrenchStudents_shouldHaveCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given 2 csf french students, one with ineligibility reason, and 1 non csf french student
        var students = getSdcStudentEntities(schoolCollection, 3);
        students.get(0).setFrenchProgramNonEligReasonCode("NONELIG");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "05");
        setEnrolledProgramCode(students.get(1), "41");
        setEnrolledProgramCode(students.get(2), "05");

        var headcounts = sdcSchoolCollectionStudentRepository.getCsfFrenchHeadcountsBySchoolId(schoolCollectionId);
        //then only the one eligible csf french student is included in the headcounts
        assertEquals("1", headcounts.get(0).getSchoolAgedFrancophone());
        assertEquals("0", headcounts.get(0).getAdultFrancophone());
        assertEquals("1", headcounts.get(0).getTotalFrancophone());
    }

    @Test
    void testGetCsfFrenchHeadcountsBySchoolId_givenNonSchoolOrAdultAgedStudents_shouldIncludeInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given 1 csf french student that is neither adult nor student aged
        var students = getSdcStudentEntities(schoolCollection, 1);
        students.get(0).setIsSchoolAged(false);

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "05");

        var headcounts = sdcSchoolCollectionStudentRepository.getCsfFrenchHeadcountsBySchoolId(schoolCollectionId);
        //then headcount is only included in the total column
        assertEquals("0", headcounts.get(0).getSchoolAgedFrancophone());
        assertEquals("0", headcounts.get(0).getAdultFrancophone());
        assertEquals("1", headcounts.get(0).getTotalFrancophone());
    }

    @Test
    void testGetCsfFrenchHeadcountsBySchoolId_givenStudentInErrorStatus_shouldNotReturnInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two csf french program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "05");
        setEnrolledProgramCode(students.get(1), "05");

        var headcounts = sdcSchoolCollectionStudentRepository.getCsfFrenchHeadcountsBySchoolId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getTotalFrancophone());
    }

    @Test
    void testGetEllHeadcountsBySchoolId_givenEligibleAndNonEligibleEllStudents_shouldReturnCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two ell program students, one with non-eligibility reason, and one non ell student
        var students = getSdcStudentEntities(schoolCollection, 3);
        students.get(0).setEllNonEligReasonCode("NONELIG");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "17");
        setEnrolledProgramCode(students.get(1), "17");
        setEnrolledProgramCode(students.get(1), "14");
        setEnrolledProgramCode(students.get(2), "14");

        var headcounts = sdcSchoolCollectionStudentRepository.getEllHeadcountsBySchoolId(schoolCollectionId);
        //then only the eligible ell student is included in headcounts
        assertEquals("1", headcounts.get(0).getTotalEllStudents());
    }

    @Test
    void testGetEllHeadcountsBySchoolId_givenStudentInErrorStatus_shouldNotReturnInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two ell program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "17");
        setEnrolledProgramCode(students.get(1), "17");

        var headcounts = sdcSchoolCollectionStudentRepository.getEllHeadcountsBySchoolId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getTotalEllStudents());
    }

    @Test
    void testGetSpecialEdHeadcountsBySchoolId_givenEligibleAndNonEligibleSpecialEdStudents_shouldReturnCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given 12 special ed program students, one with non-eligibility reason, and 1 non-special ed student
        var students = getSdcStudentEntities(schoolCollection, 13);
        students.get(0).setSpecialEducationCategoryCode("A");
        students.get(0).setSpecialEducationNonEligReasonCode("NONELIG");
        students.get(1).setSpecialEducationCategoryCode("B");
        students.get(2).setSpecialEducationCategoryCode("C");
        students.get(3).setSpecialEducationCategoryCode("D");
        students.get(4).setSpecialEducationCategoryCode("E");
        students.get(5).setSpecialEducationCategoryCode("F");
        students.get(6).setSpecialEducationCategoryCode("G");
        students.get(7).setSpecialEducationCategoryCode("H");
        students.get(8).setSpecialEducationCategoryCode("K");
        students.get(9).setSpecialEducationCategoryCode("P");
        students.get(10).setSpecialEducationCategoryCode("Q");
        students.get(11).setSpecialEducationCategoryCode("R");
        students.get(12).setSpecialEducationCategoryCode(null);

        sdcSchoolCollectionStudentRepository.saveAll(students);
        var headcounts = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySchoolId(schoolCollectionId);
        //then only eligible special ed students included in headcounts
        assertEquals("1", headcounts.get(0).getLevelOnes());
        assertEquals("0", headcounts.get(0).getSpecialEdACodes());
        assertEquals("1", headcounts.get(0).getSpecialEdBCodes());
        assertEquals("5", headcounts.get(0).getLevelTwos());
        assertEquals("1", headcounts.get(0).getSpecialEdCCodes());
        assertEquals("1", headcounts.get(0).getSpecialEdDCodes());
        assertEquals("1", headcounts.get(0).getSpecialEdECodes());
        assertEquals("1", headcounts.get(0).getSpecialEdFCodes());
        assertEquals("1", headcounts.get(0).getSpecialEdGCodes());
        assertEquals("1", headcounts.get(0).getLevelThrees());
        assertEquals("1", headcounts.get(0).getSpecialEdHCodes());
        assertEquals("4", headcounts.get(0).getOtherLevels());
        assertEquals("1", headcounts.get(0).getSpecialEdKCodes());
        assertEquals("1", headcounts.get(0).getSpecialEdPCodes());
        assertEquals("1", headcounts.get(0).getSpecialEdQCodes());
        assertEquals("1", headcounts.get(0).getSpecialEdRCodes());
        assertEquals("11", headcounts.get(0).getAllLevels());
    }

    @Test
    void testGetSpecialEdHeadcountsBySchoolId_givenStudentInErrorStatus_shouldNotReturnInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two special ed program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");
        students.get(0).setSpecialEducationCategoryCode("A");
        students.get(1).setSpecialEducationCategoryCode("A");

        sdcSchoolCollectionStudentRepository.saveAll(students);
        var headcounts = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySchoolId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getAllLevels());
    }

    @Test
    void testGetIndigenousHeadcountsBySchoolId_givenEligibleAndNonIndigenousStudents_shouldHaveCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two indegenous program students, one with non-eligibility reason
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setIndigenousSupportProgramNonEligReasonCode("NONELIG");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "29");
        setEnrolledProgramCode(students.get(0), "33");
        setEnrolledProgramCode(students.get(1), "36");

        var headcounts = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySchoolId(schoolCollectionId);

        //then only count the eligible student
        assertEquals("0", headcounts.get(0).getIndigenousLanguageTotal());
        assertEquals("0", headcounts.get(0).getIndigenousSupportTotal());
        assertEquals("1", headcounts.get(0).getOtherProgramTotal());
        assertEquals("1", headcounts.get(0).getAllSupportProgramTotal());
    }

    @Test
    void testGetIndigenousHeadcountsBySchoolId_givenStudentHasMultipleIndigenousPrograms_shouldHaveCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given one student with three indigenous program codes
        var students = getSdcStudentEntities(schoolCollection, 1);

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "29");
        setEnrolledProgramCode(students.get(0), "33");
        setEnrolledProgramCode(students.get(0), "36");

        var headcounts = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySchoolId(schoolCollectionId);

        //then have one count for each program type, but all support programs should only count the one student (this might change)
        assertEquals("1", headcounts.get(0).getIndigenousLanguageTotal());
        assertEquals("1", headcounts.get(0).getIndigenousSupportTotal());
        assertEquals("1", headcounts.get(0).getOtherProgramTotal());
        assertEquals("1", headcounts.get(0).getAllSupportProgramTotal());
    }

    @Test
    void testGetIndigenousHeadcountsBySchoolId_givenStudentInErrorStatus_shouldNotReturnInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID(), UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();

        //given two indigenous program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "33");
        setEnrolledProgramCode(students.get(1), "36");

        var headcounts = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySchoolId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getAllSupportProgramTotal());
    }

    private List<SdcSchoolCollectionStudentEntity> getSdcStudentEntities(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, int numStudents) {
        List<SdcSchoolCollectionStudentEntity> students = new ArrayList<>();
        for (int i = 0; i < numStudents; i++) {
            SdcSchoolCollectionStudentEntity student = createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
            students.add(student);
        }
        return students;
    }

    private void setEnrolledProgramCode(SdcSchoolCollectionStudentEntity studentEntity, String enrolledProgram) {
        var enrolledProgramEntity = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProgramEntity.setSdcSchoolCollectionStudentEntity(studentEntity);
        enrolledProgramEntity.setEnrolledProgramCode(enrolledProgram);
        enrolledProgramEntity.setCreateUser("ABC");
        enrolledProgramEntity.setUpdateUser("ABC");
        enrolledProgramEntity.setCreateDate(LocalDateTime.now());
        enrolledProgramEntity.setUpdateDate(LocalDateTime.now());
        enrolledProgramRepository.save(enrolledProgramEntity);
    }
}
