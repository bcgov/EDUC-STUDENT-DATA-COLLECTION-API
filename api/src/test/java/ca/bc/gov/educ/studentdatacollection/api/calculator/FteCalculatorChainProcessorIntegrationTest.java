package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.StudentMerge;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class FteCalculatorChainProcessorIntegrationTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private FteCalculatorChainProcessor fteCalculatorChainProcessor;
    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    @Autowired
    SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    @Autowired
    CollectionRepository collectionRepository;
    @Autowired
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private StudentRuleData studentData;
    @Autowired
    RestUtils restUtils;

    @BeforeEach
    public void setUp() throws IOException {
        final File file = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("sdc-student-saga-data.json")).getFile()
        );
        studentData = new ObjectMapper().readValue(file, StudentRuleData.class);
        CollectionEntity collection = new CollectionEntity();
        collection.setCollectionID(UUID.randomUUID());
        collection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collection.setSnapshotDate(LocalDate.now());
        SdcSchoolCollectionEntity sdcSchoolCollection = new SdcSchoolCollectionEntity();
        sdcSchoolCollection.setCollectionEntity(collection);
        studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollection);
    }

    @Test
    void testProcessFteCalculator_OffshoreStudent() {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("OFFSHORE");

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(this.studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.OFFSHORE.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_StudentOutOfProvince() {
        // Given
        this.studentData.getSdcSchoolCollectionStudentEntity().setSchoolFundingCode("14");

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.OUT_OF_PROVINCE.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_TooYoung() {
        // Given
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
        this.studentData.getSdcSchoolCollectionStudentEntity().setDob(format.format(LocalDate.now().minusYears(3)));

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.TOO_YOUNG.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_GraduatedAdultIndySchool() {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("INDEPEND");
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("GA");

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.GRADUATED_ADULT_IND_AUTH.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_IndependentSchoolAndBandCode() {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("INDEPEND");
        this.studentData.getSdcSchoolCollectionStudentEntity().setBandCode("");
        this.studentData.getSdcSchoolCollectionStudentEntity().setSchoolFundingCode("20");

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.NOMINAL_ROLL_ELIGIBLE.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_NoCoursesInLastTwoYearsSchoolAged() {
        // Given
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0000");
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsSchoolAged(true);
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("10");
        this.studentData.getSdcSchoolCollectionStudentEntity().setCreateDate(LocalDateTime.now());

        CollectionEntity collection = createMockCollectionEntity();
        SchoolTombstone school = createMockSchool();
        SdcSchoolCollectionEntity sdcSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcSchoolCollection.setCreateDate(LocalDateTime.now().minusYears(1));

        collectionRepository.save(collection);

        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollection);
        this.studentData.getSchool().setSchoolId(sdcSchoolCollection.getSchoolID().toString());

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        StudentMerge studentMerge = getStudentMergeResult();
        penMatchResult.getMatchingRecords().get(0).setMatchingPEN(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedPen());
        penMatchResult.getMatchingRecords().get(0).setStudentID(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId().toString());
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        when(this.restUtils.getMergedStudentIds(any(), any())).thenReturn(List.of(studentMerge));
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.INACTIVE.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_NoCoursesInLastTwoYearsAdult() {
        // Given
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0000");
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsSchoolAged(false);
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsAdult(true);
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("10");
        this.studentData.getSdcSchoolCollectionStudentEntity().setCreateDate(LocalDateTime.now());

        CollectionEntity collection = createMockCollectionEntity();
        SchoolTombstone school = createMockSchool();
        SdcSchoolCollectionEntity sdcSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcSchoolCollection.setCreateDate(LocalDateTime.now().minusYears(1));

        collectionRepository.save(collection);

        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollection);
        this.studentData.getSchool().setSchoolId(sdcSchoolCollection.getSchoolID().toString());

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        StudentMerge studentMerge = getStudentMergeResult();
        penMatchResult.getMatchingRecords().get(0).setMatchingPEN(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedPen());
        penMatchResult.getMatchingRecords().get(0).setStudentID(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId().toString());
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        when(this.restUtils.getMergedStudentIds(any(), any())).thenReturn(List.of(studentMerge));
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.INACTIVE.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_NoCoursesZeroAdult() {
        // Given
        this.studentData.getSchool().setFacilityTypeCode("STANDARD");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0000");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCoursesDec(new BigDecimal(0.00));
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsAdult(true);
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("10");
        this.studentData.getSdcSchoolCollectionStudentEntity().setCreateDate(LocalDateTime.now());

        CollectionEntity collection = createMockCollectionEntity();
        SchoolTombstone school = createMockSchool();
        SdcSchoolCollectionEntity sdcSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcSchoolCollection.setCreateDate(LocalDateTime.now().minusYears(1));

        collectionRepository.save(collection);

        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollection);
        this.studentData.getSchool().setSchoolId(sdcSchoolCollection.getSchoolID().toString());

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        StudentMerge studentMerge = getStudentMergeResult();
        penMatchResult.getMatchingRecords().get(0).setMatchingPEN(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedPen());
        penMatchResult.getMatchingRecords().get(0).setStudentID(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId().toString());
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        when(this.restUtils.getMergedStudentIds(any(), any())).thenReturn(List.of(studentMerge));
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.ZERO_COURSES.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_NoCoursesZeroSchoolAged() {
        // Given
        this.studentData.getSchool().setFacilityTypeCode("STANDARD");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0000");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCoursesDec(new BigDecimal(0.00));
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsSchoolAged(true);
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("10");
        this.studentData.getSdcSchoolCollectionStudentEntity().setCreateDate(LocalDateTime.now());

        CollectionEntity collection = createMockCollectionEntity();
        SchoolTombstone school = createMockSchool();
        SdcSchoolCollectionEntity sdcSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcSchoolCollection.setCreateDate(LocalDateTime.now().minusYears(1));

        collectionRepository.save(collection);

        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollection);
        this.studentData.getSchool().setSchoolId(sdcSchoolCollection.getSchoolID().toString());

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        StudentMerge studentMerge = getStudentMergeResult();
        penMatchResult.getMatchingRecords().get(0).setMatchingPEN(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedPen());
        penMatchResult.getMatchingRecords().get(0).setStudentID(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId().toString());
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        when(this.restUtils.getMergedStudentIds(any(), any())).thenReturn(List.of(studentMerge));
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.ZERO_COURSES.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_DistrictDoubleReported() {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("PUBLIC");
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        CollectionEntity collectionOrig = createMockCollectionEntity();
        collectionOrig.setSnapshotDate(LocalDate.of(collectionOrig.getOpenDate().getYear(), 2, 15));
        collectionOrig.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionOrig = collectionRepository.save(collectionOrig);

        var districtID = UUID.randomUUID();
        var districtColl = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collectionOrig, districtID));

        SdcSchoolCollectionEntity sdcSchoolCollectionEntityOrig = createMockSdcSchoolCollectionEntity(collectionOrig, null);
        sdcSchoolCollectionEntityOrig.setSdcDistrictCollectionID(districtColl.getSdcDistrictCollectionID());
        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollectionEntityOrig);
        sdcSchoolCollectionEntityOrig = sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntityOrig);

        var lastCollectionDate = LocalDateTime.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 5, 0, 0);

        this.studentData.getSchool().setSchoolId(sdcSchoolCollectionEntityOrig.getSchoolID().toString());
        this.studentData.getSchool().setDistrictId(String.valueOf(districtID));
        SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = createMockSchoolStudentForSagaEntity(sdcSchoolCollectionEntityOrig);

        sdcSchoolCollectionStudentEntity.setEnrolledGradeCode("08");
        sdcSchoolCollectionStudentEntity.setCreateDate(LocalDateTime.of(LocalDateTime.now().getYear(), Month.JANUARY, 1, 0, 0));
        sdcSchoolCollectionStudentRepository.save(sdcSchoolCollectionStudentEntity);
        this.studentData.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentEntity);

        var oldCollection = createMockCollectionEntity();
        var oldSnapDate = LocalDate.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 29);
        oldCollection.setSnapshotDate(oldSnapDate);
        var oldSdcCollection = createMockSdcSchoolCollectionEntity(oldCollection, null);
        collectionRepository.save(oldCollection);
        var oldDistrictColl = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(oldCollection, districtID));
        oldSdcCollection.setSdcDistrictCollectionID(oldDistrictColl.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(oldSdcCollection);
        var oneYearAgoCollectionStudent = createMockSchoolStudentEntity(oldSdcCollection);
        oneYearAgoCollectionStudent.setCreateDate(lastCollectionDate);
        oneYearAgoCollectionStudent.setAssignedStudentId(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId());
        sdcSchoolCollectionStudentRepository.save(oneYearAgoCollectionStudent);

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        StudentMerge studentMerge = getStudentMergeResult();
        penMatchResult.getMatchingRecords().get(0).setMatchingPEN(oneYearAgoCollectionStudent.getAssignedPen());
        penMatchResult.getMatchingRecords().get(0).setStudentID(oneYearAgoCollectionStudent.getAssignedStudentId().toString());
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        when(this.restUtils.getMergedStudentIds(any(), any())).thenReturn(List.of(studentMerge));
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.DISTRICT_DUPLICATE_FUNDING.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_DistrictDoubleReported_GivenMergedStudent_ReturnsZeroFTE() {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("PUBLIC");
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        CollectionEntity collectionOrig = createMockCollectionEntity();
        collectionOrig.setSnapshotDate(LocalDate.of(collectionOrig.getOpenDate().getYear(), 2, 15));
        collectionOrig.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionOrig = collectionRepository.save(collectionOrig);

        var districtID = UUID.randomUUID();
        var districtColl = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collectionOrig, districtID));

        SdcSchoolCollectionEntity sdcSchoolCollectionEntityOrig = createMockSdcSchoolCollectionEntity(collectionOrig, null);
        sdcSchoolCollectionEntityOrig.setSdcDistrictCollectionID(districtColl.getSdcDistrictCollectionID());
        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollectionEntityOrig);
        sdcSchoolCollectionEntityOrig = sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntityOrig);

        var lastCollectionDate = LocalDateTime.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 5, 0, 0);

        this.studentData.getSchool().setSchoolId(sdcSchoolCollectionEntityOrig.getSchoolID().toString());
        this.studentData.getSchool().setDistrictId(String.valueOf(districtID));
        SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = createMockSchoolStudentForSagaEntity(sdcSchoolCollectionEntityOrig);

        sdcSchoolCollectionStudentEntity.setEnrolledGradeCode("08");
        sdcSchoolCollectionStudentEntity.setCreateDate(LocalDateTime.of(LocalDateTime.now().getYear(), Month.JANUARY, 1, 0, 0));
        sdcSchoolCollectionStudentRepository.save(sdcSchoolCollectionStudentEntity);
        this.studentData.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentEntity);

        var oldCollection = createMockCollectionEntity();
        var oldSnapDate = LocalDate.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 29);
        oldCollection.setSnapshotDate(oldSnapDate);
        var oldSdcCollection = createMockSdcSchoolCollectionEntity(oldCollection, null);
        collectionRepository.save(oldCollection);
        var oldDistrictColl = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(oldCollection, districtID));
        oldSdcCollection.setSdcDistrictCollectionID(oldDistrictColl.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(oldSdcCollection);
        var oneYearAgoCollectionStudent = createMockSchoolStudentEntity(oldSdcCollection);
        oneYearAgoCollectionStudent.setCreateDate(lastCollectionDate);
        var mergedStudentId = UUID.randomUUID();
        oneYearAgoCollectionStudent.setAssignedStudentId(mergedStudentId);
        sdcSchoolCollectionStudentRepository.save(oneYearAgoCollectionStudent);

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        StudentMerge studentMerge = getStudentMergeResult();
        studentMerge.setMergeStudentID(mergedStudentId.toString());
        studentMerge.setStudentID(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId().toString());
        penMatchResult.getMatchingRecords().get(0).setMatchingPEN(oneYearAgoCollectionStudent.getAssignedPen());
        penMatchResult.getMatchingRecords().get(0).setStudentID(oneYearAgoCollectionStudent.getAssignedStudentId().toString());
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        when(this.restUtils.getMergedStudentIds(any(), any())).thenReturn(List.of(studentMerge));
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.DISTRICT_DUPLICATE_FUNDING.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_IndAuthorityDoubleReported() {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("INDEPEND");
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");

        CollectionEntity collectionEntity = createMockCollectionEntity();
        var snapDate = LocalDate.of(LocalDateTime.now().getYear(), Month.FEBRUARY, 15);
        collectionEntity.setSnapshotDate(snapDate);
        collectionEntity.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionEntity = collectionRepository.save(collectionEntity);
        SdcSchoolCollectionEntity sdcSchoolCollection = createMockSdcSchoolCollectionEntity(collectionEntity, null);
        sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcSchoolCollection);

        SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = createMockSchoolStudentForSagaEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentEntity.setEnrolledGradeCode("08");
        sdcSchoolCollectionStudentEntity.setCreateDate(LocalDateTime.of(LocalDateTime.now().getYear(), Month.JANUARY, 1, 0, 0));

        sdcSchoolCollectionStudentRepository.save(sdcSchoolCollectionStudentEntity);

        this.studentData.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentEntity);

        this.studentData.getSchool().setSchoolId(sdcSchoolCollection.getSchoolID().toString());
        this.studentData.getSchool().setDistrictId(UUID.randomUUID().toString());

        var oldCollection = createMockCollectionEntity();
        var oldSnapDate = LocalDate.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 29);
        oldCollection.setSnapshotDate(oldSnapDate);
        oldCollection = collectionRepository.save(oldCollection);
        var oldSdcCollection = createMockSdcSchoolCollectionEntity(oldCollection, null);
        oldSdcCollection.setSchoolID(sdcSchoolCollection.getSchoolID());
        oldSdcCollection = sdcSchoolCollectionRepository.save(oldSdcCollection);
        var oneYearAgoCollectionStudent = createMockSchoolStudentForSagaEntity(oldSdcCollection);
        oneYearAgoCollectionStudent.setAssignedStudentId(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId());
        sdcSchoolCollectionStudentRepository.save(oneYearAgoCollectionStudent);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(sdcSchoolCollection.getSchoolID())));
        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setMatchingPEN(oneYearAgoCollectionStudent.getAssignedPen());
        penMatchResult.getMatchingRecords().get(0).setStudentID(oneYearAgoCollectionStudent.getAssignedStudentId().toString());
        when(restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.IND_AUTH_DUPLICATE_FUNDING.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_AdultStudent() {
        // Given
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollectionEntity);
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsAdult(true);
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0500");

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("0.625"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_CollectionAndGrade() {
        // Given
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollectionEntity);
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("12");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0700");
        this.studentData.getSchool().setFacilityTypeCode(FacilityTypeCodes.SUMMER.getCode());

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("0.875"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_NewOnlineStudent() {
        var school = this.createMockSchool();
        var district = this.createMockDistrict();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));
        // Given
        CollectionEntity collectionOrig = createMockCollectionEntity();
        collectionOrig.setSnapshotDate(LocalDate.of(collectionOrig.getOpenDate().getYear(), 2, 15));
        collectionOrig.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionRepository.save(collectionOrig);

        var districtID = UUID.randomUUID();
        var districtColl = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collectionOrig, districtID));

        SdcSchoolCollectionEntity sdcSchoolCollectionEntityOrig = createMockSdcSchoolCollectionEntity(collectionOrig, UUID.fromString(school.getSchoolId()));
        sdcSchoolCollectionEntityOrig.setSdcDistrictCollectionID(districtColl.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntityOrig);
        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollectionEntityOrig);
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        this.studentData.getSchool().setSchoolId(sdcSchoolCollectionEntityOrig.getSchoolID().toString());
        this.studentData.getSchool().setDistrictId(districtColl.getDistrictID().toString());

        SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = createMockSchoolStudentForSagaEntity(sdcSchoolCollectionEntityOrig);
        sdcSchoolCollectionStudentEntity.setEnrolledGradeCode("KH");
        sdcSchoolCollectionStudentEntity.setCreateDate(LocalDateTime.of(LocalDateTime.now().getYear(), Month.JANUARY, 1, 0, 0));

        sdcSchoolCollectionStudentRepository.save(sdcSchoolCollectionStudentEntity);

        this.studentData.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentEntity);

        var lastCollectionDate = LocalDateTime.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 5, 0, 0);

        var oldCollection = createMockCollectionEntity();
        var oldSnapDate = LocalDate.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 29);
        oldCollection.setSnapshotDate(oldSnapDate);
        var oldSdcCollection = createMockSdcSchoolCollectionEntity(oldCollection, null);
        collectionRepository.save(oldCollection);
        var oldDistrictColl = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(oldCollection, districtID));
        oldSdcCollection.setSchoolID(sdcSchoolCollectionEntityOrig.getSchoolID());
        oldSdcCollection.setSdcDistrictCollectionID(oldDistrictColl.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(oldSdcCollection);
        var oneYearAgoCollectionStudent = createMockSchoolStudentEntity(oldSdcCollection);
        oneYearAgoCollectionStudent.setCreateDate(lastCollectionDate);
        oneYearAgoCollectionStudent.setAssignedStudentId(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId());
        oneYearAgoCollectionStudent.setEnrolledGradeCode("HS");

        sdcSchoolCollectionStudentRepository.save(oneYearAgoCollectionStudent);

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setMatchingPEN(oneYearAgoCollectionStudent.getAssignedPen());
        penMatchResult.getMatchingRecords().get(0).setStudentID(oneYearAgoCollectionStudent.getAssignedStudentId().toString());
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("0.4529"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_AlternatePrograms() {
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);

        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollectionEntity);
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("03");
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsGraduated(false);
        this.studentData.getSchool().setFacilityTypeCode("ALT_PROGS");

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        assertEquals(BigDecimal.ONE, result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_StudentGrade() {
        // Given
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollectionEntity);
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("KH");

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("0.5"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_SupportBlocks() {
        // Given
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollectionEntity);
        this.studentData.getSdcSchoolCollectionStudentEntity().setSupportBlocks("0");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0900");

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("1.125"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_StudentGraduated() {
        // Given
        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollectionEntity);
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("1100");

        // When
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("1.375"), result.getFte());
        assertNull(result.getFteZeroReason());
    }
}
