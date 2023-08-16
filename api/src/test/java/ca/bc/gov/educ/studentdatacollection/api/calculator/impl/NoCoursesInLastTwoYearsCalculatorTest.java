package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

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
        SdcStudentSagaData student = new SdcStudentSagaData();
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
        SdcStudentSagaData student = new SdcStudentSagaData();
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
