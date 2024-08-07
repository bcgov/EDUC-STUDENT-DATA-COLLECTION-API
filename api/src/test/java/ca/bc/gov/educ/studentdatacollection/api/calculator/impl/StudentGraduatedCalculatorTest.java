package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class StudentGraduatedCalculatorTest {

    private StudentGraduatedCalculator studentGraduatedCalculator;
    private FteCalculator nextCalculator;

    @BeforeEach
    void setup() {
        nextCalculator = mock(FteCalculator.class);
        studentGraduatedCalculator = new StudentGraduatedCalculator();
        studentGraduatedCalculator.setNext(nextCalculator);
    }

    @ParameterizedTest
    @CsvSource({
            "true, 0500, 0.625",
            "false, 0500, 0.875",
            "false, 0800, 1",
            "false, , 0.25"
    })
    void testCalculateFte_StudentGraduated_ThenFteCalculatedWithoutSupportBlocks(boolean isGraduated, String numberOfCourses, String expectedResults) {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsGraduated(isGraduated);
        student.setNumberOfCourses(numberOfCourses);
        student.setSupportBlocks("2");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(createSchool());

        // When
        FteCalculationResult result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal(expectedResults);
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentNotGraduatedWithEmptyCourses_ThenFteCalculatedWithSupportBlocks() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsGraduated(false);
        student.setNumberOfCourses("");
        student.setSupportBlocks("2");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.25");
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentNotGraduatedWithCourses_ThenFteCalculatedWithSupportBlocks() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setIsGraduated(true);
        student.setNumberOfCourses("900");
        student.setSupportBlocks("2");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        var school = createSchool();
        school.setSchoolCategoryCode("INDEPEND");
        studentData.setSchool(school);

        // When
        FteCalculationResult result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("1");
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentWithNullGraduation_ThenFteCalculatedWithSupportBlocks() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setNumberOfCourses("0500");
        student.setSupportBlocks("2");

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);

        // When
        FteCalculationResult result = studentGraduatedCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0.875");
        assertEquals(expectedFte, result.getFte());
        assertNull(result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    private SchoolTombstone createSchool(){
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode("PUBLIC");
        schoolTombstone.setFacilityTypeCode("STANDARD");
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        return schoolTombstone;
    }
}
