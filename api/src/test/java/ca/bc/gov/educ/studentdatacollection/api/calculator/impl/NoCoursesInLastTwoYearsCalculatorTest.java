package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NoCoursesInLastTwoYearsCalculatorTest {

    private FteCalculator nextCalculator;
    private NoCoursesInLastTwoYearsCalculator noCoursesInLastTwoYearsCalculator;
    private FteCalculatorUtils fteCalculatorUtils;

    @BeforeEach
    public void setUp() {
        nextCalculator = mock(FteCalculator.class);
        fteCalculatorUtils = mock(FteCalculatorUtils.class);

        noCoursesInLastTwoYearsCalculator = new NoCoursesInLastTwoYearsCalculator();
        noCoursesInLastTwoYearsCalculator.setNext(nextCalculator);
        noCoursesInLastTwoYearsCalculator.fteCalculatorUtils = fteCalculatorUtils;
    }

    @Test
    void testCalculateFte_NoCoursesInLastTwoYears_ReturnsFteCalculation() {
        // Given
        StudentRuleData student = new StudentRuleData();
        student.setSdcSchoolCollectionStudentEntity(new SdcSchoolCollectionStudentEntity());
        when(fteCalculatorUtils.noCoursesForStudentInLastTwoYears(any())).thenReturn(true);

        // When
        FteCalculationResult result = noCoursesInLastTwoYearsCalculator.calculateFte(student);

        // Then
        assertEquals(BigDecimal.ZERO, result.getFte());
        assertEquals(ZeroFteReasonCodes.INACTIVE.getCode(), result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_HasCoursesInLastTwoYears_CallsNextCalculator() {
        // Given
        StudentRuleData student = new StudentRuleData();
        student.setSdcSchoolCollectionStudentEntity(new SdcSchoolCollectionStudentEntity());
        when(fteCalculatorUtils.noCoursesForStudentInLastTwoYears(any())).thenReturn(false);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = noCoursesInLastTwoYearsCalculator.calculateFte(student);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(student);
    }
}
