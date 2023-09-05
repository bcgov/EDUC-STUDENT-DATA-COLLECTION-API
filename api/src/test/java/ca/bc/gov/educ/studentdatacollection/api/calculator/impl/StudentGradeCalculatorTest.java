package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentGradeCalculatorTest {

    private StudentGradeCalculator studentGradeCalculator;
    private FteCalculator nextCalculator;

    @BeforeEach
    public void setup() {
        nextCalculator = mock(FteCalculator.class);
        studentGradeCalculator = new StudentGradeCalculator();
        studentGradeCalculator.setNext(nextCalculator);
    }

    @ParameterizedTest
    @CsvSource({
            "KH, 0200, 0.5",
            "HS, 0200, 0.0471",
            "08, 0200, 0.75",
            "09, 0200, 0.75",
            "10, 0200, 0",
            "11, 0200, 0",
            "12, 0200, 0",
            "SU, 0200, 0",
            "08, 1200, 1",
    })
    void testCalculateFte_WithValidGradeCodes(String gradeCode, String numberOfCourses, String expectedFteValue) {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(gradeCode);
        student.setNumberOfCourses(numberOfCourses);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        boolean isHighSchoolGrade = gradeCode.equals("10") || gradeCode.equals("11") || gradeCode.equals("12") || gradeCode.equals("SU");
        if (isHighSchoolGrade) {
            FteCalculationResult expectedResult = new FteCalculationResult();
            expectedResult.setFte(new BigDecimal(expectedFteValue));
            expectedResult.setFteZeroReason(null);

            when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        }
        FteCalculationResult result = studentGradeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal(expectedFteValue);
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());

        // Ensure that the nextCalculator.calculateFte method is called for grade codes "10", "11", "12", and "SU"
        if (isHighSchoolGrade) {
            verify(nextCalculator).calculateFte(studentData);
        } else {
            // Ensure that the nextCalculator.calculateFte method is not called for other grade codes
            verify(nextCalculator, never()).calculateFte(any());
        }
    }

    @Test
    void testCalculateFte_With8GradeAndEmptyCourses_ThenReturnFteCalculation() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode("08");
        student.setNumberOfCourses("");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        FteCalculationResult result = studentGradeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.5");
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_With8GradeAndNullCourses_ThenReturnFteCalculation() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode("08");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        FteCalculationResult result = studentGradeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.5");
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_InvalidGradeCode() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode("InvalidGradeCode");
        student.setSdcSchoolCollection(new SdcSchoolCollectionEntity());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When and Then
        assertThrows(StudentDataCollectionAPIRuntimeException.class, () -> studentGradeCalculator.calculateFte(studentData));
    }
}
