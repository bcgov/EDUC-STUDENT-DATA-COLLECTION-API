package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GraduatedAdultIndySchoolCalculatorTest {

    private FteCalculator nextCalculator;

    private GraduatedAdultIndySchoolCalculator graduatedAdultIndySchoolCalculator;

    @BeforeEach
    void setUp() {
        nextCalculator = mock(FteCalculator.class);
        graduatedAdultIndySchoolCalculator = new GraduatedAdultIndySchoolCalculator();
        graduatedAdultIndySchoolCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_GraduatedAdultIndySchool() {
        // Given
        School school = new School();
        school.setSchoolCategoryCode("INDEPEND");

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode("GA");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = graduatedAdultIndySchoolCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.getFte());
        assertEquals(ZeroFteReasonCodes.GRADUATED_ADULT_IND_AUTH.getCode(), result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_NonGraduatedAdultIndySchool() {
        // Given
        School school = new School();
        school.setSchoolCategoryCode("INDEPEND");

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode("12");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(studentData)).thenReturn(expectedResult);
        FteCalculationResult result = graduatedAdultIndySchoolCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_NonGraduatedNonIndySchool() {
        // Given
        School school = new School();
        school.setSchoolCategoryCode("DIST_ONLINE");

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode("GA");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(studentData)).thenReturn(expectedResult);
        FteCalculationResult result = graduatedAdultIndySchoolCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_NonGraduatedNullSchool() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode("GA");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);
        when(nextCalculator.calculateFte(studentData)).thenReturn(expectedResult);
        FteCalculationResult result = graduatedAdultIndySchoolCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }
}

