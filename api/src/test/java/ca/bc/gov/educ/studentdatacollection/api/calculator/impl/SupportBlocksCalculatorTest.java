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
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setSupportBlocks("1");
        student.setNumberOfCourses("6");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", BigDecimal.ZERO);
        expectedResult.put("fteZeroReason", null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        Map<String, Object> result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_WithoutSupportBlocks_ThenFteReturned() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setSupportBlocks("0");
        student.setNumberOfCourses("5");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.625");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithoutEmptySupportBlocks_ThenFteReturned() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setSupportBlocks("");
        student.setNumberOfCourses("5");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.625");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithNullSupportBlocks_ThenFteReturned() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setNumberOfCourses("5");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.625");
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithoutSupportBlocksAndEmptyCourses_ThenFteReturned() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setSupportBlocks("0");
        student.setNumberOfCourses("");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithoutSupportBlocksAndNullCourses_ThenFteReturned() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setSupportBlocks("0");

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        Map<String, Object> result = supportBlocksCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        assertEquals(expectedFte, result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator, never()).calculateFte(any());
    }
}