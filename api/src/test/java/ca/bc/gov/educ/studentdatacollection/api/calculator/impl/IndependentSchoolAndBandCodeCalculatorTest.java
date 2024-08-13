package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class IndependentSchoolAndBandCodeCalculatorTest {

    @Mock
    private FteCalculator nextCalculator;

    @InjectMocks
    private IndependentSchoolAndBandCodeCalculator independentSchoolAndBandCodeCalculator;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        independentSchoolAndBandCodeCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_WithIndependentSchoolAndValidBandCode_ReturnsFte() {
        // Given
        String schoolCategory = "INDEPEND";
        String bandCodeValue = "BAND_CODE_1";

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setBandCode(bandCodeValue);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.NOMINAL_ROLL_ELIGIBLE.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithValidBandCodeAndExistingFundingCode_ReturnsFte() {
        // Given
        String schoolCategory = "INDEPEND";
        String bandCodeValue = "BAND_CODE_1";
        String fundingCode = "21";

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingCode);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.getFte(), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_WithIndependentSchoolAndFundingCode20_ReturnsFte() {
        // Given
        String schoolCategory = "INDEPEND";
        String bandCodeValue = "BAND_CODE_INVALID";
        String fundingCode = "20";

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingCode);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.NOMINAL_ROLL_ELIGIBLE.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithNonIndependentSchool_CallsNextFte() {
        // Given
        String schoolCategory = "DISTRICT"; // Non-independent school facility type code
        String bandCodeValue = "BAND_CODE_3";
        String fundingCode = "20";

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingCode);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);


        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.getFte(), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_WithNullBandCode_CallsNextFte() {
        // Given
        String schoolCategory = "INDEPEND";

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(schoolTombstone);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.getFte(), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_WithNullSchool_CallsNextFte() {
        // Given
        String bandCodeValue = "BAND_CODE_3";
        String fundingCode = "20";

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingCode);

        StudentRuleData studentData = new StudentRuleData();

        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.getFte(), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }
}
