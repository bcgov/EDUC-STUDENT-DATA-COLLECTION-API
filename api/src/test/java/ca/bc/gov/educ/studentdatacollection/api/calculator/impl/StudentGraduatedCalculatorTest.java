package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StudentGraduatedCalculatorTest {

    private StudentGraduatedCalculator studentGraduatedCalculator;
    private FteCalculator nextCalculator;

    @BeforeEach
    void setup() {
        nextCalculator = mock(FteCalculator.class);
        studentGraduatedCalculator = new StudentGraduatedCalculator();
        studentGraduatedCalculator.setNext(nextCalculator);
    }

    @ParameterizedTest
    @CsvSource({
            "true, 0500, 0.625",
            "false, 0500, 0.875",
            "false, 0800, 1",
            "false, , 0.25"
    })
    void testCalculateFte_StudentGraduated_ThenFteCalculatedWithoutSupportBlocks(boolean isGraduated, String numberOfCourses, String expectedResults) {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsGraduated(isGraduated);
        student.setNumberOfCourses(numberOfCourses);
        student.setSupportBlocks("2");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal(expectedResults);
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentNotGraduatedWithEmptyCourses_ThenFteCalculatedWithSupportBlocks() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsGraduated(false);
        student.setNumberOfCourses("");
        student.setSupportBlocks("2");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.25");
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentWithNullGraduation_ThenFteCalculatedWithSupportBlocks() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setNumberOfCourses("0500");
        student.setSupportBlocks("2");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.875");
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }
}
