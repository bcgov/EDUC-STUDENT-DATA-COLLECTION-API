package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FteCalculatorUtilsTest {

    @Mock
    private SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    @Mock
    private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    @Mock
    private RestUtils restUtils;

    @InjectMocks
    private FteCalculatorUtils fteCalculatorUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIsSpringCollectionForFebruaryCollection() {
        // Arrange
        StudentRuleData studentRuleData = new StudentRuleData();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);

        // Act
        var result = fteCalculatorUtils.isSpringCollection(studentRuleData);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsSpringCollectionForMayCollection() {
        // Arrange
        StudentRuleData studentRuleData = new StudentRuleData();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.MAY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        // Act
        var result = fteCalculatorUtils.isSpringCollection(studentRuleData);

        // Assert
        assertTrue(result);
    }
    @Test
    void studentPreviouslyReportedInDistrict_NoAssignedStudentId_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("PUBLIC");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(studentRuleData);

        // Then
        assertFalse(result);
    }
    @Test
    void studentPreviouslyReportedInDistrict_NoPreviousCollectionsForSchools_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("PUBLIC");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setDistrictId(UUID.randomUUID().toString());
        school.setSchoolId(UUID.randomUUID().toString());
        studentRuleData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        var collection = createMockCollectionEntity();
        var sdcCollection = createMockSdcSchoolCollectionEntity(collection, null, null);
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        student.setSdcSchoolCollection(sdcCollection);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInDistrict_NoPreviousCollectionsForStudent_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("PUBLIC");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setDistrictId(UUID.randomUUID().toString());
        school.setSchoolId(UUID.randomUUID().toString());
        studentRuleData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(0L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInDistrict_NullDistrictID_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("PUBLIC");
        school.setFacilityTypeCode("DIST_LEARN");
        studentRuleData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInDistrict_StudentHadPreviousCourse_ReturnsTrue() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("PUBLIC");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setDistrictId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(1L);


        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(sdcStudentSagaData);

        // Then
        assertTrue(result);
    }

    @ParameterizedTest
    @CsvSource({
            "HS, true",
            "KH, true",
            "KF, true",
            "01, true",
            "02, true",
            "03, true",
            "04, true",
            "05, true",
            "06, true",
            "07, true",
            "EU, true",
            "08, true",
            "09, true",
            "10, false",
            "11, false",
            "12, false",
            "SU, false",
            "GA, false"
    })
    void studentPreviouslyReportedInDistrict_GivenDifferentGrades_ReturnExpectedResult(String enrolledGradeCode, boolean expectedResult) {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("PUBLIC");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setDistrictId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(sdcStudentSagaData);

        // Then
        assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @CsvSource({
            "PUBLIC, DIST_LEARN, true",
            "PUBLIC, DISTONLINE, true",
            "PUBLIC, STANDARD, false",
            "PUBLIC, CONT_ED, true",
            "INDEPEND, DIST_LEARN, false",
            "INDEPEND, DISTONLINE, false",
            "INDEPEND, STANDARD, false",
            "PUBLIC, CONT_ED, true",
    })
    void studentPreviouslyReportedInDistrict_GivenDifferentSchoolCategoriesAndFacilities_ReturnExpectedResult(String schoolCategory, String facilityType, boolean expectedResult) {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);
        school.setFacilityTypeCode(facilityType);
        school.setDistrictId(UUID.randomUUID().toString());
        school.setSchoolId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(sdcStudentSagaData);

        // Then
        assertEquals(expectedResult, result);

    }
    @Test
    void studentPreviouslyReportedInIndependentAuthority_NoAssignedStudentId_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("INDEPEND");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_NoPreviousCollectionsForSchools_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("INDEPEND");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        var collection = createMockCollectionEntity();
        var sdcCollection = createMockSdcSchoolCollectionEntity(collection, null, null);
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        student.setSdcSchoolCollection(sdcCollection);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_NoPreviousCollectionsForStudent_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("INDEPEND");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(school);
        var collection = createMockCollectionEntity();
        var sdcCollection = createMockSdcSchoolCollectionEntity(collection, null, null);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());

        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        student.setSdcSchoolCollection(sdcCollection);
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(0L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_NullIndependentAuthority_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("INDEPEND");
        school.setFacilityTypeCode("DIST_LEARN");
        sdcStudentSagaData.setSchool(school);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_StudentHadPreviousCourse_ReturnsTrue() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("INDEPEND");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertTrue(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_NoSchoolIdsFound_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode("INDEPEND");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.empty());
        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @ParameterizedTest
    @CsvSource({
            "HS, true",
            "KH, true",
            "KF, true",
            "01, true",
            "02, true",
            "03, true",
            "04, true",
            "05, true",
            "06, true",
            "07, true",
            "EU, true",
            "08, true",
            "09, true",
            "10, false",
            "11, false",
            "12, false",
            "SU, false",
            "GA, false"
    })
    void studentPreviouslyReportedInIndependentAuthority_GivenDifferentGrades_ReturnExpectedResult(String enrolledGradeCode, boolean expectedResult) {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolId(UUID.randomUUID().toString());
        school.setSchoolCategoryCode("INDEPEND");
        school.setFacilityTypeCode("DIST_LEARN");
        school.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @CsvSource({
            "INDEPEND, DIST_LEARN, true",
            "INDEPEND, STANDARD, false",
            "OFFSHORE, DIST_LEARN, false"
    })
    void studentPreviouslyReportedInIndependentAuthority_GivenDifferentSchoolCategoriesAndFacilities_ReturnExpectedResult(String schoolCategory, String facilityType, boolean expectedResult) {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);
        school.setFacilityTypeCode(facilityType);
        school.setIndependentAuthorityId("AUTH_ID");
        school.setSchoolId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertEquals(expectedResult, result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_SchoolIsNull_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), anyList())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @ParameterizedTest
    @CsvSource({
            "HS, false",
            "KH, true",
            "KF, true",
            "01, true",
            "02, true",
            "03, true",
            "04, true",
            "05, true",
            "06, true",
            "07, true",
            "EU, true",
            "08, true",
            "09, true",
            "10, false",
            "11, false",
            "12, false",
            "SU, false",
            "GA, false"
    })
    void testHomeSchoolStudentIsNowOnlineKto9Student_GivenDifferentGrades_ReturnExpectedResult(String enrolledGradeCode, boolean expectedResult) {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setFacilityTypeCode("DIST_LEARN");
        school.setSchoolId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), any(String.class), any())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9Student(sdcStudentSagaData);

        // Then
        assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @CsvSource({
            "FEBRUARY, DIST_LEARN, true",
            "FEBRUARY, STANDARD, false",
            "FEBRUARY, DISTONLINE, true",
            "SEPTEMBER, DIST_LEARN, false",
            "SEPTEMBER, STANDARD, false",
            "SEPTEMBER, DISTONLINE, false",
            "MAY, DIST_LEARN, true",
            "MAY, STANDARD, false",
            "MAY, DISTONLINE, true",
            "JULY, DIST_LEARN, false",
            "JULY, STANDARD, false",
            "JULY, DISTONLINE, false",
    })
    void testHomeSchoolStudentIsNowOnlineKto9Student_GivenDifferentSchoolCategoriesAndFacilities_ReturnExpectedResult(String collectionType, String facilityType, boolean expectedResult) {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setFacilityTypeCode(facilityType);
        school.setSchoolId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(collectionType);
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), any(String.class), any())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9Student(sdcStudentSagaData);

        // Then
        assertEquals(expectedResult, result);
    }
    @Test
    void testHomeSchoolStudentIsNowOnlineKto9Student_GivenNoAssignedStudentId_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setFacilityTypeCode("DIST_LEARN");
        school.setSchoolId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        // When
        var result = fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9Student(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }
    @Test
    void testHomeSchoolStudentIsNowOnlineKto9Student_GivenNoPreviousCollectionForStudent_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setSchoolId(UUID.randomUUID().toString());
        school.setFacilityTypeCode("DIST_LEARN");
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        var collection = createMockCollectionEntity();
        var sdcCollection = createMockSdcSchoolCollectionEntity(collection, null, null);
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        student.setSdcSchoolCollection(sdcCollection);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(any(UUID.class), any(String.class), any())).thenReturn(0L);

        // When
        var result = fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9Student(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void noCoursesForStudentInLastTwoYears_NotSchoolAged_ShouldReturnFalse() {
        // Given
        UUID schoolId = UUID.randomUUID();
        LocalDateTime studentCreateDate = LocalDateTime.now();
        String enrolledGradeCode = "10";
        String facilityTypeCode = "DIST_LEARN";
        String numberOfCourses = "0";

        School school = new School();
        school.setSchoolId(UUID.randomUUID().toString());
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionEntity schoolCollection1 = new SdcSchoolCollectionEntity();
        schoolCollection1.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection1.setSchoolID(schoolId);
        schoolCollection1.setCreateDate(studentCreateDate.minusYears(1)); // One year ago

        SdcSchoolCollectionEntity schoolCollection2 = new SdcSchoolCollectionEntity();
        schoolCollection2.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection2.setSchoolID(schoolId);
        schoolCollection2.setCreateDate(studentCreateDate.minusYears(2)); // Two years ago

        List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = Arrays.asList(schoolCollection1, schoolCollection2);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setCreateDate(studentCreateDate);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses(numberOfCourses);
        student.setAssignedStudentId(UUID.randomUUID());
        student.setIsSchoolAged(false);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(any(UUID.class), anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForStudentInLastTwoYears(studentData);

        // Then
        assertFalse(result);
    }

    @Test
    void noCoursesForStudentInLastTwoYears_NoCoursesInLastTwoYears_ShouldReturnTrue() {
        // Given
        UUID schoolId = UUID.randomUUID();
        LocalDateTime studentCreateDate = LocalDateTime.now();
        String enrolledGradeCode = "10";
        String facilityTypeCode = "DIST_LEARN";
        String numberOfCourses = "0000";

        School school = new School();
        school.setSchoolId(UUID.randomUUID().toString());
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionEntity schoolCollection1 = new SdcSchoolCollectionEntity();
        schoolCollection1.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection1.setSchoolID(schoolId);
        schoolCollection1.setCreateDate(studentCreateDate.minusYears(1)); // One year ago

        SdcSchoolCollectionEntity schoolCollection2 = new SdcSchoolCollectionEntity();
        schoolCollection2.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection2.setSchoolID(schoolId);
        schoolCollection2.setCreateDate(studentCreateDate.minusYears(2)); // Two years ago

        List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = Arrays.asList(schoolCollection1, schoolCollection2);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setCreateDate(studentCreateDate);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses(numberOfCourses);
        student.setAssignedStudentId(UUID.randomUUID());
        student.setIsSchoolAged(true);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setSdcSchoolCollection(schoolCollection1);

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(any(UUID.class), anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForStudentInLastTwoYears(studentData);

        // Then
        assertTrue(result);
    }

    @Test
    void noCoursesForStudentInLastTwoYears_OneCourseInLastTwoYears_ReturnsFalse() {
        // Given
        UUID schoolId = UUID.randomUUID();
        LocalDateTime studentCreateDate = LocalDateTime.now();
        String enrolledGradeCode = "10";
        String facilityTypeCode = "DIST_LEARN";
        String numberOfCourses = "0000";

        School school = new School();
        school.setSchoolId(schoolId.toString());
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionEntity schoolCollection1 = new SdcSchoolCollectionEntity();
        schoolCollection1.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection1.setSchoolID(schoolId);
        schoolCollection1.setCreateDate(studentCreateDate.minusYears(1)); // One year ago

        SdcSchoolCollectionEntity schoolCollection2 = new SdcSchoolCollectionEntity();
        schoolCollection2.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection2.setSchoolID(schoolId);
        schoolCollection2.setCreateDate(studentCreateDate.minusYears(2)); // Two years ago

        List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = Arrays.asList(schoolCollection1, schoolCollection2);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setCreateDate(studentCreateDate);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses(numberOfCourses);
        student.setAssignedStudentId(UUID.randomUUID());
        student.setIsSchoolAged(true);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setSdcSchoolCollection(schoolCollection1);

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(any(UUID.class), anyList(), any(String.class)))
                .thenReturn(1L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForStudentInLastTwoYears(studentData);

        // Then
        assertFalse(result);
    }
    @Test
    void noCoursesForStudentInLastTwoYears_NoAssignedStudentId_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        School school = new School();
        school.setFacilityTypeCode("DIST_LEARN");
        sdcStudentSagaData.setSchool(school);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        // When
        boolean result = fteCalculatorUtils.noCoursesForStudentInLastTwoYears(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }
    @Test
    void noCoursesForStudentInLastTwoYears_NoCollectionsInLastTwoYears_ReturnsTrue() {
        // Given
        UUID schoolId = UUID.randomUUID();
        LocalDateTime studentCreateDate = LocalDateTime.now();
        String enrolledGradeCode = "10";
        String facilityTypeCode = "DIST_LEARN";
        String numberOfCourses = "0000";

        School school = new School();
        school.setSchoolId(schoolId.toString());
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionEntity schoolCollection1 = new SdcSchoolCollectionEntity();
        schoolCollection1.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection1.setSchoolID(schoolId);
        schoolCollection1.setCreateDate(studentCreateDate.minusYears(1)); // One year ago

        SdcSchoolCollectionEntity schoolCollection2 = new SdcSchoolCollectionEntity();
        schoolCollection2.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection2.setSchoolID(schoolId);
        schoolCollection2.setCreateDate(studentCreateDate.minusYears(2)); // Two years ago

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setCreateDate(studentCreateDate);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses(numberOfCourses);
        student.setAssignedStudentId(UUID.randomUUID());
        student.setIsSchoolAged(true);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setSdcSchoolCollection(schoolCollection1);

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(List.of());
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(any(UUID.class), anyList(), any(String.class)))
                .thenReturn(1L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForStudentInLastTwoYears(studentData);

        // Then
        assertTrue(result);
    }

    @ParameterizedTest
    @CsvSource({
            "DIST_LEARN, 0000, true",
            "DISTONLINE, 0000, true",
            "DIST_LEARN, 0300, false",
            "DISTONLINE, 0100, false",
            "STANDARD, 0000, false",
            "STANDARD, 0300, false",
    })
    void noCoursesForStudentInLastTwoYears_GivenDifferentFacilityTypesAndNumCourses_ShouldReturnTrue(String facilityTypeCode, String numberOfCourses, boolean expectedResult) {
        // Given
        UUID schoolId = UUID.randomUUID();
        LocalDateTime studentCreateDate = LocalDateTime.now();

        School school = new School();
        school.setSchoolId(UUID.randomUUID().toString());
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionEntity schoolCollection1 = new SdcSchoolCollectionEntity();
        schoolCollection1.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection1.setSchoolID(schoolId);
        schoolCollection1.setCreateDate(studentCreateDate.minusYears(1)); // One year ago

        SdcSchoolCollectionEntity schoolCollection2 = new SdcSchoolCollectionEntity();
        schoolCollection2.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection2.setSchoolID(schoolId);
        schoolCollection2.setCreateDate(studentCreateDate.minusYears(2)); // Two years ago

        List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = Arrays.asList(schoolCollection1, schoolCollection2);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setCreateDate(studentCreateDate);
        student.setEnrolledGradeCode("10");
        student.setNumberOfCourses(numberOfCourses);
        student.setAssignedStudentId(UUID.randomUUID());
        student.setIsSchoolAged(true);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setSdcSchoolCollection(schoolCollection1);

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(any(UUID.class), anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForStudentInLastTwoYears(studentData);

        // Then
        assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @CsvSource({
            "HS, false",
            "KH, false",
            "KF, false",
            "01, false",
            "02, false",
            "03, false",
            "04, false",
            "05, false",
            "06, false",
            "07, false",
            "EU, false",
            "08, true",
            "09, true",
            "10, true",
            "11, true",
            "12, true",
            "SU, true",
            "GA, true"
    })
    void noCoursesForStudentInLastTwoYears_GivenAllGrades_ShouldReturnTrue(String enrolledGradeCode, boolean expectedResult) {
        // Given
        UUID schoolId = UUID.randomUUID();
        LocalDateTime studentCreateDate = LocalDateTime.now();
        String facilityTypeCode = "DIST_LEARN";
        String numberOfCourses = "0000";

        School school = new School();
        school.setSchoolId(UUID.randomUUID().toString());
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionEntity schoolCollection1 = new SdcSchoolCollectionEntity();
        schoolCollection1.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection1.setSchoolID(schoolId);
        schoolCollection1.setCreateDate(studentCreateDate.minusYears(1)); // One year ago

        SdcSchoolCollectionEntity schoolCollection2 = new SdcSchoolCollectionEntity();
        schoolCollection2.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection2.setSchoolID(schoolId);
        schoolCollection2.setCreateDate(studentCreateDate.minusYears(2)); // Two years ago

        List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = Arrays.asList(schoolCollection1, schoolCollection2);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setCreateDate(studentCreateDate);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses(numberOfCourses);
        student.setAssignedStudentId(UUID.randomUUID());
        student.setIsSchoolAged(true);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setSdcSchoolCollection(schoolCollection1);

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(any(UUID.class), anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForStudentInLastTwoYears(studentData);

        // Then
        assertEquals(expectedResult, result);
    }

    @Test
    void noCoursesForStudentInLastTwoYears_NumberOfCoursesNull_ReturnsTrue() {
        // Given
        UUID schoolId = UUID.randomUUID();
        LocalDateTime studentCreateDate = LocalDateTime.now();
        String enrolledGradeCode = "10";
        String facilityTypeCode = "DIST_LEARN";

        School school = new School();
        school.setSchoolId(schoolId.toString());
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionEntity schoolCollection1 = new SdcSchoolCollectionEntity();
        schoolCollection1.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection1.setSchoolID(schoolId);
        schoolCollection1.setCreateDate(studentCreateDate.minusYears(1)); // One year ago

        SdcSchoolCollectionEntity schoolCollection2 = new SdcSchoolCollectionEntity();
        schoolCollection2.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection2.setSchoolID(schoolId);
        schoolCollection2.setCreateDate(studentCreateDate.minusYears(2)); // Two years ago

        List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = Arrays.asList(schoolCollection1, schoolCollection2);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setCreateDate(studentCreateDate);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setIsSchoolAged(true);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setAssignedStudentId(UUID.randomUUID());
        student.setSdcSchoolCollection(schoolCollection1);

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(any(UUID.class), anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForStudentInLastTwoYears(studentData);

        // Then
        assertTrue(result);
    }

    public CollectionEntity createMockCollectionEntity(){
        CollectionEntity sdcEntity = new CollectionEntity();
        sdcEntity.setCollectionTypeCode("SEPTEMBER");
        sdcEntity.setOpenDate(LocalDateTime.now());
        sdcEntity.setCloseDate(LocalDateTime.now().plusDays(5));
        sdcEntity.setSnapshotDate(LocalDate.of(sdcEntity.getOpenDate().getYear(), 9, 29));
        sdcEntity.setSubmissionDueDate(sdcEntity.getSnapshotDate().plusDays(3));
        sdcEntity.setDuplicationResolutionDueDate(sdcEntity.getSnapshotDate().plusDays(6));
        sdcEntity.setSignOffDueDate(sdcEntity.getSnapshotDate().plusDays(9));
        sdcEntity.setCreateUser("ABC");
        sdcEntity.setCreateDate(LocalDateTime.now());
        sdcEntity.setUpdateUser("ABC");
        sdcEntity.setUpdateDate(LocalDateTime.now());
        return sdcEntity;
    }

    public SdcSchoolCollectionEntity createMockSdcSchoolCollectionEntity(CollectionEntity entity, UUID schoolID, UUID districtID){
        SdcSchoolCollectionEntity sdcEntity = new SdcSchoolCollectionEntity();
        sdcEntity.setCollectionEntity(entity);
        sdcEntity.setSchoolID(schoolID == null ? UUID.randomUUID() : schoolID);
        sdcEntity.setDistrictID(districtID == null ? UUID.randomUUID() : districtID);
        sdcEntity.setUploadDate(LocalDateTime.now());
        sdcEntity.setUploadFileName("abc.txt");
        sdcEntity.setUploadReportDate(null);
        sdcEntity.setSdcSchoolCollectionStatusCode("NEW");
        sdcEntity.setCreateUser("ABC");
        sdcEntity.setCreateDate(LocalDateTime.now());
        sdcEntity.setUpdateUser("ABC");
        sdcEntity.setUpdateDate(LocalDateTime.now());

        return sdcEntity;
    }
}
