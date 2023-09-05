package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollectionAndGradeCalculatorTest {

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
        String collectionTypeCode = "JULY";
        String enrolledGradeCode = "06";
        int numberOfCourses = 4;

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses(String.valueOf(numberOfCourses));

        StudentRuleData studentData = new StudentRuleData();
        studentData.setCollectionTypeCode(collectionTypeCode);
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
        String collectionTypeCode = "JULY";
        String enrolledGradeCode = "10";

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses("0600");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setCollectionTypeCode(collectionTypeCode);
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
        String collectionTypeCode = "JULY";
        String enrolledGradeCode = "13"; // Invalid grade code

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setSdcSchoolCollection(new SdcSchoolCollectionEntity());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setCollectionTypeCode(collectionTypeCode);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When and Then
        assertThrows(StudentDataCollectionAPIRuntimeException.class, () -> collectionAndGradeCalculator.calculateFte(studentData));
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_NonJulyCollection_CallsNextCalculator() {
        // Given
        String collectionTypeCode = "OCTOBER"; // Non-July collection type
        String enrolledGradeCode = "10";
        int numberOfCourses = 6;

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses(String.valueOf(numberOfCourses));

        StudentRuleData studentData = new StudentRuleData();
        studentData.setCollectionTypeCode(collectionTypeCode);
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
        String collectionTypeCode = "JULY";
        String enrolledGradeCode = "10";

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setCollectionTypeCode(collectionTypeCode);
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

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
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

