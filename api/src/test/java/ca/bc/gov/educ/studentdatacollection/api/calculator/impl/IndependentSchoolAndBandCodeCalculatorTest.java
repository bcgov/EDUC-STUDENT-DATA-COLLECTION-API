package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.BandCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.BandCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

class IndependentSchoolAndBandCodeCalculatorTest {

    @Mock
    private FteCalculator nextCalculator;

    @Mock
    private BandCodeRepository bandCodeRepository;

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
        LocalDateTime effectiveDate = LocalDateTime.now().minusDays(1); // One day ago
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(1); // One day ahead of now

        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

        BandCodeEntity bandCode = new BandCodeEntity();
        bandCode.setEffectiveDate(effectiveDate);
        bandCode.setExpiryDate(expiryDate);

        when(bandCodeRepository.findById(bandCodeValue)).thenReturn(Optional.of(bandCode));

        // When
        Map<String, Object> result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = "The student is Nominal Roll eligible and is federally funded.";

        assertEquals(expectedFte, result.get("fte"));
        assertEquals(expectedFteZeroReason, result.get("fteZeroReason"));
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithIndependentSchoolAndInvalidBandCode_CallsNextFte() {
        // Given
        String schoolCategory = "INDEPEND";
        String bandCodeValue = "BAND_CODE_INVALID"; // Invalid band code value
        LocalDateTime effectiveDate = LocalDateTime.now().plusDays(1); // One day ahead of now
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(2); // Two days ahead of now

        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

        BandCodeEntity bandCode = new BandCodeEntity();
        bandCode.setEffectiveDate(effectiveDate);
        bandCode.setExpiryDate(expiryDate);

        when(bandCodeRepository.findById(bandCodeValue)).thenReturn(Optional.of(bandCode));

        // When
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        Map<String, Object> result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.get("fte"), result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_WithIndependentSchoolAndFundingCode20_ReturnsFte() {
        // Given
        String schoolCategory = "INDEPEND";
        String bandCodeValue = "BAND_CODE_INVALID";
        String fundingcode = "20";
        LocalDateTime effectiveDate = LocalDateTime.now().plusDays(1); // One day ahead of now
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(2); // Two days ahead of now

        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingcode);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

        BandCodeEntity bandCode = new BandCodeEntity();
        bandCode.setEffectiveDate(effectiveDate);
        bandCode.setExpiryDate(expiryDate);

        when(bandCodeRepository.findById(bandCodeValue)).thenReturn(Optional.of(bandCode));

        // When
        Map<String, Object> result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = "The student is Nominal Roll eligible and is federally funded.";

        assertEquals(expectedFte, result.get("fte"));
        assertEquals(expectedFteZeroReason, result.get("fteZeroReason"));
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithNonIndependentSchool_CallsNextFte() {
        // Given
        String schoolCategory = "DISTRICT"; // Non-independent school facility type code
        String bandCodeValue = "BAND_CODE_3";
        String fundingCode = "20";
        LocalDateTime effectiveDate = LocalDateTime.now().minusDays(1); // One day ago
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(1); // One day ahead of now

        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingCode);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

        BandCodeEntity bandCode = new BandCodeEntity();
        bandCode.setEffectiveDate(effectiveDate);
        bandCode.setExpiryDate(expiryDate);

        when(bandCodeRepository.findById(bandCodeValue)).thenReturn(Optional.of(bandCode));

        // When
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        Map<String, Object> result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.get("fte"), result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_WithInvalidBandCode_CallsNextFte() {
        // Given
        String schoolCategory = "INDEPEND";
        String bandCodeValue = "UNKNOWN_BAND_CODE";

        School school = new School();
        school.setSchoolCategoryCode(schoolCategory);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

        when(bandCodeRepository.findById(bandCodeValue)).thenReturn(Optional.empty()); // Return empty optional for unknown band code

        // When
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        Map<String, Object> result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.get("fte"), result.get("fte"));
        assertNull(result.get("fteZeroReason"));
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
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        Map<String, Object> result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.get("fte"), result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_WithNullSchool_CallsNextFte() {
        // Given
        String bandCodeValue = "BAND_CODE_3";
        String fundingCode = "20";
        LocalDateTime effectiveDate = LocalDateTime.now().minusDays(1); // One day ago
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(1); // One day ahead of now

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setBandCode(bandCodeValue);
        student.setSchoolFundingCode(fundingCode);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        BandCodeEntity bandCode = new BandCodeEntity();
        bandCode.setEffectiveDate(effectiveDate);
        bandCode.setExpiryDate(expiryDate);

        when(bandCodeRepository.findById(bandCodeValue)).thenReturn(Optional.of(bandCode));

        // When
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        Map<String, Object> result = independentSchoolAndBandCodeCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.get("fte"), result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator).calculateFte(studentData);
    }
}