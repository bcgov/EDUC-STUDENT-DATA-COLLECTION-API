package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class IndAuthorityDoubleReportedCalculatorTest {

    @Mock
    private SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    @Mock
    private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    @Mock
    private RestUtils restUtils;
    @Mock
    private FteCalculatorUtils fteCalculatorUtils;
    @Mock
    private FteCalculator nextCalculator;
    @InjectMocks
    private IndAuthorityDoubleReportedCalculator indAuthorityDoubleReportedCalculator;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        indAuthorityDoubleReportedCalculator.setNext(nextCalculator);
    }

   /* @Test
    void testCalculateFte_WithSpringCollectionAndIndependentOnlineSchoolAndInDistrictFundedGradeAndNoPreviousCollections_ShouldCallNextCalculator() {
        // Given
        School school = new School();
        school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
        school.setIndependentAuthorityId(UUID.randomUUID().toString());
        school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());

        SdcSchoolCollectionStudent sdcSchoolCollectionStudent = new SdcSchoolCollectionStudent();
        sdcSchoolCollectionStudent.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setCollectionTypeCode(CollectionTypeCodes.ENTRY2.getTypeCode());
        studentData.setSdcSchoolCollectionStudent(sdcSchoolCollectionStudent);
        studentData.setSchool(school);

        SdcSchoolCollectionEntity schoolCollection = new SdcSchoolCollectionEntity();
        schoolCollection.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection.setSchoolID(UUID.randomUUID());
        schoolCollection.setCreateDate(LocalDateTime.parse(LocalDateTime.now().toString()).minusYears(1)); // One year ago

        Map<String, LocalDateTime> startEndDateMap = new HashMap<>();
        startEndDateMap.put("startDate", LocalDateTime.now());
        startEndDateMap.put("endDate", LocalDateTime.now());

        when(sdcSchoolCollectionRepository.findAllBySchoolIDInAndCreateDateBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(List.of(schoolCollection)));
        //when(fteCalculatorUtils.getPreviousCollectionStartAndEndDates(any())).thenReturn(startEndDateMap);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollectionIDIn(anyList())).thenReturn(0L);
        when(restUtils.getSchoolIDsByIndependentAuthorityID(any())).thenReturn(Optional.of(List.of(UUID.randomUUID())));

        // When
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        Map<String, Object> result = indAuthorityDoubleReportedCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.get("fte"), result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_SpringCollectionAndIndependentOnlineSchoolAndInDistrictFundedGradeAndPreviousCollections_ShouldReturnZeroFteWithReason() {
        // Given
        School school = new School();
        school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
        school.setIndependentAuthorityId(UUID.randomUUID().toString());
        school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());

        SdcSchoolCollectionStudent sdcSchoolCollectionStudent = new SdcSchoolCollectionStudent();
        sdcSchoolCollectionStudent.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setCollectionTypeCode(CollectionTypeCodes.ENTRY2.getTypeCode());
        studentData.setSdcSchoolCollectionStudent(sdcSchoolCollectionStudent);
        studentData.setSchool(school);

        SdcSchoolCollectionEntity schoolCollection = new SdcSchoolCollectionEntity();
        schoolCollection.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection.setSchoolID(UUID.randomUUID());
        schoolCollection.setCreateDate(LocalDateTime.parse(LocalDateTime.now().toString()).minusYears(1)); // One year ago

        Map<String, LocalDateTime> startEndDateMap = new HashMap<>();
        startEndDateMap.put("startDate", LocalDateTime.now());
        startEndDateMap.put("endDate", LocalDateTime.now());

        when(sdcSchoolCollectionRepository.findAllBySchoolIDInAndCreateDateBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(List.of(schoolCollection)));
        //when(fteCalculatorUtils.getPreviousCollectionStartAndEndDates(any())).thenReturn(startEndDateMap);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollectionIDIn(anyList())).thenReturn(1L);
        when(restUtils.getSchoolIDsByIndependentAuthorityID(any())).thenReturn(Optional.of(List.of(UUID.randomUUID())));

        // When
        Map<String, Object> result = indAuthorityDoubleReportedCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.get("fte"));
        assertEquals("The authority has already received funding for the student this year.", result.get("fteZeroReason"));
        verify(sdcSchoolCollectionRepository).findAllBySchoolIDInAndCreateDateBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(sdcSchoolCollectionStudentRepository).countAllByAssignedStudentIdAndSdcSchoolCollectionIDIn(anyList());
    }

    @Test
    void testCalculateFte_WithMayCollectionAndIndependentOnlineSchoolAndInDistrictFundedGradeAndNoPreviousCollections_ShouldCallNextCalculator() {
        // Given
        School school = new School();
        school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
        school.setIndependentAuthorityId(UUID.randomUUID().toString());
        school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());

        SdcSchoolCollectionStudent sdcSchoolCollectionStudent = new SdcSchoolCollectionStudent();
        sdcSchoolCollectionStudent.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setCollectionTypeCode(CollectionTypeCodes.ENTRY2.getTypeCode());
        studentData.setSdcSchoolCollectionStudent(sdcSchoolCollectionStudent);
        studentData.setSchool(school);

        SdcSchoolCollectionEntity schoolCollection = new SdcSchoolCollectionEntity();
        schoolCollection.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection.setSchoolID(UUID.randomUUID());
        schoolCollection.setCreateDate(LocalDateTime.parse(LocalDateTime.now().toString()).minusYears(1)); // One year ago

        Map<String, LocalDateTime> startEndDateMap = new HashMap<>();
        startEndDateMap.put("startDate", LocalDateTime.now());
        startEndDateMap.put("endDate", LocalDateTime.now());

        when(sdcSchoolCollectionRepository.findAllBySchoolIDInAndCreateDateBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(List.of(schoolCollection)));
        //when(fteCalculatorUtils.getPreviousCollectionStartAndEndDates(any())).thenReturn(startEndDateMap);
        when(sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollectionIDIn(anyList())).thenReturn(1L);
        when(restUtils.getSchoolIDsByIndependentAuthorityID(any())).thenReturn(Optional.of(List.of(UUID.randomUUID())));

        // When
        Map<String, Object> result = indAuthorityDoubleReportedCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.get("fte"));
        assertEquals("The authority has already received funding for the student this year.", result.get("fteZeroReason"));
        verify(sdcSchoolCollectionRepository).findAllBySchoolIDInAndCreateDateBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(sdcSchoolCollectionStudentRepository).countAllByAssignedStudentIdAndSdcSchoolCollectionIDIn(anyList());
    }

    @Test
    void testCalculateFte_WithMayCollectionAndIndependentOnlineSchoolAndInDistrictFundedGradeAndPreviousCollections_ShouldReturnZeroFteWithReason() {
        // Given
        String collectionTypeCode = CollectionTypeCodes.ENTRY3.getTypeCode();
        String schoolCategoryCode = SchoolCategoryCodes.INDEPEND.getCode();
        String facilityTypeCode = FacilityTypeCodes.DIST_LEARN.getCode();
        String enrolledGradeCode = "10";
        String createDate = LocalDateTime.now().toString();

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setCollectionTypeCode(collectionTypeCode);
        studentData.setSchoolCategoryCode(schoolCategoryCode);
        studentData.setFacilityTypeCode(facilityTypeCode);
        studentData.setSdcSchoolCollectionStudent(new SdcSchoolCollectionStudent());
        studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode(enrolledGradeCode);

        SdcSchoolCollectionEntity schoolCollection = new SdcSchoolCollectionEntity();
        schoolCollection.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection.setSchoolID(UUID.randomUUID());
        schoolCollection.setCreateDate(LocalDateTime.parse(createDate).minusYears(1)); // One year ago
        List<SdcSchoolCollectionEntity> previousCollections = Arrays.asList(schoolCollection);

        when(sdcSchoolCollectionRepository.findAllBySchoolIDInAndCreateDateBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(previousCollections);
        when(sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollectionIDIn(anyList())).thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = indAuthorityDoubleReportedCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.get("fte"));
        assertEquals("The authority has already received funding for the student this year.", result.get("fteZeroReason"));
        verify(sdcSchoolCollectionRepository).findAllBySchoolIDInAndCreateDateBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(sdcSchoolCollectionStudentRepository).findAllBySdcSchoolCollectionIDIn(anyList());
    }

    @Test
    void testCalculateFte_WithNonSpringOrMayCollection_ShouldCallNextCalculator() {
        // Given
        String collectionTypeCode = "OTHER_TYPE";
        String schoolCategoryCode = SchoolCategoryCodes.INDEPEND.getCode();
        String facilityTypeCode = FacilityTypeCodes.DIST_LEARN.getCode();
        String enrolledGradeCode = "10";
        String createDate = LocalDateTime.now().toString();

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setCollectionTypeCode(collectionTypeCode);
        studentData.setSchoolCategoryCode(schoolCategoryCode);
        studentData.setFacilityTypeCode(facilityTypeCode);
        studentData.setSdcSchoolCollectionStudent(new SdcSchoolCollectionStudent());
        studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode(enrolledGradeCode);

        // When
        BigDecimal expectedFte = BigDecimal.valueOf(0.7);
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", expectedFte);
        expectedResult.put("fteZeroReason", null);
        when(indAuthorityDoubleReportedCalculator.getNextCalculator()).thenReturn(new FteCalculator() {
            @Override
            public void setNext(FteCalculator nextCalculator) {
            }

            @Override
            public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
                return expectedResult;
            }
        });
        Map<String, Object> result = indAuthorityDoubleReportedCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(indAuthorityDoubleReportedCalculator).getNextCalculator();
    }

    @Test
    void testCalculateFte_WithNonIndependentOnlineSchool_ShouldCallNextCalculator() {
        // Given
        String collectionTypeCode = CollectionTypeCodes.ENTRY2.getTypeCode();
        String schoolCategoryCode = SchoolCategoryCodes.REGULAR.getCode();
        String facilityTypeCode = FacilityTypeCodes.REGULAR.getCode();
        String enrolledGradeCode = "10";
        String createDate = LocalDateTime.now().toString();

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setCollectionTypeCode(collectionTypeCode);
        studentData.setSchoolCategoryCode(schoolCategoryCode);
        studentData.setFacilityTypeCode(facilityTypeCode);
        studentData.setSdcSchoolCollectionStudent(new SdcSchoolCollectionStudent());
        studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode(enrolledGradeCode);

        // When
        BigDecimal expectedFte = BigDecimal.valueOf(0.7);
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", expectedFte);
        expectedResult.put("fteZeroReason", null);
        when(indAuthorityDoubleReportedCalculator.getNextCalculator()).thenReturn(new FteCalculator() {
            @Override
            public void setNext(FteCalculator nextCalculator) {
            }

            @Override
            public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
                return expectedResult;
            }
        });
        Map<String, Object> result = indAuthorityDoubleReportedCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(indAuthorityDoubleReportedCalculator).getNextCalculator();
    }

    @Test
    void testCalculateFte_WithNonDistrictFundedGrade_ShouldCallNextCalculator() {
        // Given
        String collectionTypeCode = CollectionTypeCodes.ENTRY2.getTypeCode();
        String schoolCategoryCode = SchoolCategoryCodes.INDEPEND.getCode();
        String facilityTypeCode = FacilityTypeCodes.DIST_LEARN.getCode();
        String enrolledGradeCode = "06";
        String createDate = LocalDateTime.now().toString();

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setCollectionTypeCode(collectionTypeCode);
        studentData.setSchoolCategoryCode(schoolCategoryCode);
        studentData.setFacilityTypeCode(facilityTypeCode);
        studentData.setSdcSchoolCollectionStudent(new SdcSchoolCollectionStudent());
        studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode(enrolledGradeCode);

        // When
        BigDecimal expectedFte = BigDecimal.valueOf(0.7);
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", expectedFte);
        expectedResult.put("fteZeroReason", null);
        when(indAuthorityDoubleReportedCalculator.getNextCalculator()).thenReturn(new FteCalculator() {
            @Override
            public void setNext(FteCalculator nextCalculator) {
            }

            @Override
            public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
                return expectedResult;
            }
        });
        Map<String, Object> result = indAuthorityDoubleReportedCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(indAuthorityDoubleReportedCalculator).getNextCalculator();
    }*/
}

