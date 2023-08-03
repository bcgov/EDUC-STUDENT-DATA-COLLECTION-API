package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Map;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

class StudentGraduatedCalculatorTest {

    private StudentGraduatedCalculator studentGraduatedCalculator;
    private FteCalculator nextCalculator;

    @BeforeEach
    void setup() {
        nextCalculator = mock(FteCalculator.class);
        studentGraduatedCalculator = new StudentGraduatedCalculator();
        studentGraduatedCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_StudentGraduated_ThenFteCalculatedWithoutSupportBlocks() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsGraduated(true);
        student.setNumberOfCourses("5");
        student.setSupportBlocks("2");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.625");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));

        // Ensure that the nextCalculator.calculateFte method is not called
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentNotGraduated_ThenFteCalculatedWithSupportBlocks() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsGraduated(false);
        student.setNumberOfCourses("5");
        student.setSupportBlocks("2");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.875");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));

        // Ensure that the nextCalculator.calculateFte method is not called
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentNotGraduatedWithFullCourseLoad_ThenFteCalculationRoundedDownToOne() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsGraduated(false);
        student.setNumberOfCourses("8");
        student.setSupportBlocks("2");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("1");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));

        // Ensure that the nextCalculator.calculateFte method is not called
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentNotGraduatedWithNullCourses_ThenFteCalculatedWithSupportBlocks() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsGraduated(false);
        student.setSupportBlocks("2");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.25");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));

        // Ensure that the nextCalculator.calculateFte method is not called
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
        Map<String, Object> result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.25");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));

        // Ensure that the nextCalculator.calculateFte method is not called
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentWithNullGraduation_ThenFteCalculatedWithSupportBlocks() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setNumberOfCourses("5");
        student.setSupportBlocks("2");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.875");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));

        // Ensure that the nextCalculator.calculateFte method is not called
        verify(nextCalculator, never()).calculateFte(any());
    }
}
