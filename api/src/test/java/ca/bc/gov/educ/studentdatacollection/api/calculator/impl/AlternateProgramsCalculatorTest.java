package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

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

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsGraduated("true");
        student.setEnrolledGradeCode(enrolledGradeCode);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

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
        String enrolledGradeCode = SchoolGradeCodes.HOMESCHOOL.getCode();

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsGraduated("false");
        student.setEnrolledGradeCode(enrolledGradeCode);

        School school = new School();
        school.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);
        studentData.setSchool(school);

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
        String enrolledGradeCode = SchoolGradeCodes.HOMESCHOOL.getCode();

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsGraduated("true");
        student.setEnrolledGradeCode(enrolledGradeCode);

        School school = new School();
        school.setFacilityTypeCode(FacilityTypeCodes.ALT_PROGS.getCode());

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);
        studentData.setSchool(school);

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
        String enrolledGradeCode = SchoolGradeCodes.HOMESCHOOL.getCode();

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setIsGraduated("true");
        student.setEnrolledGradeCode(enrolledGradeCode);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

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
        String enrolledGradeCode = SchoolGradeCodes.HOMESCHOOL.getCode();

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode(enrolledGradeCode);

        School school = new School();
        school.setFacilityTypeCode(FacilityTypeCodes.ALT_PROGS.getCode());

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);
        studentData.setSchool(school);

        // When
        FteCalculationResult result = alternateProgramsCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ONE, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }
}

