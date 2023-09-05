package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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
        school.setSchoolCategoryCode(SchoolCategoryCodes.OFFSHORE.getCode());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(new SdcSchoolCollectionStudentEntity());

        // When
        FteCalculationResult result = offshoreStudentCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        assertEquals(expectedFte, result.getFte());
        assertEquals(ZeroFteReasonCodes.OFFSHORE.getCode(), result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithNonOffshoreSchoolCategory_CallsNextFteCalculation() {
        // Given
        School school = new School();
        school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(new SdcSchoolCollectionStudentEntity());

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
        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(new SdcSchoolCollectionStudentEntity());

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

