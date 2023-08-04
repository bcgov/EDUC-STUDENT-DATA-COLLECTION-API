package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.util.SchoolYear;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

class TooYoungCalculatorTest {

    private TooYoungCalculator tooYoungCalculator;
    private FteCalculator nextCalculator;

    @BeforeEach
    public void setup() {
        nextCalculator = mock(FteCalculator.class);
        tooYoungCalculator = new TooYoungCalculator();
        tooYoungCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_StudentTooYoung() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
        student.setDob(format.format(LocalDateTime.now().minusYears(4)));
        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult result = tooYoungCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.getFte());
        assertEquals("The student is too young.", result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentOldEnough() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
        student.setDob(format.format(LocalDateTime.now().minusYears(5)));

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = tooYoungCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_StudentExactlyOldEnough() {
        // Given
        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");

        SchoolYear schoolYear = new SchoolYear();
        LocalDate dob = LocalDate.parse(schoolYear.getStartDate().getYear() + "-12-31").minusYears(5);
        student.setDob(format.format(dob));

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = tooYoungCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }
}