package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

class AdultStudentCalculatorTest {

    private FteCalculator nextCalculator;
    private AdultStudentCalculator adultStudentCalculator;

    @BeforeEach
    public void setUp() {
        nextCalculator = mock(FteCalculator.class);
        adultStudentCalculator = new AdultStudentCalculator();
        adultStudentCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_IsAdultStudent() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsAdult(true);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
        student.setNumberOfCourses("0500");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult result = adultStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(new BigDecimal("0.625"), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_IsGAAndNotAdultStudent() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsAdult(false);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADUATED_ADULT.getCode());
        student.setNumberOfCourses("0300");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult result = adultStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(new BigDecimal("0.375"), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_IsNotAdultAndNotGAStudent() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsAdult(false);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult fteValues = new FteCalculationResult();
        fteValues.setFte(BigDecimal.ZERO);
        fteValues.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(fteValues);
        FteCalculationResult result = adultStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(fteValues.getFte(), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_NullIsAdultStudent() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult fteValues = new FteCalculationResult();
        fteValues.setFte(BigDecimal.ZERO);
        fteValues.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(fteValues);
        FteCalculationResult result = adultStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(fteValues.getFte(), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_NullNumCourses() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsAdult(true);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADUATED_ADULT.getCode());

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult result = adultStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }
}

