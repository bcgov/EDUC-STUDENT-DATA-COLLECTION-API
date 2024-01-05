package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class FteCalculatorChainProcessorIntegrationTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private FteCalculatorChainProcessor fteCalculatorChainProcessor;
    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
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
    }

    @Test
    void testProcessFteCalculator_OffshoreStudent() {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("OFFSHORE");

        // When
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
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.NOMINAL_ROLL_ELIGIBLE.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    @Transactional(rollbackFor = Exception.class)
    void testProcessFteCalculator_NoCoursesInLastTwoYears() throws IOException {
        // Given
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0");
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsSchoolAged(true);
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("10");
        this.studentData.getSdcSchoolCollectionStudentEntity().setCreateDate(LocalDateTime.now());

        final File file = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("sdc-school-collection-entity.json")).getFile()
        );

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        SdcSchoolCollectionEntity sdcSchoolCollection = objectMapper.readValue(file, SdcSchoolCollectionEntity.class);
        sdcSchoolCollection.setCreateDate(LocalDateTime.now().minusYears(1));

        var collection = collectionRepository.save(sdcSchoolCollection.getCollectionEntity());
        sdcSchoolCollection.getCollectionEntity().setCollectionID(collection.getCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollection);
        this.studentData.getSchool().setSchoolId(sdcSchoolCollection.getSchoolID().toString());
        this.studentData.getSdcSchoolCollectionStudentEntity().setSdcSchoolCollection(sdcSchoolCollection);
        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.INACTIVE.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    @Transactional(rollbackFor = Exception.class)
    void testProcessFteCalculator_DistrictDoubleReported() throws IOException {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("PUBLIC");
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        this.studentData.setCollectionTypeCode("FEBRUARY");
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("08");
        this.studentData.getSdcSchoolCollectionStudentEntity().setCreateDate(LocalDateTime.of(LocalDateTime.now().getYear(), Month.FEBRUARY, 5, 0, 0));

        final File file = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("sdc-school-collection-entity.json")).getFile()
        );

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        SdcSchoolCollectionEntity sdcSchoolCollection = objectMapper.readValue(file, SdcSchoolCollectionEntity.class);
        var lastCollectionDate = LocalDateTime.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 5, 0, 0);
        sdcSchoolCollection.setCreateDate(lastCollectionDate);

        var collection = collectionRepository.save(sdcSchoolCollection.getCollectionEntity());
        sdcSchoolCollection.getCollectionEntity().setCollectionID(collection.getCollectionID());
        sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcSchoolCollection);
        this.studentData.getSchool().setSchoolId(sdcSchoolCollection.getSchoolID().toString());
        this.studentData.getSchool().setDistrictId(String.valueOf(sdcSchoolCollection.getDistrictID()));

        var oneYearAgoCollectionStudent = createMockSchoolStudentEntity(sdcSchoolCollection);
        oneYearAgoCollectionStudent.setCreateDate(lastCollectionDate);
        oneYearAgoCollectionStudent.setSdcSchoolCollection(sdcSchoolCollection);
        oneYearAgoCollectionStudent.setAssignedStudentId(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId());
        sdcSchoolCollectionStudentRepository.save(oneYearAgoCollectionStudent);

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = ZeroFteReasonCodes.DISTRICT_DUPLICATE_FUNDING.getCode();

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    @Transactional(rollbackFor = Exception.class)
    void testProcessFteCalculator_IndAuthorityDoubleReported() throws IOException {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("INDEPEND");
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        this.studentData.setCollectionTypeCode("FEBRUARY");
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("08");
        this.studentData.getSdcSchoolCollectionStudentEntity().setCreateDate(LocalDateTime.of(LocalDateTime.now().getYear(), Month.FEBRUARY, 5, 0, 0));

        final File file = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("sdc-school-collection-entity.json")).getFile()
        );

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        SdcSchoolCollectionEntity sdcSchoolCollection = objectMapper.readValue(file, SdcSchoolCollectionEntity.class);
        var lastCollectionDate = LocalDateTime.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 5, 0, 0);
        sdcSchoolCollection.setCreateDate(lastCollectionDate);

        var collection = collectionRepository.save(sdcSchoolCollection.getCollectionEntity());
        sdcSchoolCollection.getCollectionEntity().setCollectionID(collection.getCollectionID());
        sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcSchoolCollection);
        this.studentData.getSchool().setSchoolId(sdcSchoolCollection.getSchoolID().toString());
        this.studentData.getSchool().setDistrictId(String.valueOf(sdcSchoolCollection.getDistrictID()));

        var oneYearAgoCollectionStudent = createMockSchoolStudentEntity(sdcSchoolCollection);
        oneYearAgoCollectionStudent.setCreateDate(lastCollectionDate);
        oneYearAgoCollectionStudent.setSdcSchoolCollection(sdcSchoolCollection);
        oneYearAgoCollectionStudent.setAssignedStudentId(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId());
        sdcSchoolCollectionStudentRepository.save(oneYearAgoCollectionStudent);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(sdcSchoolCollection.getSchoolID())));

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
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsAdult(true);
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0500");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("0.625"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_CollectionAndGrade() {
        // Given
        this.studentData.setCollectionTypeCode("JULY");
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("12");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0700");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("0.875"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    @Transactional(rollbackFor = Exception.class)
    void testProcessFteCalculator_NewOnlineStudent() {
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        // Given
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        this.studentData.setCollectionTypeCode("FEBRUARY");
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("KH");
        this.studentData.getSdcSchoolCollectionStudentEntity().setCreateDate(LocalDateTime.of(LocalDateTime.now().getYear(), Month.FEBRUARY, 5, 0, 0));

        var lastCollectionDate = LocalDateTime.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 5, 0, 0);

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId())));

        var oneYearAgoCollectionStudent = createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        oneYearAgoCollectionStudent.setCreateDate(lastCollectionDate);
        oneYearAgoCollectionStudent.setSdcSchoolCollection(sdcSchoolCollectionEntity);
        oneYearAgoCollectionStudent.setAssignedStudentId(this.studentData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId());
        oneYearAgoCollectionStudent.setEnrolledGradeCode("HS");

        sdcSchoolCollectionStudentRepository.save(oneYearAgoCollectionStudent);

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("0.4529"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_AlternatePrograms() {
        // Given
        this.studentData.setCollectionTypeCode("SEPTEMBER");
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("03");
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsGraduated(false);
        this.studentData.getSchool().setFacilityTypeCode("ALT_PROGS");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(BigDecimal.ONE, result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_StudentGrade() {
        // Given
        this.studentData.setCollectionTypeCode("SEPTEMBER");
        this.studentData.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("KH");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("0.5"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_SupportBlocks() {
        // Given
        this.studentData.getSdcSchoolCollectionStudentEntity().setSupportBlocks("0");
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("0900");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("1.125"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_StudentGraduated() {
        // Given
        this.studentData.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);
        this.studentData.getSdcSchoolCollectionStudentEntity().setNumberOfCourses("1100");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("1.375"), result.getFte());
        assertNull(result.getFteZeroReason());
    }
}
