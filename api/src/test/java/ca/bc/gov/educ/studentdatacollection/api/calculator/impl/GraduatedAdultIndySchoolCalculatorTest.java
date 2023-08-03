package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, Object> result = graduatedAdultIndySchoolCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.get("fte"));
        assertEquals("The student is graduated adult reported by an independent school.", result.get("fteZeroReason"));
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
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(studentData)).thenReturn(expectedResult);
        Map<String, Object> result = graduatedAdultIndySchoolCalculator.calculateFte(studentData);

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
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(studentData)).thenReturn(expectedResult);
        Map<String, Object> result = graduatedAdultIndySchoolCalculator.calculateFte(studentData);

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
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(studentData)).thenReturn(expectedResult);
        Map<String, Object> result = graduatedAdultIndySchoolCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }
}

