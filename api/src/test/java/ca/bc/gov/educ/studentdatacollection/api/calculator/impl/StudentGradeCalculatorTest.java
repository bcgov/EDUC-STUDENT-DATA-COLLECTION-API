package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

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
            "KH, 2, 0.5",
            "HS, 2, 0.0471",
            "08, 2, 0.75",
            "09, 2, 0.75",
            "10, 2, 0",
            "11, 2, 0",
            "12, 2, 0",
            "SU, 2, 0",
            "08, 12, 1",
    })
    void testCalculateFte_WithValidGradeCodes(String gradeCode, String numberOfCourses, String expectedFteValue) {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode(gradeCode);
        student.setNumberOfCourses(numberOfCourses);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

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
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode("08");
        student.setNumberOfCourses("");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

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
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode("08");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

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
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode("InvalidGradeCode");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When and Then
        assertThrows(SagaRuntimeException.class, () -> studentGradeCalculator.calculateFte(studentData));
    }
}
