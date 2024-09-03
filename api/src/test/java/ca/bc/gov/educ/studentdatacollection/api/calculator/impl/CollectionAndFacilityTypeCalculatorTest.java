package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CollectionAndFacilityTypeCalculatorTest extends BaseStudentDataCollectionAPITest {
    private FteCalculator nextCalculator;
    private CollectionAndFacilityTypeCalculator collectionAndFacilityTypeCalculator;
    private FteCalculatorUtils fteCalculatorUtils;

    @BeforeEach
    public void setUp() {
        nextCalculator = mock(FteCalculator.class);
        fteCalculatorUtils = mock(FteCalculatorUtils.class);

        collectionAndFacilityTypeCalculator = new CollectionAndFacilityTypeCalculator();
        collectionAndFacilityTypeCalculator.setNext(nextCalculator);
        collectionAndFacilityTypeCalculator.fteCalculatorUtils = fteCalculatorUtils;
    }

    @Test
    void testCalculateFte_JulyCollectionAndFacilityTypeDifferentThanSummerSchool_StudentIncludedInDistrictThisSchoolYearNonZeroFteAndSchoolNotOnline_ReturnsFteCalculation() {
        // Given
        District district = createMockDistrict();
        SchoolTombstone school = createMockSchool();
        school.setDistrictId(district.getDistrictId());
        school.setFacilityTypeCode(FacilityTypeCodes.PROVINCIAL.getCode());

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setAssignedStudentId(UUID.randomUUID());
        student.setFte(BigDecimal.TEN);

        CollectionEntity collection = createMockCollectionEntity();
        var newSnapDate = LocalDate.of(LocalDateTime.now().getYear(), Month.JULY, 29);
        collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
        collection.setSnapshotDate(newSnapDate);
        SdcSchoolCollectionEntity sdcSchoolCollectionEntityNew = createMockSdcSchoolCollectionEntity(collection, null);
        sdcSchoolCollectionEntityNew.setSdcSchoolStudentEntities(Collections.singleton(student));
        sdcSchoolCollectionEntityNew.setSchoolID(UUID.fromString(school.getSchoolId()));

        student.setSdcSchoolCollection(sdcSchoolCollectionEntityNew);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        when(fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline(studentData)).thenReturn(true);
        when(fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(studentData)).thenReturn(false);
        when(fteCalculatorUtils.reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(studentData)).thenReturn(true);

        // When
        FteCalculationResult result = collectionAndFacilityTypeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0");

        assertEquals(expectedFte, result.getFte());
        assertEquals("The district has already received funding for the student this year.", result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_JulyCollectionAndFacilityTypeDifferentThanSummerSchool_StudentIncludedInDistrictThisSchoolYearNonZeroFteAndSchoolOnlineKto9_ReturnsFteCalculation() {
        // Given
        District district = createMockDistrict();
        SchoolTombstone school = createMockSchool();
        school.setDistrictId(district.getDistrictId());
        school.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setAssignedStudentId(UUID.randomUUID());
        student.setFte(BigDecimal.TEN);
        student.setEnrolledGradeCode("08");

        CollectionEntity collection = createMockCollectionEntity();
        var newSnapDate = LocalDate.of(LocalDateTime.now().getYear(), Month.JULY, 29);
        collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
        collection.setSnapshotDate(newSnapDate);
        SdcSchoolCollectionEntity sdcSchoolCollectionEntityNew = createMockSdcSchoolCollectionEntity(collection, null);
        sdcSchoolCollectionEntityNew.setSdcSchoolStudentEntities(Collections.singleton(student));
        sdcSchoolCollectionEntityNew.setSchoolID(UUID.fromString(school.getSchoolId()));

        student.setSdcSchoolCollection(sdcSchoolCollectionEntityNew);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        when(fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline(studentData)).thenReturn(false);
        when(fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(studentData)).thenReturn(true);
        when(fteCalculatorUtils.reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(studentData)).thenReturn(true);

        // When
        FteCalculationResult result = collectionAndFacilityTypeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0");

        assertEquals(expectedFte, result.getFte());
        assertEquals("The district has already received funding for the student this year.", result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_JulyCollectionAndFacilityTypeDifferentThanSummerSchool_NotReportedInOnlineSchoolInThisCollectionOrPrevious_ReturnsFteCalculation() {
        // Given
        District district = createMockDistrict();
        SchoolTombstone school = createMockSchool();
        school.setDistrictId(district.getDistrictId());
        school.setFacilityTypeCode(FacilityTypeCodes.PROVINCIAL.getCode());

        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        student.setAssignedStudentId(UUID.randomUUID());
        student.setFte(BigDecimal.TEN);

        CollectionEntity collection = createMockCollectionEntity();
        var newSnapDate = LocalDate.of(LocalDateTime.now().getYear(), Month.JULY, 29);
        collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
        collection.setSnapshotDate(newSnapDate);
        SdcSchoolCollectionEntity sdcSchoolCollectionEntityNew = createMockSdcSchoolCollectionEntity(collection, null);
        sdcSchoolCollectionEntityNew.setSdcSchoolStudentEntities(Collections.singleton(student));
        sdcSchoolCollectionEntityNew.setSchoolID(UUID.fromString(school.getSchoolId()));

        student.setSdcSchoolCollection(sdcSchoolCollectionEntityNew);

        StudentRuleData studentData = new StudentRuleData();
        studentData.setSchool(school);
        studentData.setSdcSchoolCollectionStudentEntity(student);

        when(fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline(studentData)).thenReturn(false);
        when(fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(studentData)).thenReturn(false);
        when(fteCalculatorUtils.reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(studentData)).thenReturn(false);

        // When
        FteCalculationResult result = collectionAndFacilityTypeCalculator.calculateFte(studentData);

        // Then
        BigDecimal expectedFte = new BigDecimal("0");

        assertEquals(expectedFte, result.getFte());
        assertEquals("None of student's educational program was delivered through online learning this year.", result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }
}