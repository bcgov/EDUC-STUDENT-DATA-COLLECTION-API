package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

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

        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

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

        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingCode);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

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

        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingCode);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

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

        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingCode);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);


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

        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

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

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingCode);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

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