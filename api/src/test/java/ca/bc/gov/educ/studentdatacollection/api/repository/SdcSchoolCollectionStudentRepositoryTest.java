package ca.bc.gov.educ.studentdatacollection.api.repository;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentEnrolledProgramRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcStudentEllRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EllStudentResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(classes = {StudentDataCollectionApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SdcSchoolCollectionStudentRepositoryTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    @Autowired
    private SdcSchoolCollectionStudentEnrolledProgramRepository enrolledProgramRepository;
    @Autowired
    private SdcStudentEllRepository sdcStudentEllRepository;
    private UUID schoolCollectionId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCareerHeadcountsBySchoolId_givenMultipleEnrolledCodesAndInvalidStudents_shouldIncludeCorrectAndDistinctStudentInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
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
        assertEquals("0", headcounts.get(0).getPreparationXA());
        assertEquals("1", headcounts.get(0).getPreparationXB());
        assertEquals("1", headcounts.get(0).getCoopXC());
        assertEquals("1", headcounts.get(0).getCoopXD());
        assertEquals("1", headcounts.get(0).getApprenticeXE());
        assertEquals("1", headcounts.get(0).getApprenticeXF());
        assertEquals("1", headcounts.get(0).getTechYouthXG());
        assertEquals("1", headcounts.get(0).getTechYouthXH());
        assertEquals("0", headcounts.get(0).getAllXA());
        assertEquals("1", headcounts.get(0).getAllXB());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getAllXC());
        assertEquals("1", headcounts.get(0).getPreparationTotal());
        assertEquals("2", headcounts.get(0).getCoopTotal());
        assertEquals("2", headcounts.get(0).getApprenticeTotal());
        assertEquals("2", headcounts.get(0).getTechYouthTotal());
        assertEquals("7", headcounts.get(0).getAllTotal());
    }

    @Test
    void testGetCareerHeadcountsBySchoolId_givenStudentInErrorStatus_shouldNotReturnInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
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
        assertEquals("0", headcounts.get(0).getAllTotal());
    }

    @Test
    void testGetFrenchHeadcountsBySchoolId_givenEligibleAndNonEligibleFrenchStudents_shouldHaveCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
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

        var headcounts = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
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
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
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

        var headcounts = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
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
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two french program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "14");
        setEnrolledProgramCode(students.get(1), "14");

        var headcounts = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getTotalTotals());
    }

    @Test
    void testGetCsfFrenchHeadcountsBySchoolId_givenEligibleAndNonEligibleCsfFrenchStudents_shouldHaveCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given 2 csf french students, one with ineligibility reason, and 1 non csf french student
        var students = getSdcStudentEntities(schoolCollection, 3);
        students.get(0).setFrenchProgramNonEligReasonCode("NONELIG");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "05");
        setEnrolledProgramCode(students.get(1), "41");
        setEnrolledProgramCode(students.get(2), "05");

        var headcounts = sdcSchoolCollectionStudentRepository.getCsfFrenchHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
        //then only the one eligible csf french student is included in the headcounts
        assertEquals("1", headcounts.get(0).getSchoolAgedFrancophone());
        assertEquals("0", headcounts.get(0).getAdultFrancophone());
        assertEquals("1", headcounts.get(0).getTotalFrancophone());
    }

    @Test
    void testGetCsfFrenchHeadcountsBySchoolId_givenNonSchoolOrAdultAgedStudents_shouldIncludeInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given 1 csf french student that is neither adult nor student aged
        var students = getSdcStudentEntities(schoolCollection, 1);
        students.get(0).setIsSchoolAged(false);

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "05");

        var headcounts = sdcSchoolCollectionStudentRepository.getCsfFrenchHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
        //then headcount is only included in the total column
        assertEquals("0", headcounts.get(0).getSchoolAgedFrancophone());
        assertEquals("0", headcounts.get(0).getAdultFrancophone());
        assertEquals("1", headcounts.get(0).getTotalFrancophone());
    }

    @Test
    void testGetCsfFrenchHeadcountsBySchoolId_givenStudentInErrorStatus_shouldNotReturnInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two csf french program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "05");
        setEnrolledProgramCode(students.get(1), "05");

        var headcounts = sdcSchoolCollectionStudentRepository.getCsfFrenchHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getTotalFrancophone());
    }

    @Test
    void testGetEllHeadcountsBySchoolId_givenEligibleAndNonEligibleEllStudents_shouldReturnCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
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

        var headcounts = sdcSchoolCollectionStudentRepository.getEllHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
        //then only the eligible ell student is included in headcounts
        assertEquals("1", headcounts.get(0).getTotalEligibleEllStudents());
        assertEquals("2", headcounts.get(0).getTotalEllStudents());
    }

    @Test
    void testGetEllHeadcountsBySchoolId_givenStudentInErrorStatus_shouldNotReturnInHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two ell program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "17");
        setEnrolledProgramCode(students.get(1), "17");

        var headcounts = sdcSchoolCollectionStudentRepository.getEllHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getTotalEllStudents());
    }

    @Test
    void testGetSpecialEdHeadcountsBySchoolId_givenEligibleAndNonEligibleSpecialEdStudents_shouldReturnCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
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
        var headcounts = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
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
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two special ed program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");
        students.get(0).setSpecialEducationCategoryCode("A");
        students.get(1).setSpecialEducationCategoryCode("A");

        sdcSchoolCollectionStudentRepository.saveAll(students);
        var headcounts = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getAllLevels());
    }

    @Test
    void testGetIndigenousHeadcountsBySchoolId_givenEligibleAndNonIndigenousStudents_shouldHaveCorrectHeadcounts() {

        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given two indegenous program students, one with non-eligibility reason
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setIndigenousSupportProgramNonEligReasonCode("NONELIG");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "29");
        setEnrolledProgramCode(students.get(0), "33");
        setEnrolledProgramCode(students.get(1), "36");

        var headcounts = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcSchoolCollectionId(schoolCollectionId);

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
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();
        //given one student with three indigenous program codes
        var students = getSdcStudentEntities(schoolCollection, 1);

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "29");
        setEnrolledProgramCode(students.get(0), "33");
        setEnrolledProgramCode(students.get(0), "36");

        var headcounts = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcSchoolCollectionId(schoolCollectionId);

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
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);
        schoolCollectionId = schoolCollection.getSdcSchoolCollectionID();

        //given two indigenous program students with one in error status
        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");

        sdcSchoolCollectionStudentRepository.saveAll(students);

        setEnrolledProgramCode(students.get(0), "33");
        setEnrolledProgramCode(students.get(1), "36");

        var headcounts = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcSchoolCollectionId(schoolCollectionId);
        //then only non error student in headcounts
        assertEquals("1", headcounts.get(0).getAllSupportProgramTotal());
    }

    @Test
    void testGetEllStudentsByFallCollectionId_givenStudentsWithCurrentEllRecord_shouldReturnCurrentYearsInEll() {
        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);

        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setYearsInEll(4);
        students.get(0).setAssignedStudentId(UUID.randomUUID());
        students.get(0).setStudentPen("111222333");
        students.get(0).setLegalLastName("Smith");
        students.get(1).setYearsInEll(2);
        students.get(1).setAssignedStudentId(UUID.randomUUID());
        students.get(1).setStudentPen("444555666");
        students.get(1).setLegalLastName("Jones");
        sdcSchoolCollectionStudentRepository.saveAll(students);
        setEnrolledProgramCode(students.get(0), "17");
        setEnrolledProgramCode(students.get(1), "17");

        SdcStudentEllEntity ellRecord0 = createMockStudentEllEntity(students.get(0));
        ellRecord0.setYearsInEll(5);
        sdcStudentEllRepository.save(ellRecord0);

        SdcStudentEllEntity ellRecord1 = createMockStudentEllEntity(students.get(1));
        ellRecord1.setYearsInEll(3);
        sdcStudentEllRepository.save(ellRecord1);

        List<EllStudentResult> results = sdcSchoolCollectionStudentRepository
                .getEllStudentsByFallCollectionId(collection.getCollectionID());

        assertEquals(2, results.size());
        var smithResult = results.stream().filter(r -> "111222333".equals(r.getStudentPen())).findFirst().orElseThrow();
        var jonesResult = results.stream().filter(r -> "444555666".equals(r.getStudentPen())).findFirst().orElseThrow();

        assertEquals("5", smithResult.getYearsInEll());
        assertEquals("3", jonesResult.getYearsInEll());
        assertEquals("Smith", smithResult.getLegalLastName());
        assertEquals("Jones", jonesResult.getLegalLastName());
    }

    @Test
    void testGetEllStudentsByFallCollectionId_givenStudentWithNoCurrentEllRecord_shouldFallBackToSnapshotYearsInEll() {
        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);

        var students = getSdcStudentEntities(schoolCollection, 1);
        students.get(0).setYearsInEll(3);
        students.get(0).setAssignedStudentId(UUID.randomUUID());
        students.get(0).setStudentPen("777888999");
        students.get(0).setLegalLastName("Williams");
        sdcSchoolCollectionStudentRepository.saveAll(students);
        setEnrolledProgramCode(students.get(0), "17");

        List<EllStudentResult> results = sdcSchoolCollectionStudentRepository
                .getEllStudentsByFallCollectionId(collection.getCollectionID());

        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getYearsInEll());
        assertEquals("777888999", results.get(0).getStudentPen());
    }

    @Test
    void testGetEllStudentsByFallCollectionId_givenMixedStudents_shouldReturnCurrentEllWhereAvailableAndFallbackOtherwise() {
        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);

        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setYearsInEll(5);
        students.get(0).setAssignedStudentId(UUID.randomUUID());
        students.get(0).setStudentPen("111000111");
        students.get(0).setLegalLastName("WithEll");
        students.get(1).setYearsInEll(2);
        students.get(1).setAssignedStudentId(UUID.randomUUID());
        students.get(1).setStudentPen("222000222");
        students.get(1).setLegalLastName("NoEll");
        sdcSchoolCollectionStudentRepository.saveAll(students);
        setEnrolledProgramCode(students.get(0), "17");
        setEnrolledProgramCode(students.get(1), "17");

        SdcStudentEllEntity ellRecord = createMockStudentEllEntity(students.get(0));
        ellRecord.setYearsInEll(6);
        sdcStudentEllRepository.save(ellRecord);

        List<EllStudentResult> results = sdcSchoolCollectionStudentRepository
                .getEllStudentsByFallCollectionId(collection.getCollectionID());

        assertEquals(2, results.size());
        var withEllResult = results.stream().filter(r -> "111000111".equals(r.getStudentPen())).findFirst().orElseThrow();
        var noEllResult = results.stream().filter(r -> "222000222".equals(r.getStudentPen())).findFirst().orElseThrow();

        assertEquals("6", withEllResult.getYearsInEll());
        assertEquals("2", noEllResult.getYearsInEll());
    }

    @Test
    void testGetEllStudentsByFallCollectionId_givenStudentWithZeroYearsInEll_shouldNotBeReturned() {
        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);

        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setYearsInEll(0);
        students.get(0).setAssignedStudentId(UUID.randomUUID());
        students.get(1).setYearsInEll(1);
        students.get(1).setAssignedStudentId(UUID.randomUUID());
        sdcSchoolCollectionStudentRepository.saveAll(students);
        setEnrolledProgramCode(students.get(0), "17");
        setEnrolledProgramCode(students.get(1), "17");

        List<EllStudentResult> results = sdcSchoolCollectionStudentRepository
                .getEllStudentsByFallCollectionId(collection.getCollectionID());

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getYearsInEll());
    }

    @Test
    void testGetEllStudentsByFallCollectionId_givenErrorStatusStudent_shouldNotBeReturned() {
        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);

        var students = getSdcStudentEntities(schoolCollection, 2);
        students.get(0).setYearsInEll(3);
        students.get(0).setAssignedStudentId(UUID.randomUUID());
        students.get(0).setSdcSchoolCollectionStudentStatusCode("ERROR");
        students.get(1).setYearsInEll(3);
        students.get(1).setAssignedStudentId(UUID.randomUUID());
        sdcSchoolCollectionStudentRepository.saveAll(students);
        setEnrolledProgramCode(students.get(0), "17");
        setEnrolledProgramCode(students.get(1), "17");

        List<EllStudentResult> results = sdcSchoolCollectionStudentRepository
                .getEllStudentsByFallCollectionId(collection.getCollectionID());

        assertEquals(1, results.size());
    }

    @Test
    void testGetEllStudentsByFallCollectionId_includesSpecificEllNonEligibleCodesWithoutRequiringProgramCode17() {
        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);

        var eligibleStudent = createMockSchoolStudentEntity(schoolCollection);
        eligibleStudent.setStudentPen("100000001");
        eligibleStudent.setLegalLastName("Eligible");
        eligibleStudent.setYearsInEll(3);
        eligibleStudent.setAssignedStudentId(UUID.randomUUID());

        var indyStudent = createMockSchoolStudentEntity(schoolCollection);
        indyStudent.setStudentPen("100000002");
        indyStudent.setLegalLastName("IndyIncluded");
        indyStudent.setYearsInEll(4);
        indyStudent.setAssignedStudentId(UUID.randomUUID());
        indyStudent.setEllNonEligReasonCode(ProgramEligibilityIssueCode.ELL_INDY_SCHOOL.getCode());

        var fiveYearStudent = createMockSchoolStudentEntity(schoolCollection);
        fiveYearStudent.setStudentPen("100000003");
        fiveYearStudent.setLegalLastName("FiveYearIncluded");
        fiveYearStudent.setYearsInEll(5);
        fiveYearStudent.setAssignedStudentId(UUID.randomUUID());
        fiveYearStudent.setEllNonEligReasonCode(ProgramEligibilityIssueCode.YEARS_IN_ELL.getCode());

        var otherEllNonEligibleStudent = createMockSchoolStudentEntity(schoolCollection);
        otherEllNonEligibleStudent.setStudentPen("100000004");
        otherEllNonEligibleStudent.setLegalLastName("OtherExcluded");
        otherEllNonEligibleStudent.setYearsInEll(6);
        otherEllNonEligibleStudent.setAssignedStudentId(UUID.randomUUID());
        otherEllNonEligibleStudent.setEllNonEligReasonCode(ProgramEligibilityIssueCode.ZERO_COURSES_ADULT_ELL.getCode());

        var noProgramStudent = createMockSchoolStudentEntity(schoolCollection);
        noProgramStudent.setStudentPen("100000005");
        noProgramStudent.setLegalLastName("NoProgramIncluded");
        noProgramStudent.setYearsInEll(7);
        noProgramStudent.setAssignedStudentId(UUID.randomUUID());

        sdcSchoolCollectionStudentRepository.saveAll(List.of(
                eligibleStudent,
                indyStudent,
                fiveYearStudent,
                otherEllNonEligibleStudent,
                noProgramStudent
        ));

        setEnrolledProgramCode(eligibleStudent, "17");
        setEnrolledProgramCode(indyStudent, "17");
        setEnrolledProgramCode(fiveYearStudent, "17");
        setEnrolledProgramCode(otherEllNonEligibleStudent, "17");

        SdcStudentEllEntity ellRecord = createMockStudentEllEntity(eligibleStudent);
        ellRecord.setYearsInEll(8);
        sdcStudentEllRepository.save(ellRecord);

        List<EllStudentResult> results = sdcSchoolCollectionStudentRepository
                .getEllStudentsByFallCollectionId(collection.getCollectionID());

        Set<String> lastNames = results.stream().map(EllStudentResult::getLegalLastName).collect(Collectors.toSet());

        assertEquals(4, results.size());
        assertTrue(lastNames.contains("Eligible"));
        assertTrue(lastNames.contains("IndyIncluded"));
        assertTrue(lastNames.contains("FiveYearIncluded"));
        assertTrue(lastNames.contains("NoProgramIncluded"));
        assertFalse(lastNames.contains("OtherExcluded"));
        assertEquals("8", results.stream()
                .filter(result -> "100000001".equals(result.getStudentPen()))
                .findFirst()
                .orElseThrow()
                .getYearsInEll());
    }

    @Test
    @Transactional
    void testStreamEllStudentsByFallCollectionId_includesSpecificEllNonEligibleCodesWithoutRequiringProgramCode17() {
        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var schoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
        sdcSchoolCollectionRepository.save(schoolCollection);

        var eligibleStudent = createMockSchoolStudentEntity(schoolCollection);
        eligibleStudent.setStudentPen("200000001");
        eligibleStudent.setLegalLastName("EligibleStream");
        eligibleStudent.setYearsInEll(3);
        eligibleStudent.setAssignedStudentId(UUID.randomUUID());

        var indyStudent = createMockSchoolStudentEntity(schoolCollection);
        indyStudent.setStudentPen("200000002");
        indyStudent.setLegalLastName("IndyStream");
        indyStudent.setYearsInEll(4);
        indyStudent.setAssignedStudentId(UUID.randomUUID());
        indyStudent.setEllNonEligReasonCode(ProgramEligibilityIssueCode.ELL_INDY_SCHOOL.getCode());

        var excludedStudent = createMockSchoolStudentEntity(schoolCollection);
        excludedStudent.setStudentPen("200000003");
        excludedStudent.setLegalLastName("ExcludedStream");
        excludedStudent.setYearsInEll(5);
        excludedStudent.setAssignedStudentId(UUID.randomUUID());
        excludedStudent.setEllNonEligReasonCode(ProgramEligibilityIssueCode.ZERO_COURSES_ADULT_ELL.getCode());

        var noProgramStudent = createMockSchoolStudentEntity(schoolCollection);
        noProgramStudent.setStudentPen("200000004");
        noProgramStudent.setLegalLastName("NoProgramStream");
        noProgramStudent.setYearsInEll(6);
        noProgramStudent.setAssignedStudentId(UUID.randomUUID());

        sdcSchoolCollectionStudentRepository.saveAll(List.of(eligibleStudent, indyStudent, excludedStudent, noProgramStudent));

        setEnrolledProgramCode(eligibleStudent, "17");
        setEnrolledProgramCode(indyStudent, "17");
        setEnrolledProgramCode(excludedStudent, "17");

        List<EllStudentResult> results;
        try (var resultStream = sdcSchoolCollectionStudentRepository.streamEllStudentsByFallCollectionId(collection.getCollectionID())) {
            results = resultStream.toList();
        }

        Set<String> lastNames = results.stream().map(EllStudentResult::getLegalLastName).collect(Collectors.toSet());

        assertEquals(3, results.size());
        assertTrue(lastNames.contains("EligibleStream"));
        assertTrue(lastNames.contains("IndyStream"));
        assertTrue(lastNames.contains("NoProgramStream"));
        assertFalse(lastNames.contains("ExcludedStream"));
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
