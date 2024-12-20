package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.StudentMerge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
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
    @Mock
    ValidationRulesService validationRulesService;

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
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
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
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
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
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
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
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        var collection = createMockCollectionEntity();
        var sdcCollection = createMockSdcSchoolCollectionEntity(collection, null);
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        student.setSdcSchoolCollection(sdcCollection);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

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
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(0L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(0L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInDistrict_NullDistrictID_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(1L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(1L);
        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInDistrict_StudentHadPreviousCourse_ReturnsTrue() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(1L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(sdcStudentSagaData);

        // Then
        assertTrue(result);
    }

    @Test
    void studentPreviouslyReportedInDistrict_StudentInSpringPrevSeptHS_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.HOMESCHOOL.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(0L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(0L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInDistrict_StudentInMayPrevFebHS_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.MAY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.HOMESCHOOL.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(0L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(0L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInDistrict_StudentInMayPrevFebNonZeroFTE_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.MAY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setFte(BigDecimal.ONE);
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(0L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(0L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(sdcStudentSagaData);

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
    void studentPreviouslyReportedInDistrict_GivenDifferentGrades_ReturnExpectedResult(String enrolledGradeCode, boolean expectedResult) {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(1L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(1L);
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
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode(schoolCategory);
        schoolTombstone.setFacilityTypeCode(facilityType);
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(1L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(1L);
        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInDistrict(sdcStudentSagaData);

        // Then
        assertEquals(expectedResult, result);

    }
    @Test
    void studentPreviouslyReportedInIndependentAuthority_NoAssignedStudentId_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("INDEPEND");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(schoolTombstone);
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
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("INDEPEND");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        var collection = createMockCollectionEntity();
        var sdcCollection = createMockSdcSchoolCollectionEntity(collection, null);
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        student.setSdcSchoolCollection(sdcCollection);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_NoPreviousCollectionsForStudent_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("INDEPEND");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(schoolTombstone);
        var collection = createMockCollectionEntity();
        var sdcCollection = createMockSdcSchoolCollectionEntity(collection, null);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());

        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        student.setSdcSchoolCollection(sdcCollection);
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(0L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(0L);
        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_NullIndependentAuthority_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("INDEPEND");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        sdcStudentSagaData.setSchool(schoolTombstone);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(1L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(1L);
        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_StudentHadPreviousCourse_ReturnsTrue() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("INDEPEND");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(1L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(1L);
        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertTrue(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_StudentInSpringPrevSeptHS_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("INDEPEND");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.HOMESCHOOL.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(0L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(0L);

        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_StudentInMayPrevFebHS_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("INDEPEND");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.MAY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.HOMESCHOOL.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(0L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(0L);
        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_StudentInMayPrevFebNonZeroFTE_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("INDEPEND");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.MAY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.HOMESCHOOL.getCode());
        student.setFte(BigDecimal.ONE);
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(0L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(0L);
        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @Test
    void studentPreviouslyReportedInIndependentAuthority_NoSchoolIdsFound_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("INDEPEND");
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.empty());
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(1L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(1L);
        // When
        var result = fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }

    @ParameterizedTest
    @CsvSource({
            "HS, true, DIST_LEARN",
            "KH, true, DIST_LEARN",
            "KF, true, DIST_LEARN",
            "01, true, DIST_LEARN",
            "02, true, DIST_LEARN",
            "03, true, DIST_LEARN",
            "04, true, DIST_LEARN",
            "05, true, DIST_LEARN",
            "06, true, DIST_LEARN",
            "07, true, DISTONLINE",
            "EU, true, DISTONLINE",
            "08, true, DISTONLINE",
            "09, true, DISTONLINE",
            "10, false, DISTONLINE",
            "11, false, DISTONLINE",
            "12, false, DISTONLINE",
            "SU, false, DISTONLINE",
            "GA, false, DISTONLINE"
    })
    void studentPreviouslyReportedInIndependentAuthority_GivenDifferentGrades_ReturnExpectedResult(String enrolledGradeCode, boolean expectedResult, String facilityTypeCode) {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        schoolTombstone.setSchoolCategoryCode("INDEPEND");
        schoolTombstone.setFacilityTypeCode(facilityTypeCode);
        schoolTombstone.setIndependentAuthorityId("AUTH_ID");
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(1L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(1L);
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
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode(schoolCategory);
        schoolTombstone.setFacilityTypeCode(facilityType);
        schoolTombstone.setIndependentAuthorityId("AUTH_ID");
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(1L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(1L);
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
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(UUID.randomUUID())));
        when(sdcSchoolCollectionRepository.findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionRepository.findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(anyList(), anyList())).thenReturn(1L);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(anyList(), anyList())).thenReturn(1L);
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
    void testHomeSchoolStudentIsNowOnlineKto9StudentOrHs_GivenDifferentGrades_ReturnExpectedResult(String enrolledGradeCode, boolean expectedResult) {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(anyList(), any(String.class), any())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9StudentOrHs(sdcStudentSagaData);

        // Then
        assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @CsvSource({
            "FEBRUARY, DIST_LEARN, true",
            "FEBRUARY, STANDARD, false",
            "FEBRUARY, DISTONLINE, true",
            "FEBRUARY, CONT_ED, true",
            "SEPTEMBER, DIST_LEARN, false",
            "SEPTEMBER, STANDARD, false",
            "SEPTEMBER, DISTONLINE, false",
            "SEPTEMBER, CONT_ED, false",
            "MAY, DIST_LEARN, true",
            "MAY, STANDARD, false",
            "MAY, DISTONLINE, true",
            "MAY, CONT_ED, true",
            "JULY, DIST_LEARN, false",
            "JULY, STANDARD, false",
            "JULY, DISTONLINE, false",
            "JULY, CONT_ED, false",
    })
    void testHomeSchoolStudentIsNowOnlineKto9StudentOrHs_GivenDifferentSchoolCategoriesAndFacilities_ReturnExpectedResult(String collectionType, String facilityType, boolean expectedResult) {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(facilityType);
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(collectionType);
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(anyList(), any(String.class), any())).thenReturn(1L);

        // When
        var result = fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9StudentOrHs(sdcStudentSagaData);

        // Then
        assertEquals(expectedResult, result);
    }
    @Test
    void testHomeSchoolStudentIsNowOnlineKto9StudentOrHs_GivenNoAssignedStudentId_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        // When
        var result = fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9StudentOrHs(sdcStudentSagaData);

        // Then
        assertFalse(result);
    }
    @Test
    void testHomeSchoolStudentIsNowOnlineKto9StudentOrHs_GivenNoPreviousCollectionForStudent_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        var collection = createMockCollectionEntity();
        var sdcCollection = createMockSdcSchoolCollectionEntity(collection, null);
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        student.setSdcSchoolCollection(sdcCollection);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);
        sdcStudentSagaData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(anyList(), any(String.class), any())).thenReturn(0L);

        // When
        var result = fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9StudentOrHs(sdcStudentSagaData);

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

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        schoolTombstone.setFacilityTypeCode(facilityTypeCode);

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
        student.setSdcSchoolCollection(schoolCollection1);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(anyList(), anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForSchoolAgedStudentInLastTwoYears(studentData);

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

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        schoolTombstone.setFacilityTypeCode(facilityTypeCode);

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
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setSdcSchoolCollection(schoolCollection1);
        studentData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(anyList(), anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForSchoolAgedStudentInLastTwoYears(studentData);

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

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolId(schoolId.toString());
        schoolTombstone.setFacilityTypeCode(facilityTypeCode);

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
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setSdcSchoolCollection(schoolCollection1);
        studentData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(anyList(), anyList(), any(String.class)))
                .thenReturn(1L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForSchoolAgedStudentInLastTwoYears(studentData);

        // Then
        assertFalse(result);
    }
    @Test
    void noCoursesForStudentInLastTwoYears_NoAssignedStudentId_ReturnsFalse() {
        // Given
        StudentRuleData sdcStudentSagaData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        schoolTombstone.setFacilityTypeCode("DIST_LEARN");
        sdcStudentSagaData.setSchool(schoolTombstone);
        SdcSchoolCollectionEntity sdcSchoolCollection = createMockSdcSchoolCollectionEntity(createMockCollectionEntity(), UUID.fromString(schoolTombstone.getSchoolId()));
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSdcSchoolCollection(sdcSchoolCollection);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        sdcStudentSagaData.setSdcSchoolCollectionStudentEntity(student);

        // When
        boolean result = fteCalculatorUtils.noCoursesForSchoolAgedStudentInLastTwoYears(sdcStudentSagaData);

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

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolId(schoolId.toString());
        schoolTombstone.setFacilityTypeCode(facilityTypeCode);

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
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setSdcSchoolCollection(schoolCollection1);
        studentData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(List.of());
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(anyList(), anyList(), any(String.class)))
                .thenReturn(1L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForSchoolAgedStudentInLastTwoYears(studentData);

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
            "DIST_LEARN, 0, true",
            "DIST_LEARN, 00, true",
            "DIST_LEARN, 000, true",
    })
    void noCoursesForStudentInLastTwoYears_GivenDifferentFacilityTypesAndNumCourses_ShouldReturnTrue(String facilityTypeCode, String numberOfCourses, boolean expectedResult) {
        // Given
        UUID schoolId = UUID.randomUUID();
        LocalDateTime studentCreateDate = LocalDateTime.now();

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        schoolTombstone.setFacilityTypeCode(facilityTypeCode);

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
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setSdcSchoolCollection(schoolCollection1);
        studentData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(anyList(), anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForSchoolAgedStudentInLastTwoYears(studentData);

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
            "GA, false"
    })
    void noCoursesForStudentInLastTwoYears_GivenAllGrades_ShouldReturnTrue(String enrolledGradeCode, boolean expectedResult) {
        // Given
        UUID schoolId = UUID.randomUUID();
        LocalDateTime studentCreateDate = LocalDateTime.now();
        String facilityTypeCode = "DIST_LEARN";
        String numberOfCourses = "0000";

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolId(UUID.randomUUID().toString());
        schoolTombstone.setFacilityTypeCode(facilityTypeCode);

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
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setSdcSchoolCollection(schoolCollection1);
        studentData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(anyList(), anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForSchoolAgedStudentInLastTwoYears(studentData);

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

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolId(schoolId.toString());
        schoolTombstone.setFacilityTypeCode(facilityTypeCode);

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
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);
        student.setAssignedStudentId(UUID.randomUUID());
        student.setSdcSchoolCollection(schoolCollection1);
        studentData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(any(UUID.class), any()))
                .thenReturn(lastTwoYearsOfCollections);
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(anyList(), anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        boolean result = fteCalculatorUtils.noCoursesForSchoolAgedStudentInLastTwoYears(studentData);

        // Then
        assertTrue(result);
    }

    @Test
    void testIncludedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline_OnlineSchool_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setFte(BigDecimal.TEN);
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(any(UUID.class), anyList(), any(String.class))).thenReturn(Collections.emptyList());

        // When
        var result = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void testIncludedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline_FteZero_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.POST_SEC.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setFte(BigDecimal.ZERO);
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));
        SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = studentRuleData.getSdcSchoolCollectionStudentEntity();

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(any(UUID.class), anyList(), any(String.class))).thenReturn(Collections.singletonList(sdcSchoolCollectionStudentEntity));

        // When
        var result = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void testIncludedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline_ReturnsTrue() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.POST_SEC.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setSdcSchoolCollection(createMockSdcSchoolCollectionEntity(createMockCollectionEntity(), null));
        student.setFte(BigDecimal.TEN);
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.randomUUID(), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(any(UUID.class), anyList(), any(String.class))).thenReturn(Collections.singletonList(student));
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));

        // When
        var result = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline(studentRuleData);

        // Then
        assertTrue(result);
    }

    @Test
    void testIncludedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnlineInGradeKto9_ZeroFte_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setFte(BigDecimal.ZERO);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(any(UUID.class), anyList(), any(String.class))).thenReturn(Collections.emptyList());

        // When
        var result = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void testIncludedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnlineInGradeKto9_NotOnlineSchool_Grade9_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.PROVINCIAL.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setFte(BigDecimal.TEN);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(any(UUID.class), anyList(), any(String.class))).thenReturn(Collections.emptyList());

        // When
        var result = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void testIncludedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnlineInGradeKto9_OnlineSchool_Grade10_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setFte(BigDecimal.TEN);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));
        SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = studentRuleData.getSdcSchoolCollectionStudentEntity();

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(any(UUID.class), anyList(), any(String.class))).thenReturn(Collections.singletonList(sdcSchoolCollectionStudentEntity));

        // When
        var result = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void testIncludedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9_OnlineSchool_Grade9_ReturnsTrue() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setSdcSchoolCollection(createMockSdcSchoolCollectionEntity(createMockCollectionEntity(), null));
        student.setFte(BigDecimal.TEN);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.randomUUID(), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(any(UUID.class), anyList(), any(String.class))).thenReturn(Collections.singletonList(student));
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));

        // When
        var result = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(studentRuleData);

        // Then
        assertTrue(result);
    }

    @Test
    void testReportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear_NotOnlineSchool_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.PROVINCIAL.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setFte(BigDecimal.TEN);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.fromString(getStudentMergeResult().getStudentID()), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(any(UUID.class), anyList(), any(String.class))).thenReturn(Collections.emptyList());

        // When
        var result = fteCalculatorUtils.reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void testReportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear_OnlineSchool_InDistrict_ReturnsTrue() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setFte(BigDecimal.TEN);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.randomUUID(), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalInAllDistrict(any(UUID.class), any(String.class))).thenReturn(Collections.singletonList(student));
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));

        // When
        var result = fteCalculatorUtils.reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(studentRuleData);

        // Then
        assertTrue(result);
    }

    @Test
    void testReportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear_OnlineSchool_NotInDistrict_ReturnsTrue() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());
        studentRuleData.setSchool(schoolTombstone);
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setFte(BigDecimal.TEN);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
        student.setCreateDate(LocalDateTime.now());
        student.setAssignedStudentId(UUID.randomUUID());
        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        studentRuleData.setHistoricStudentIds(List.of(UUID.randomUUID(), student.getAssignedStudentId()));

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalInAllDistrict(any(UUID.class), any(String.class))).thenReturn(Collections.singletonList(student));
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));

        // When
        var result = fteCalculatorUtils.reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(studentRuleData);

        // Then
        assertTrue(result);
    }

    @Test
    void testReportedInOtherDistrictsInPreviousCollectionThisSchoolYearInGrade8Or9WithNonZeroFte_Grade8Or9_NonZeroFte_ReturnsTrue() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setAssignedStudentId(UUID.randomUUID());

        SdcSchoolCollectionStudentEntity previousStudentEntity = new SdcSchoolCollectionStudentEntity();
        previousStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
        previousStudentEntity.setFte(BigDecimal.TEN);
        previousStudentEntity.setAssignedStudentId(student.getAssignedStudentId());

        SchoolTombstone school = new SchoolTombstone();
        school.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(school);

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalInOtherDistrictsNotInGrade8Or9WithNonZeroFte(any(UUID.class), anyList(), any(String.class))).thenReturn(Collections.singletonList(previousStudentEntity));

        studentRuleData.setSdcSchoolCollectionStudentEntity(student);

        // When
        var result = fteCalculatorUtils.reportedInOtherDistrictsInPreviousCollectionThisSchoolYearInGrade8Or9WithNonZeroFte(studentRuleData);

        // Then
        assertTrue(result);
    }

    @Test
    void testReportedInOtherDistrictsInPreviousCollectionThisSchoolYearInGrade8Or9WithNonZeroFte_Grade8Or9_ZeroFte_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setAssignedStudentId(UUID.randomUUID());

        SdcSchoolCollectionStudentEntity previousStudentEntity = new SdcSchoolCollectionStudentEntity();
        previousStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
        previousStudentEntity.setFte(BigDecimal.ZERO);
        previousStudentEntity.setAssignedStudentId(student.getAssignedStudentId());

        SchoolTombstone school = new SchoolTombstone();
        school.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(school);

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalInOtherDistrictsNotInGrade8Or9WithNonZeroFte(any(UUID.class), anyList(), any(String.class))).thenReturn(Collections.singletonList(previousStudentEntity));

        studentRuleData.setSdcSchoolCollectionStudentEntity(student);

        // When
        var result = fteCalculatorUtils.reportedInOtherDistrictsInPreviousCollectionThisSchoolYearInGrade8Or9WithNonZeroFte(studentRuleData);

        // Then
        assertTrue(result);
    }

    @Test
    void testReportedInOtherDistrictsInPreviousCollectionThisSchoolYearInGrade8Or9WithNonZeroFte_NotGrade8Or9_NonZeroFte_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setAssignedStudentId(UUID.randomUUID());

        SdcSchoolCollectionStudentEntity previousStudentEntity = new SdcSchoolCollectionStudentEntity();
        previousStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
        previousStudentEntity.setFte(BigDecimal.TEN);
        previousStudentEntity.setAssignedStudentId(student.getAssignedStudentId());

        SchoolTombstone school = new SchoolTombstone();
        school.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(school);

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalInOtherDistrictsNotInGrade8Or9WithNonZeroFte(any(UUID.class), anyList(), any(String.class)))
                .thenReturn(Collections.singletonList(previousStudentEntity));

        studentRuleData.setSdcSchoolCollectionStudentEntity(student);

        // When
        var result = fteCalculatorUtils.reportedInOtherDistrictsInPreviousCollectionThisSchoolYearInGrade8Or9WithNonZeroFte(studentRuleData);

        // Then
        assertTrue(result);
    }

    @Test
    void testReportedInAnyPreviousCollectionThisSchoolYearInOtherDistrictsInGrade8Or9WithNonZeroFte_NoPreviousCollections_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setAssignedStudentId(UUID.randomUUID());

        SchoolTombstone school = new SchoolTombstone();
        school.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(school);

        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalInOtherDistrictsNotInGrade8Or9WithNonZeroFte(any(UUID.class),anyList(), any(String.class))).thenReturn(Collections.emptyList());

        studentRuleData.setSdcSchoolCollectionStudentEntity(student);

        // When
        var result = fteCalculatorUtils.reportedInOtherDistrictsInPreviousCollectionThisSchoolYearInGrade8Or9WithNonZeroFte(studentRuleData);

        // Then
        assertFalse(result);
    }

    @Test
    void testReportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear_OnlineSchoolWithNonZeroFte_ReturnsTrue() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setFte(BigDecimal.TEN);
        student.setSdcSchoolCollection(createMockSdcSchoolCollectionEntity(createMockCollectionEntity(), null));
        student.setAssignedStudentId(UUID.randomUUID());

        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalInAllDistrict(any(UUID.class), any(String.class))).thenReturn(Collections.singletonList(student));
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));

        // When
        var result = fteCalculatorUtils.reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(studentRuleData);

        // Then
        assertTrue(result);
    }

    @Test
    void testReportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear_OnlineSchoolWithZeroFte_ReturnsFalse() {
        // Given
        StudentRuleData studentRuleData = new StudentRuleData();
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        studentRuleData.setSchool(schoolTombstone);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setFte(BigDecimal.ZERO);
        student.setSdcSchoolCollection(createMockSdcSchoolCollectionEntity(createMockCollectionEntity(), null));
        student.setAssignedStudentId(UUID.randomUUID());

        studentRuleData.setSdcSchoolCollectionStudentEntity(student);
        when(sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalInAllDistrict(any(UUID.class), any(String.class))).thenReturn(Collections.singletonList(student));
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));

        // When
        var result = fteCalculatorUtils.reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(studentRuleData);

        // Then
        assertFalse(result);
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

    public SdcSchoolCollectionEntity createMockSdcSchoolCollectionEntity(CollectionEntity entity, UUID schoolID){
        SdcSchoolCollectionEntity sdcEntity = new SdcSchoolCollectionEntity();
        sdcEntity.setCollectionEntity(entity);
        sdcEntity.setSchoolID(schoolID == null ? UUID.randomUUID() : schoolID);
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

    public StudentMerge getStudentMergeResult(){
        StudentMerge studentMerge = new StudentMerge();
        studentMerge.setStudentID(String.valueOf(UUID.randomUUID()));
        studentMerge.setMergeStudentID(String.valueOf(UUID.randomUUID()));
        studentMerge.setStudentMergeID(String.valueOf(UUID.randomUUID()));
        studentMerge.setStudentMergeID("TO");
        studentMerge.setStudentMergeSourceCode("MINISTRY");
        return studentMerge;
    }
}
