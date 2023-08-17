package ca.bc.gov.educ.studentdatacollection.api.calculator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
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

@SpringBootTest
@ActiveProfiles("test")
class FteCalculatorChainProcessorIntegrationTest {

    @Autowired
    private FteCalculatorChainProcessor fteCalculatorChainProcessor;
    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    @Autowired
    CollectionRepository collectionRepository;
    @Autowired
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private SdcStudentSagaData studentData;
    @Autowired
    RestUtils restUtils;

    @BeforeEach
    public void setUp() throws IOException {
        final File file = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("sdc-student-saga-data.json")).getFile()
        );
        studentData = new ObjectMapper().readValue(file, SdcStudentSagaData.class);
    }

    @Test
    void testProcessFteCalculator_OffshoreStudent() {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("OFFSHORE");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(this.studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = "Offshore students do not receive funding.";

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_StudentOutOfProvince() {
        // Given
        this.studentData.getSdcSchoolCollectionStudent().setSchoolFundingCode("14");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = "Out-of-Province/International Students are not eligible for funding.";

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_TooYoung() {
        // Given
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
        this.studentData.getSdcSchoolCollectionStudent().setDob(format.format(LocalDate.now().minusYears(3)));

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = "The student is too young.";

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_GraduatedAdultIndySchool() {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("INDEPEND");
        this.studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode("GA");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = "The student is graduated adult reported by an independent school.";

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_IndependentSchoolAndBandCode() {
        // Given
        this.studentData.getSchool().setSchoolCategoryCode("INDEPEND");
        this.studentData.getSdcSchoolCollectionStudent().setBandCode("");
        this.studentData.getSdcSchoolCollectionStudent().setSchoolFundingCode("20");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = "The student is Nominal Roll eligible and is federally funded.";

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    @Transactional(rollbackFor = Exception.class)
    void testProcessFteCalculator_NoCoursesInLastTwoYears() throws IOException {
        // Given
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        this.studentData.getSdcSchoolCollectionStudent().setNumberOfCourses("0");
        this.studentData.getSdcSchoolCollectionStudent().setIsSchoolAged("true");
        this.studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode("10");
        this.studentData.getSdcSchoolCollectionStudent().setCreateDate(LocalDateTime.now().toString());

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

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = "The student has not been reported as \"active\" in a new course in the last two years.";

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
        this.studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode("08");
        this.studentData.getSdcSchoolCollectionStudent().setCreateDate(LocalDateTime.of(LocalDateTime.now().getYear(), Month.FEBRUARY, 5, 0, 0).toString());

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

        var oneYearAgoStudentCollection = SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudentEntity(this.studentData.getSdcSchoolCollectionStudent());
        oneYearAgoStudentCollection.setCreateDate(lastCollectionDate);
        oneYearAgoStudentCollection.setSdcSchoolCollectionID(sdcSchoolCollection.getSdcSchoolCollectionID());
        sdcSchoolCollectionStudentRepository.save(oneYearAgoStudentCollection);

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = "The district has already received funding for the student this year.";

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
        this.studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode("08");
        this.studentData.getSdcSchoolCollectionStudent().setCreateDate(LocalDateTime.of(LocalDateTime.now().getYear(), Month.FEBRUARY, 5, 0, 0).toString());

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

        var oneYearAgoStudentCollection = SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudentEntity(this.studentData.getSdcSchoolCollectionStudent());
        oneYearAgoStudentCollection.setCreateDate(lastCollectionDate);
        oneYearAgoStudentCollection.setSdcSchoolCollectionID(sdcSchoolCollection.getSdcSchoolCollectionID());
        sdcSchoolCollectionStudentRepository.save(oneYearAgoStudentCollection);

        when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(sdcSchoolCollection.getSchoolID())));

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        BigDecimal expectedFte = BigDecimal.ZERO;
        String expectedFteZeroReason = "The authority has already received funding for the student this year.";

        assertEquals(expectedFte, result.getFte());
        assertEquals(expectedFteZeroReason, result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_AdultStudent() {
        // Given
        this.studentData.getSdcSchoolCollectionStudent().setIsAdult("true");
        this.studentData.getSdcSchoolCollectionStudent().setNumberOfCourses("0500");

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
        this.studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode("12");
        this.studentData.getSdcSchoolCollectionStudent().setNumberOfCourses("0700");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("0.875"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    @Transactional(rollbackFor = Exception.class)
    void testProcessFteCalculator_NewOnlineStudent() {
        // Given
        this.studentData.getSchool().setFacilityTypeCode("DIST_LEARN");
        this.studentData.setCollectionTypeCode("FEBRUARY");
        this.studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode("KH");
        this.studentData.getSdcSchoolCollectionStudent().setCreateDate(LocalDateTime.of(LocalDateTime.now().getYear(), Month.FEBRUARY, 5, 0, 0).toString());

        var lastCollectionDate = LocalDateTime.of(LocalDateTime.now().minusYears(1).getYear(), Month.SEPTEMBER, 5, 0, 0);
        var oneYearAgoStudentCollection = SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudentEntity(this.studentData.getSdcSchoolCollectionStudent());
        oneYearAgoStudentCollection.setCreateDate(lastCollectionDate);
        oneYearAgoStudentCollection.setEnrolledGradeCode("HS");

        sdcSchoolCollectionStudentRepository.save(oneYearAgoStudentCollection);

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
        this.studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode("03");
        this.studentData.getSdcSchoolCollectionStudent().setIsGraduated("false");
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
        this.studentData.getSdcSchoolCollectionStudent().setEnrolledGradeCode("KH");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("0.5"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_SupportBlocks() {
        // Given
        this.studentData.getSdcSchoolCollectionStudent().setSupportBlocks("0");
        this.studentData.getSdcSchoolCollectionStudent().setNumberOfCourses("0900");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("1.125"), result.getFte());
        assertNull(result.getFteZeroReason());
    }

    @Test
    void testProcessFteCalculator_StudentGraduated() {
        // Given
        this.studentData.getSdcSchoolCollectionStudent().setIsGraduated("true");
        this.studentData.getSdcSchoolCollectionStudent().setNumberOfCourses("1100");

        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentData);

        // Then
        assertEquals(new BigDecimal("1.375"), result.getFte());
        assertNull(result.getFteZeroReason());
    }
}
