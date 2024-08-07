package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes.DISTRICT_DUPLICATE_FUNDING;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes.IND_AUTH_DUPLICATE_FUNDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NewOnlineStudentCalculatorTest {
    private FteCalculator nextCalculator;
    private NewOnlineStudentCalculator newOnlineStudentCalculator;
    private FteCalculatorUtils fteCalculatorUtils;

    @BeforeEach
    public void setUp() {
        nextCalculator = mock(FteCalculator.class);
        fteCalculatorUtils = mock(FteCalculatorUtils.class);

        newOnlineStudentCalculator = new NewOnlineStudentCalculator();
        newOnlineStudentCalculator.setNext(nextCalculator);
        newOnlineStudentCalculator.fteCalculatorUtils = fteCalculatorUtils;
    }

    @ParameterizedTest
    @CsvSource({
            "KH, 0200, 0.4529",
            "KF, 0200, 0.9529",
            "01, 0200, 0.9529",
            "02, 0200, 0.9529",
            "03, 0200, 0.9529",
            "04, 0200, 0.9529",
            "05, 0200, 0.9529",
            "06, 0200, 0.9529",
            "07, 0200, 0.9529",
            "EU, 0200, 0.9529",
            "08, 0200, 0.75",
            "09, 0200, 0.75",
            "HS, 0200, 0",
            "08, 1200, 0.9529"
    })
    void testCalculateFte_homeSchoolStudentIsNowOnlineKto9StudentOrHs_publicDis_ShouldCalculateFteCorrectly(String enrolledGradeCode, String numberOfCourses, String expectedResult) {
        // Given
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudent = new SdcSchoolCollectionStudentEntity();
        sdcSchoolCollectionStudent.setEnrolledGradeCode(enrolledGradeCode);
        sdcSchoolCollectionStudent.setNumberOfCourses(numberOfCourses);
        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudent);
        studentData.setSchool(schoolTombstone);

        when(fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9StudentOrHs(studentData)).thenReturn(true);

        // When
        FteCalculationResult result = newOnlineStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(new BigDecimal(expectedResult), result.getFte());
        if (sdcSchoolCollectionStudent.getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode())) {
            assertEquals(result.getFteZeroReason(), DISTRICT_DUPLICATE_FUNDING.getCode());
        } else {
            assertNull(result.getFteZeroReason());
        }
        verify(nextCalculator, never()).calculateFte(any());
    }

    @ParameterizedTest
    @CsvSource({
            "KH, 0200, 0.4529",
            "KF, 0200, 0.9529",
            "01, 0200, 0.9529",
            "02, 0200, 0.9529",
            "03, 0200, 0.9529",
            "04, 0200, 0.9529",
            "05, 0200, 0.9529",
            "06, 0200, 0.9529",
            "07, 0200, 0.9529",
            "EU, 0200, 0.9529",
            "08, 0200, 0.75",
            "09, 0200, 0.75",
            "HS, 0200, 0",
            "08, 1200, 0.9529"
    })
    void testCalculateFte_homeSchoolStudentIsNowOnlineKto9StudentOrHs_indAuth_ShouldCalculateFteCorrectly(String enrolledGradeCode, String numberOfCourses, String expectedResult) {
        // Given
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
        SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudent = new SdcSchoolCollectionStudentEntity();
        sdcSchoolCollectionStudent.setEnrolledGradeCode(enrolledGradeCode);
        sdcSchoolCollectionStudent.setNumberOfCourses(numberOfCourses);
        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudent);
        studentData.setSchool(schoolTombstone);

        when(fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9StudentOrHs(studentData)).thenReturn(true);

        // When
        FteCalculationResult result = newOnlineStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(new BigDecimal(expectedResult), result.getFte());
        if (sdcSchoolCollectionStudent.getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode())) {
            assertEquals(result.getFteZeroReason(), IND_AUTH_DUPLICATE_FUNDING.getCode());
        } else {
            assertNull(result.getFteZeroReason());
        }
        verify(nextCalculator, never()).calculateFte(any());
    }


    @Test
    void testCalculateFte_numCoursesIsNull() {
        // Given
        SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudent = new SdcSchoolCollectionStudentEntity();
        sdcSchoolCollectionStudent.setEnrolledGradeCode("08");
        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudent);

        when(fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9StudentOrHs(studentData)).thenReturn(true);

        // When
        FteCalculationResult result = newOnlineStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(new BigDecimal("0.5"), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_homeSchoolStudentIsNotOnlineKto9StudentOrHs() {
        // Given
        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(new SdcSchoolCollectionStudentEntity());

        when(fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9StudentOrHs(studentData)).thenReturn(false);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = newOnlineStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }
}
