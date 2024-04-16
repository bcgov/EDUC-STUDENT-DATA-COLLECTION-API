package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollectionAndGradeCalculatorTest extends BaseStudentDataCollectionAPITest {

    private FteCalculator nextCalculator;
    private CollectionAndGradeCalculator collectionAndGradeCalculator;

    @BeforeEach
    public void setUp() {
        nextCalculator = mock(FteCalculator.class);
        collectionAndGradeCalculator = new CollectionAndGradeCalculator();
        collectionAndGradeCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_JulyCollectionWithGrade1To7_ReturnsFteCalculation() {
        // Given
        String enrolledGradeCode = "06";
        int numberOfCourses = 4;

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setNumberOfCourses(String.valueOf(numberOfCourses));

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = collectionAndGradeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ONE;

        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_JulyCollectionWithGrade8To12_ReturnsFteCalculation() {
        // Given
        String enrolledGradeCode = "10";

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setNumberOfCourses("0600");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = collectionAndGradeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.75");

        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_JulyCollectionWithInvalidGrade_ReturnsFteCalculation() {
        // Given
        String enrolledGradeCode = "13"; // Invalid grade code

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When and Then
        assertThrows(StudentDataCollectionAPIRuntimeException.class, () -> collectionAndGradeCalculator.calculateFte(studentData));
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_NonJulyCollection_CallsNextCalculator() {
        // Given
        String enrolledGradeCode = "10";
        int numberOfCourses = 6;

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.MAY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);


        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setNumberOfCourses(String.valueOf(numberOfCourses));

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(studentData)).thenReturn(expectedResult);
        FteCalculationResult result = collectionAndGradeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_NullNumCourses_ReturnsFteCalculation() {
        // Given
        String enrolledGradeCode = "10";

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = collectionAndGradeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;

        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_NullCollectionType_CallsNextCalculator() {
        // Given
        String enrolledGradeCode = "10";
        int numberOfCourses = 6;

        CollectionEntity collection = createMockCollectionEntity();
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        student.setNumberOfCourses(String.valueOf(numberOfCourses));

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(studentData)).thenReturn(expectedResult);
        FteCalculationResult result = collectionAndGradeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }
}

