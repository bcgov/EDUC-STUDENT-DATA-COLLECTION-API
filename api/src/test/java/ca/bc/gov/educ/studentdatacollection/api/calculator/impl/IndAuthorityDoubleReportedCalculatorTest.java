package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndAuthorityDoubleReportedCalculatorTest {

    private FteCalculator nextCalculator;
    private IndAuthorityDoubleReportedCalculator indAuthorityDoubleReportedCalculator;
    private FteCalculatorUtils fteCalculatorUtils;

    @BeforeEach
    public void setUp() {
        nextCalculator = mock(FteCalculator.class);
        fteCalculatorUtils = mock(FteCalculatorUtils.class);

        indAuthorityDoubleReportedCalculator = new IndAuthorityDoubleReportedCalculator();
        indAuthorityDoubleReportedCalculator.setNext(nextCalculator);
        indAuthorityDoubleReportedCalculator.fteCalculatorUtils = fteCalculatorUtils;
    }

    @Test
    void testCalculateFte_StudentDoubleReported() {
        // Given
        SdcStudentSagaData studentData = new SdcStudentSagaData();

        when(fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(studentData)).thenReturn(true);

        // When
        FteCalculationResult result = indAuthorityDoubleReportedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.IND_AUTH_DUPLICATE_FUNDING.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentNotDoubleReported() {
        // Given
        SdcStudentSagaData studentData = new SdcStudentSagaData();

        when(fteCalculatorUtils.studentPreviouslyReportedInIndependentAuthority(studentData)).thenReturn(false);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = indAuthorityDoubleReportedCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }
}

