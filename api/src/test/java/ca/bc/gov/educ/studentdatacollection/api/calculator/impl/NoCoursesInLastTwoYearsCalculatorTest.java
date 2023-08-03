package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

class NoCoursesInLastTwoYearsCalculatorTest {

    @Mock
    private SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    @Mock
    private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    @Mock
    private FteCalculator nextCalculator;
    @InjectMocks
    private NoCoursesInLastTwoYearsCalculator noCoursesInLastTwoYearsCalculator;
    @InjectMocks
    private FteCalculatorUtils fteCalculatorUtils;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        noCoursesInLastTwoYearsCalculator.setNext(nextCalculator);
    }

    /*@Test
    void testCalculateFte_WithEightPlusGradeCodeAndNoCoursesInLastTwoYears_ShouldReturnZeroFte() {
        // Given
        UUID schoolId = UUID.randomUUID();
        String studentCreateDate = LocalDateTime.now().toString();
        String enrolledGradeCode = "10";
        String facilityTypeCode = "DIST_LEARN";
        String numberOfCourses = "0";

        School school = new School();
        school.setSchoolId(schoolId.toString());
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionEntity schoolCollection1 = new SdcSchoolCollectionEntity();
        schoolCollection1.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection1.setSchoolID(schoolId);
        schoolCollection1.setCreateDate(LocalDateTime.parse(studentCreateDate).minusYears(1)); // One year ago

        SdcSchoolCollectionEntity schoolCollection2 = new SdcSchoolCollectionEntity();
        schoolCollection2.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection2.setSchoolID(schoolId);
        schoolCollection2.setCreateDate(LocalDateTime.parse(studentCreateDate).minusYears(2)); // Two years ago

        List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = Arrays.asList(schoolCollection1, schoolCollection2);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setCreateDate(studentCreateDate);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses(numberOfCourses);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

        when(sdcSchoolCollectionRepository.findAllBySchoolIDAndCreateDateBetween(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(lastTwoYearsOfCollections));
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        Map<String, Object> result = noCoursesInLastTwoYearsCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.get("fte"));
        assertEquals("The student has not been reported as \"active\" in a new course in the last two years.", result.get("fteZeroReason"));
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_WithEightPlusGradeCodeAndOneCourseInLastTwoYears_CallsNextCalculator() {
        // Given
        UUID schoolId = UUID.randomUUID();
        String studentCreateDate = LocalDateTime.now().toString();
        String enrolledGradeCode = "10";
        String facilityTypeCode = "DIST_LEARN";
        String numberOfCourses = "0";

        School school = new School();
        school.setSchoolId(schoolId.toString());
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionEntity schoolCollection1 = new SdcSchoolCollectionEntity();
        schoolCollection1.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection1.setSchoolID(schoolId);
        schoolCollection1.setCreateDate(LocalDateTime.parse(studentCreateDate).minusYears(1)); // One year ago

        SdcSchoolCollectionEntity schoolCollection2 = new SdcSchoolCollectionEntity();
        schoolCollection2.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection2.setSchoolID(schoolId);
        schoolCollection2.setCreateDate(LocalDateTime.parse(studentCreateDate).minusYears(2)); // Two years ago

        List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = Arrays.asList(schoolCollection1, schoolCollection2);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setCreateDate(studentCreateDate);
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses(numberOfCourses);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

        when(sdcSchoolCollectionRepository.findAllBySchoolIDAndCreateDateBetween(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(lastTwoYearsOfCollections));
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(anyList(), any(String.class)))
                .thenReturn(1L);

        // When
        Map<String, Object> fteValues = new HashMap<>();
        fteValues.put("fte", BigDecimal.ONE);
        fteValues.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(any())).thenReturn(fteValues);
        Map<String, Object> result = noCoursesInLastTwoYearsCalculator.calculateFte(studentData);

        // Then
        assertEquals(fteValues.get("fte"), result.get("fte"));
        assertNull(result.get("fteZeroReason"));
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_WithNonEightPlusGradeCodeAndNoCoursesInLastTwoYears_ShouldCallNextCalculator() {
        // Given
        String enrolledGradeCode = "07";
        String facilityTypeCode = "DIST_LEARN";
        String numberOfCourses = "0";

        School school = new School();
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setEnrolledGradeCode(enrolledGradeCode);
        student.setNumberOfCourses(numberOfCourses);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

        // When
        BigDecimal expectedFte = BigDecimal.valueOf(0.7);
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("fte", expectedFte);
        expectedResult.put("fteZeroReason", null);
        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);

        Map<String, Object> result = noCoursesInLastTwoYearsCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult.get("fte"), result.get("fte"));
        assertEquals(expectedResult.get("fteZeroReason"), result.get("fteZeroReason"));
        verify(nextCalculator).calculateFte(studentData);
    }

    @Test
    void testCalculateFte_NumberOfCoursesNull_ShouldReturnZeroFte() {
        // Given
        UUID schoolId = UUID.randomUUID();
        String studentCreateDate = LocalDateTime.now().toString();
        String enrolledGradeCode = "10";
        String facilityTypeCode = "DIST_LEARN";

        School school = new School();
        school.setSchoolId(schoolId.toString());
        school.setFacilityTypeCode(facilityTypeCode);

        SdcSchoolCollectionEntity schoolCollection1 = new SdcSchoolCollectionEntity();
        schoolCollection1.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection1.setSchoolID(schoolId);
        schoolCollection1.setCreateDate(LocalDateTime.parse(studentCreateDate).minusYears(1)); // One year ago

        SdcSchoolCollectionEntity schoolCollection2 = new SdcSchoolCollectionEntity();
        schoolCollection2.setSdcSchoolCollectionID(UUID.randomUUID());
        schoolCollection2.setSchoolID(schoolId);
        schoolCollection2.setCreateDate(LocalDateTime.parse(studentCreateDate).minusYears(2)); // Two years ago

        List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = Arrays.asList(schoolCollection1, schoolCollection2);

        SdcSchoolCollectionStudent student = new SdcSchoolCollectionStudent();
        student.setCreateDate(studentCreateDate);
        student.setEnrolledGradeCode(enrolledGradeCode);

        SdcStudentSagaData studentData = new SdcStudentSagaData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudent(student);

        when(sdcSchoolCollectionRepository.findAllBySchoolIDAndCreateDateBetween(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(lastTwoYearsOfCollections));
        when(sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(anyList(), any(String.class)))
                .thenReturn(0L);

        // When
        Map<String, Object> result = noCoursesInLastTwoYearsCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.get("fte"));
        assertEquals("The student has not been reported as \"active\" in a new course in the last two years.", result.get("fteZeroReason"));
        verify(nextCalculator, never()).calculateFte(any());
    }*/
}
