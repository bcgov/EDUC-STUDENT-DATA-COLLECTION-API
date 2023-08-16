package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
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

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode("GA");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

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

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode("12");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

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

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode("GA");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

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
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode("GA");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

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

