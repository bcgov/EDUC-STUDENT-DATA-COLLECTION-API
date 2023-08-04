package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

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
            "KH, 0.4529",
            "KF, 0.9529",
            "01, 0.9529",
            "02, 0.9529",
            "03, 0.9529",
            "04, 0.9529",
            "05, 0.9529",
            "06, 0.9529",
            "07, 0.9529",
            "EU, 0.9529",
            "08, 0.75",
            "09, 0.75"
    })
    void testCalculateFte_homeSchoolStudentIsNowOnlineKto9Student_ShouldCalculateFteCorrectly(String enrolledGradeCode, String expectedResult) {
        // Given
        SdcSchoolCollectionStudent sdcSchoolCollectionStudent = new SdcSchoolCollectionStudent();
        sdcSchoolCollectionStudent.setEnrolledGradeCode(enrolledGradeCode);
        sdcSchoolCollectionStudent.setNumberOfCourses("2");
        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(sdcSchoolCollectionStudent);

        when(fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9Student(studentData)).thenReturn(true);

        // When
        FteCalculationResult result = newOnlineStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(new BigDecimal(expectedResult), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_numCoursesIsNull() {
        // Given
        SdcSchoolCollectionStudent sdcSchoolCollectionStudent = new SdcSchoolCollectionStudent();
        sdcSchoolCollectionStudent.setEnrolledGradeCode("08");
        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(sdcSchoolCollectionStudent);

        when(fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9Student(studentData)).thenReturn(true);

        // When
        FteCalculationResult result = newOnlineStudentCalculator.calculateFte(studentData);

        // Then
        assertEquals(new BigDecimal("0.5"), result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_homeSchoolStudentIsNotOnlineKto9Student() {
        // Given
        SdcStudentSagaData studentData = new SdcStudentSagaData();

        when(fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9Student(studentData)).thenReturn(false);

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
