package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class SupportBlocksCalculatorTest {

    private SupportBlocksCalculator supportBlocksCalculator;
    private FteCalculator nextCalculator;

    @BeforeEach
    public void setup() {
        nextCalculator = mock(FteCalculator.class);
        supportBlocksCalculator = new SupportBlocksCalculator();
        supportBlocksCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_WithSupportBlocks_ThenNextCalculatorCalled() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSupportBlocks("1");
        student.setNumberOfCourses("6");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ZERO);
        expectedResult.setFteZeroReason(null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_WithSupportBlocksIndy_ThenNextCalculatorCalled() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSupportBlocks("0");
        student.setNumberOfCourses("900");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        var school = createSchool();
        school.setSchoolCategoryCode("INDEPEND");
        studentData.setSchool(school);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.getFte(), result.getFte());
    }

    @Test
    void testCalculateFte_WithoutSupportBlocks_ThenFteReturned() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSupportBlocks("0");
        student.setNumberOfCourses("0500");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(createSchool());

        // When
        FteCalculationResult result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.625");
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithoutEmptySupportBlocks_ThenFteReturned() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSupportBlocks("");
        student.setNumberOfCourses("0500");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(createSchool());

        // When
        FteCalculationResult result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.625");
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithNullSupportBlocks_ThenFteReturned() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setNumberOfCourses("0500");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(createSchool());

        // When
        FteCalculationResult result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.625");
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }


    @Test
    void testCalculateFte_WithoutSupportBlocksAndEmptyCourses_ThenFteReturned() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSupportBlocks("0");
        student.setNumberOfCourses("");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(createSchool());

        // When
        FteCalculationResult result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        assertEquals(expectedFte, result.getFte());
        assertEquals(ZeroFteReasonCodes.NUM_COURSES.getCode(), result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithoutSupportBlocksAndNullCourses_ThenFteReturned() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setSupportBlocks("0");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(createSchool());

        // When
        FteCalculationResult result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        assertEquals(expectedFte, result.getFte());
        assertEquals(ZeroFteReasonCodes.NUM_COURSES.getCode(), result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    private SchoolTombstone createSchool(){
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("STANDARD");
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        return schoolTombstone;
    }
}
