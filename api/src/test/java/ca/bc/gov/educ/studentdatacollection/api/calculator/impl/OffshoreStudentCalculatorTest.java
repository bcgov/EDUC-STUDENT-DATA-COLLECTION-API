package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

class OffshoreStudentCalculatorTest {

    private OffshoreStudentCalculator offshoreStudentCalculator;
    private FteCalculator nextCalculator;

    @BeforeEach
    public void setup() {
        nextCalculator = mock(FteCalculator.class);
        offshoreStudentCalculator = new OffshoreStudentCalculator();
        offshoreStudentCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_WithOffshoreSchoolCategory_ReturnsZeroFteWithReason() {
        // Given
        School school = new School();
        school.setSchoolCategoryCode(Constants.OFFSHORE);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);

        // When
        FteCalculationResult result = offshoreStudentCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        assertEquals(expectedFte, result.getFte());
        assertEquals("Offshore students do not receive funding.", result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithNonOffshoreSchoolCategory_CallsNextFteCalculation() {
        // Given
        School school = new School();
        school.setSchoolCategoryCode(Constants.INDEPEND);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = offshoreStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.getFte(), result.getFte());
        assertEquals(expectedResult.getFteZeroReason(), result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_WithNullSchool_CallsNextFteCalculation() {
        // Given
        SdcStudentSagaData studentData = new SdcStudentSagaData();

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = offshoreStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.getFte(), result.getFte());
        assertEquals(expectedResult.getFteZeroReason(), result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }
}

