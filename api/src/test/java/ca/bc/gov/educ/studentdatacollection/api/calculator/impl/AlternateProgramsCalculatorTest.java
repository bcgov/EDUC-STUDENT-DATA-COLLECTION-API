package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class AlternateProgramsCalculatorTest {

    private FteCalculator nextCalculator;
    private AlternateProgramsCalculator alternateProgramsCalculator;

    @BeforeEach
    public void setUp() {
        nextCalculator = mock(FteCalculator.class);
        alternateProgramsCalculator = new AlternateProgramsCalculator();
        alternateProgramsCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_KfTo7EuGradeCode() {
        // Given
        String enrolledGradeCode = SchoolGradeCodes.GRADE07.getCode();

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsGraduated(true);
        student.setEnrolledGradeCode(enrolledGradeCode);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = alternateProgramsCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ONE, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_NonGraduatedStudentWithNonAlternateProgramAndGradeNotInList() {
        // Given
        String enrolledGradeCode = SchoolGradeCodes.GRADE09.getCode();

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsGraduated(false);
        student.setEnrolledGradeCode(enrolledGradeCode);

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(schoolTombstone);

        // When
        FteCalculationResult fteValues = new FteCalculationResult();
        fteValues.setFte(BigDecimal.ZERO);
        fteValues.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(fteValues);
        FteCalculationResult result = alternateProgramsCalculator.calculateFte(studentData);

        // Then
        assertEquals(fteValues.getFte(), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_GraduatedStudentWithAlternateProgramAndGradeNotInList() {
        // Given
        String enrolledGradeCode = SchoolGradeCodes.GRADE09.getCode();

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsGraduated(true);
        student.setEnrolledGradeCode(enrolledGradeCode);

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.ALT_PROGS.getCode());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(schoolTombstone);

        // When
        FteCalculationResult fteValues = new FteCalculationResult();
        fteValues.setFte(BigDecimal.ZERO);
        fteValues.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(fteValues);
        FteCalculationResult result = alternateProgramsCalculator.calculateFte(studentData);

        // Then
        assertEquals(fteValues.getFte(), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_NullSchool() {
        // Given
        String enrolledGradeCode = SchoolGradeCodes.GRADE09.getCode();

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsGraduated(true);
        student.setEnrolledGradeCode(enrolledGradeCode);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult fteValues = new FteCalculationResult();
        fteValues.setFte(BigDecimal.ZERO);
        fteValues.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(fteValues);
        FteCalculationResult result = alternateProgramsCalculator.calculateFte(studentData);

        // Then
        assertEquals(fteValues.getFte(), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_NullIsGraduated() {
        // Given
        String enrolledGradeCode = SchoolGradeCodes.GRADE09.getCode();

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.ALT_PROGS.getCode());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(schoolTombstone);

        // When
        FteCalculationResult result = alternateProgramsCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ONE, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_IsNotGraduatedIsAltProgramGradeNotInList() {
        // Given
        String enrolledGradeCode = SchoolGradeCodes.GRADE09.getCode();

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setIsGraduated(false);

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.ALT_PROGS.getCode());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(schoolTombstone);

        // When
        FteCalculationResult result = alternateProgramsCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ONE, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_IsNotGraduatedIsAltProgramGradeHS() {
        // Given
        String enrolledGradeCode = SchoolGradeCodes.HOMESCHOOL.getCode();

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setIsGraduated(true);

        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.ALT_PROGS.getCode());

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(schoolTombstone);

        // When
        FteCalculationResult fteValues = new FteCalculationResult();
        fteValues.setFte(BigDecimal.ZERO);
        fteValues.setFteZeroReason(null);
        when(nextCalculator.calculateFte(any())).thenReturn(fteValues);
        FteCalculationResult result = alternateProgramsCalculator.calculateFte(studentData);

        // Then
        assertEquals(fteValues.getFte(), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator).calculateFte(studentData);
    }
}

