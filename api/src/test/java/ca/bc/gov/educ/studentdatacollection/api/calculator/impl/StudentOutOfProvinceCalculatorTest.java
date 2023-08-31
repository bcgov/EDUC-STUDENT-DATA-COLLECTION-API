package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class StudentOutOfProvinceCalculatorTest {

    private StudentOutOfProvinceCalculator studentOutOfProvinceCalculator;
    private FteCalculator nextCalculator;

    @BeforeEach
    public void setup() {
        nextCalculator = mock(FteCalculator.class);
        studentOutOfProvinceCalculator = new StudentOutOfProvinceCalculator();
        studentOutOfProvinceCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_StudentOutOfProvince_ThenReturnZeroFteAndReason() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSchoolFundingCode("14");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = studentOutOfProvinceCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.getFte());
        assertEquals(ZeroFteReasonCodes.OUT_OF_PROVINCE.getCode(), result.getFteZeroReason());

        // Ensure that the nextCalculator.calculateFte method is not called
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentInProvince_ThenNextFteCalculatorCalled() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSchoolFundingCode("05");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = studentOutOfProvinceCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);

        // Ensure that the nextCalculator.calculateFte method is called with the correct studentData
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_NullSchoolFundingCode_ThenNextFteCalculatorIsCalled() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSchoolFundingCode(null);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = studentOutOfProvinceCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);

        // Ensure that the nextCalculator.calculateFte method is called with the correct studentData
        verify(nextCalculator).calculateFte(studentData);
    }
}
