package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

class StudentOutOfProvinceCalculatorTest {

    private StudentOutOfProvinceCalculator studentOutOfProvinceCalculator;
    private FteCalculator nextCalculator;

    @BeforeEach
    public void setup() {
        nextCalculator = mock(FteCalculator.class);
        studentOutOfProvinceCalculator = new StudentOutOfProvinceCalculator();
        studentOutOfProvinceCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_StudentOutOfProvince_ThenReturnZeroFteAndReason() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setSchoolFundingCode("14");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = studentOutOfProvinceCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.get("fte"));
        assertEquals("Out-of-Province/International Students are not eligible for funding.", result.get("fteZeroReason"));

        // Ensure that the nextCalculator.calculateFte method is not called
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentInProvince_ThenNextFteCalculatorCalled() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setSchoolFundingCode("05");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        Map<String, Object> result = studentOutOfProvinceCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);

        // Ensure that the nextCalculator.calculateFte method is called with the correct studentData
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_NullSchoolFundingCode_ThenNextFteCalculatorIsCalled() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setSchoolFundingCode(null);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ONE);
        expectedResult.put("fteZeroReason", null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        Map<String, Object> result = studentOutOfProvinceCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);

        // Ensure that the nextCalculator.calculateFte method is called with the correct studentData
        verify(nextCalculator).calculateFte(studentData);
    }
}