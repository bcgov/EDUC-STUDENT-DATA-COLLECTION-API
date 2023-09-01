package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

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
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsAdult(true);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
        student.setNumberOfCourses("0500");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

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
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsAdult(false);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADUATED_ADULT.getCode());
        student.setNumberOfCourses("0300");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

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
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsAdult(false);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

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
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

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
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsAdult(true);
        student.setEnrolledGradeCode(SchoolGradeCodes.GRADUATED_ADULT.getCode());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = adultStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }
}

