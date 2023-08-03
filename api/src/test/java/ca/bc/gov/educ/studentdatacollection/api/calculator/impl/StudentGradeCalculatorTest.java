package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
            "KH, 0.5",
            "HS, 0.0471",
            "08, 0.75",
            "09, 0.75",
            "10, 0",
            "11, 0",
            "12, 0",
            "SU, 0"
    })
    void testCalculateFte_WithValidGradeCodes(String gradeCode, String expectedFteValue) {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode(gradeCode);
        student.setNumberOfCourses("2");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        boolean isHighSchoolGrade = gradeCode.equals("10") || gradeCode.equals("11") || gradeCode.equals("12") || gradeCode.equals("SU");
        if (isHighSchoolGrade) {
            Map<String, Object> expectedResult = new HashMap<>();
            expectedResult.put("fte", new BigDecimal(expectedFteValue));
            expectedResult.put("fteZeroReason", null);

            when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        }
        Map<String, Object> result = studentGradeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal(expectedFteValue);
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));

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

        Map<String, Object> result = studentGradeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.5");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_With8GradeAndNullCourses_ThenReturnFteCalculation() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode("08");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        Map<String, Object> result = studentGradeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.5");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));
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
