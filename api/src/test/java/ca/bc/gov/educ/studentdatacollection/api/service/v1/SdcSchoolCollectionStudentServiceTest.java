package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorChainProcessor;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityRulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.rules.RulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SdcSchoolCollectionStudentServiceTest {

    @Mock
    private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

    @InjectMocks
    private SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

    @Mock
    private SdcSchoolCollectionStudentHistoryService sdcSchoolCollectionStudentHistoryService;

    @Mock
    SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;

    @Mock
    SdcSchoolCollectionStudentValidationIssueRepository sdcSchoolCollectionStudentValidationIssueRepository;

    @Mock
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    @Mock
    CollectionRepository collectionRepository;

    @Mock
    SdcDuplicateRepository sdcDuplicateRepository;

    @Mock
    RestUtils restUtils;

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    RulesProcessor rulesProcessor;

    @Mock
    ProgramEligibilityRulesProcessor programEligibilityRulesProcessor;

    @Mock
    FteCalculatorChainProcessor fteCalculatorChainProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUpdateFteColumns_WhenStudentExists_SavesEntity() {
        // Given
        FteCalculationResult fteCalculationResult = new FteCalculationResult();
        fteCalculationResult.setFte(BigDecimal.ONE);
        fteCalculationResult.setFteZeroReason("SomeReason");

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        // When
        sdcSchoolCollectionStudentService.updateFteColumns(fteCalculationResult, mockStudentEntity);

        // Then
        // Assert that the FTE and FTE zero reason code are updated as expected
        assertSame(fteCalculationResult.getFte(), mockStudentEntity.getFte());
        assertSame(fteCalculationResult.getFteZeroReason(), mockStudentEntity.getFteZeroReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenNotEligibleForAnything_UpdatesAllColumns() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<ProgramEligibilityIssueCode> errors = List.of(ProgramEligibilityIssueCode.OFFSHORE);

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        sdcSchoolCollectionStudentService.updateProgramEligibilityColumns(errors, mockStudentEntity);

        String reasonCode = ProgramEligibilityIssueCode.OFFSHORE.getCode();
        assertSame(reasonCode, mockStudentEntity.getFrenchProgramNonEligReasonCode());
        assertSame(reasonCode, mockStudentEntity.getEllNonEligReasonCode());
        assertSame(reasonCode, mockStudentEntity.getIndigenousSupportProgramNonEligReasonCode());
        assertSame(reasonCode, mockStudentEntity.getCareerProgramNonEligReasonCode());
        assertSame(reasonCode, mockStudentEntity.getSpecialEducationNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenNotEnrolledInFrenchPrograms_UpdatesFrenchEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<ProgramEligibilityIssueCode> errors = List.of(
                ProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        sdcSchoolCollectionStudentService.updateProgramEligibilityColumns(errors, mockStudentEntity);

        String reasonCode = ProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH.getCode();
        assertSame(reasonCode, mockStudentEntity.getFrenchProgramNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenNotEnrolledInCareerPrograms_UpdatesCareerProgramEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<ProgramEligibilityIssueCode> errors = List.of(
                ProgramEligibilityIssueCode.NOT_ENROLLED_CAREER
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        sdcSchoolCollectionStudentService.updateProgramEligibilityColumns(errors, mockStudentEntity);

        String reasonCode = ProgramEligibilityIssueCode.NOT_ENROLLED_CAREER.getCode();
        assertSame(reasonCode, mockStudentEntity.getCareerProgramNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenNotEnrolledInIndigenousPrograms_UpdatesIndigenousEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<ProgramEligibilityIssueCode> errors = List.of(
                ProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        sdcSchoolCollectionStudentService.updateProgramEligibilityColumns(errors, mockStudentEntity);

        String reasonCode = ProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS.getCode();
        assertSame(reasonCode, mockStudentEntity.getIndigenousSupportProgramNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenStudentDoesNotRequireSpecialEd_UpdatesSpecialEdEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<ProgramEligibilityIssueCode> errors = List.of(
                ProgramEligibilityIssueCode.NOT_ENROLLED_SPECIAL_ED
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        sdcSchoolCollectionStudentService.updateProgramEligibilityColumns(errors, mockStudentEntity);

        String reasonCode = ProgramEligibilityIssueCode.NOT_ENROLLED_SPECIAL_ED.getCode();
        assertSame(reasonCode, mockStudentEntity.getSpecialEducationNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenStudentIsGraduatedOrAGraduatedAdult_UpdatesSpecialEdEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<ProgramEligibilityIssueCode> errors = List.of(
                ProgramEligibilityIssueCode.NON_ELIG_SPECIAL_EDUCATION
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        sdcSchoolCollectionStudentService.updateProgramEligibilityColumns(errors, mockStudentEntity);

        String reasonCode = ProgramEligibilityIssueCode.NON_ELIG_SPECIAL_EDUCATION.getCode();
        assertSame(reasonCode, mockStudentEntity.getSpecialEducationNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenIndigenousStudentIsAdult_UpdatesIndigenousEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<ProgramEligibilityIssueCode> errors = List.of(
                ProgramEligibilityIssueCode.INDIGENOUS_ADULT
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        sdcSchoolCollectionStudentService.updateProgramEligibilityColumns(errors, mockStudentEntity);

        String reasonCode = ProgramEligibilityIssueCode.INDIGENOUS_ADULT.getCode();
        assertSame(reasonCode, mockStudentEntity.getIndigenousSupportProgramNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenIndigenousStudentHasNoAncestry_UpdatesIndigenousEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<ProgramEligibilityIssueCode> errors = List.of(
                ProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        sdcSchoolCollectionStudentService.updateProgramEligibilityColumns(errors, mockStudentEntity);

        String reasonCode = ProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY.getCode();
        assertSame(reasonCode, mockStudentEntity.getIndigenousSupportProgramNonEligReasonCode());
    }

  @Test
  void testClearSdcSchoolStudentProgramEligibilityColumns_WhenColumnsAreSet_SetsAllColumnsToNull() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        String reasonCode = ProgramEligibilityIssueCode.HOMESCHOOL.getCode();

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        mockStudentEntity.setFrenchProgramNonEligReasonCode(reasonCode);
        mockStudentEntity.setEllNonEligReasonCode(reasonCode);
        mockStudentEntity.setIndigenousSupportProgramNonEligReasonCode(reasonCode);
        mockStudentEntity.setCareerProgramNonEligReasonCode(reasonCode);
        mockStudentEntity.setSpecialEducationNonEligReasonCode(reasonCode);

        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        sdcSchoolCollectionStudentService.clearSdcSchoolStudentProgramEligibilityColumns(mockStudentEntity);

        // Then
        assertNull(mockStudentEntity.getFrenchProgramNonEligReasonCode());
        assertNull(mockStudentEntity.getEllNonEligReasonCode());
        assertNull(mockStudentEntity.getIndigenousSupportProgramNonEligReasonCode());
        assertNull(mockStudentEntity.getCareerProgramNonEligReasonCode());
        assertNull(mockStudentEntity.getSpecialEducationNonEligReasonCode());
  }

  @Test
    void testConversionOfNumOfCourses_WithValidInput_ConvertsCorrectly() {
      SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
      UUID studentID = UUID.randomUUID();
      mockStudentEntity.setSdcSchoolCollectionStudentID(studentID);
      mockStudentEntity.setNumberOfCourses("1100");

      sdcSchoolCollectionStudentService.convertNumOfCourses(mockStudentEntity);

      BigDecimal expectedNumber = new BigDecimal("11.00");

      assertEquals(0, expectedNumber.compareTo(mockStudentEntity.getNumberOfCoursesDec()));
  }

    @Test
    void testPrepareAndSendSdcStudentsForFurtherProcessing_givenStudentEntities_shouldPrepareAndPublish() {
        final SdcSchoolCollectionStudentEntity entity = new SdcSchoolCollectionStudentEntity();
        entity.setSdcSchoolCollectionStudentID(UUID.randomUUID());

        final SdcSchoolCollectionEntity collectionEntity = new SdcSchoolCollectionEntity();
        collectionEntity.setSdcSchoolCollectionID(UUID.randomUUID());
        collectionEntity.setSchoolID(UUID.randomUUID());

        final CollectionEntity collectionEntity1 = new CollectionEntity();
        collectionEntity1.setCollectionTypeCode("TEST");
        collectionEntity.setCollectionEntity(collectionEntity1);
        entity.setSdcSchoolCollection(collectionEntity);

        final SchoolTombstone school = new SchoolTombstone();
        school.setMincode("12345678");
        when(sdcSchoolCollectionRepository.findById(any())).thenReturn(Optional.of(collectionEntity));
        when(restUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(school));

        doNothing().when(messagePublisher).dispatchMessage(anyString(), any(byte[].class));

        sdcSchoolCollectionStudentService.prepareAndSendSdcStudentsForFurtherProcessing(List.of(entity));
        verify(restUtils, atLeastOnce()).getSchoolBySchoolID(any());
        verify(messagePublisher, atLeastOnce()).dispatchMessage(anyString(), any(byte[].class));
    }
    @Test
    void testPrepareStudentsForDemogUpdate_givenStudentEntities_shouldPrepareAndPublish() {
        final SdcSchoolCollectionStudentEntity entity = new SdcSchoolCollectionStudentEntity();
        entity.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        entity.setAssignedStudentId(UUID.randomUUID());

        final SdcSchoolCollectionEntity collectionEntity = new SdcSchoolCollectionEntity();
        collectionEntity.setSdcSchoolCollectionID(UUID.randomUUID());
        collectionEntity.setSchoolID(UUID.randomUUID());

        final CollectionEntity collectionEntity1 = new CollectionEntity();
        collectionEntity1.setCollectionTypeCode("TEST");
        collectionEntity.setCollectionEntity(collectionEntity1);
        entity.setSdcSchoolCollection(collectionEntity);

        final SchoolTombstone school = new SchoolTombstone();
        school.setMincode("12345678");

        when(collectionRepository.findActiveCollection()).thenReturn(Optional.of(collectionEntity1));
        when(restUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(school));

        sdcSchoolCollectionStudentService.prepareStudentsForDemogUpdate(List.of(entity));
        verify(restUtils, atLeastOnce()).getSchoolBySchoolID(any());
    }

    @Test
    void testIsCurrentStudentAttendingSchoolOfRecord_givenOtherStudentsWithSameAssignedID_shouldReturnTrue() {
        final SdcSchoolCollectionStudentEntity entity = new SdcSchoolCollectionStudentEntity();
        entity.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        entity.setAssignedStudentId(UUID.randomUUID());
        entity.setNumberOfCourses("2.0");

        final SdcSchoolCollectionEntity collectionEntity = new SdcSchoolCollectionEntity();
        collectionEntity.setSdcSchoolCollectionID(UUID.randomUUID());
        collectionEntity.setSchoolID(UUID.randomUUID());
        entity.setSdcSchoolCollection(collectionEntity);

        final SdcSchoolCollectionStudentEntity entity1 = new SdcSchoolCollectionStudentEntity();
        entity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        entity1.setAssignedStudentId(UUID.randomUUID());
        entity1.setNumberOfCourses("1.0");

        final SdcSchoolCollectionEntity collectionEntity1 = new SdcSchoolCollectionEntity();
        collectionEntity1.setSdcSchoolCollectionID(UUID.randomUUID());
        collectionEntity1.setSchoolID(UUID.randomUUID());
        entity1.setSdcSchoolCollection(collectionEntity1);

        final SchoolTombstone school = new SchoolTombstone();
        school.setMincode("12345678");
        school.setSchoolCategoryCode("TEST");

        when(restUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(school));

        final var result = sdcSchoolCollectionStudentService.isCurrentStudentAttendingSchoolOfRecord(entity, List.of(entity1));
        assertTrue(result);
    }

    @Test
    void testIsCurrentStudentAttendingSchoolOfRecord_givenOtherStudentsWithSameAssignedIDAndMoreCourses_shouldReturnFalse() {
        final SdcSchoolCollectionStudentEntity entity = new SdcSchoolCollectionStudentEntity();
        entity.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        entity.setAssignedStudentId(UUID.randomUUID());
        entity.setNumberOfCourses("1.0");

        final SdcSchoolCollectionEntity collectionEntity = new SdcSchoolCollectionEntity();
        collectionEntity.setSdcSchoolCollectionID(UUID.randomUUID());
        collectionEntity.setSchoolID(UUID.randomUUID());
        entity.setSdcSchoolCollection(collectionEntity);

        final SdcSchoolCollectionStudentEntity entity1 = new SdcSchoolCollectionStudentEntity();
        entity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        entity1.setAssignedStudentId(UUID.randomUUID());
        entity1.setNumberOfCourses("2.0");

        final SdcSchoolCollectionEntity collectionEntity1 = new SdcSchoolCollectionEntity();
        collectionEntity1.setSdcSchoolCollectionID(UUID.randomUUID());
        collectionEntity1.setSchoolID(UUID.randomUUID());
        entity1.setSdcSchoolCollection(collectionEntity1);

        final SchoolTombstone school = new SchoolTombstone();
        school.setMincode("12345678");
        school.setSchoolCategoryCode("TEST");

        when(restUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(school));
        final var result = sdcSchoolCollectionStudentService.isCurrentStudentAttendingSchoolOfRecord(entity, List.of(entity1));
        assertFalse(result);
    }

    @Test
    void testIsCurrentStudentAttendingSchoolOfRecord_givenOtherStudentsWithSameAssignedIDAndSameCourses_shouldReturnTrue() {
        final SdcSchoolCollectionStudentEntity entity = new SdcSchoolCollectionStudentEntity();
        entity.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        entity.setAssignedStudentId(UUID.randomUUID());
        entity.setNumberOfCourses("1.0");

        final SdcSchoolCollectionEntity collectionEntity = new SdcSchoolCollectionEntity();
        collectionEntity.setSdcSchoolCollectionID(UUID.randomUUID());
        collectionEntity.setSchoolID(UUID.randomUUID());
        entity.setSdcSchoolCollection(collectionEntity);

        final SdcSchoolCollectionStudentEntity entity1 = new SdcSchoolCollectionStudentEntity();
        entity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        entity1.setAssignedStudentId(UUID.randomUUID());
        entity1.setNumberOfCourses("1.0");

        final SdcSchoolCollectionEntity collectionEntity1 = new SdcSchoolCollectionEntity();
        collectionEntity1.setSdcSchoolCollectionID(UUID.randomUUID());
        collectionEntity1.setSchoolID(UUID.randomUUID()); // Different School ID for entity1
        entity1.setSdcSchoolCollection(collectionEntity1);

        final SchoolTombstone school = new SchoolTombstone();
        school.setMincode("12345678");
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        school.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());
        school.setDistrictId("456");

        final SchoolTombstone school1 = new SchoolTombstone();
        school1.setMincode("12345679");
        school1.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        school1.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());
        school1.setDistrictId("457"); // Different district ID

        when(restUtils.getSchoolBySchoolID(String.valueOf(collectionEntity.getSchoolID()))).thenReturn(Optional.of(school));
        when(restUtils.getSchoolBySchoolID(String.valueOf(collectionEntity1.getSchoolID()))).thenReturn(Optional.of(school1));

        District mockDistrict1 = new District();
        mockDistrict1.setDistrictNumber("123");
        District mockDistrict2 = new District();
        mockDistrict2.setDistrictNumber("321");

        when(restUtils.getDistrictByDistrictID("456")).thenReturn(Optional.of(mockDistrict1));
        when(restUtils.getDistrictByDistrictID("457")).thenReturn(Optional.of(mockDistrict2));

        final var result = sdcSchoolCollectionStudentService.isCurrentStudentAttendingSchoolOfRecord(entity, List.of(entity1));
        assertTrue(result);
    }

    @Test
    void testIsCurrentStudentAttendingSchoolOfRecord_givenOtherStudentsWithSameAssignedIDAndSameCoursesAndDifferentSchoolCategory_shouldReturnFalse() {
        final SdcSchoolCollectionStudentEntity entity = new SdcSchoolCollectionStudentEntity();
        entity.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        entity.setAssignedStudentId(UUID.randomUUID());
        entity.setNumberOfCourses("1.0");

        final SdcSchoolCollectionEntity collectionEntity = new SdcSchoolCollectionEntity();
        collectionEntity.setSdcSchoolCollectionID(UUID.randomUUID());
        collectionEntity.setSchoolID(UUID.randomUUID());
        entity.setSdcSchoolCollection(collectionEntity);

        final SdcSchoolCollectionStudentEntity entity1 = new SdcSchoolCollectionStudentEntity();
        entity1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        entity1.setAssignedStudentId(UUID.randomUUID());
        entity1.setNumberOfCourses("1.0");

        final SdcSchoolCollectionEntity collectionEntity1 = new SdcSchoolCollectionEntity();
        collectionEntity1.setSdcSchoolCollectionID(UUID.randomUUID());
        collectionEntity1.setSchoolID(UUID.randomUUID());
        entity1.setSdcSchoolCollection(collectionEntity1);

        final SchoolTombstone school = new SchoolTombstone();
        school.setMincode("12345678");
        school.setSchoolCategoryCode("TEST");
        school.setFacilityTypeCode("TEST");

        final SchoolTombstone school1 = new SchoolTombstone();
        school1.setMincode("12345678");
        school1.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        school1.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());

        when(restUtils.getSchoolBySchoolID(String.valueOf(entity.getSdcSchoolCollection().getSchoolID()))).thenReturn(Optional.of(school));
        when(restUtils.getSchoolBySchoolID(String.valueOf(entity1.getSdcSchoolCollection().getSchoolID()))).thenReturn(Optional.of(school1));

        final var result = sdcSchoolCollectionStudentService.isCurrentStudentAttendingSchoolOfRecord(entity, List.of(entity1));

        assertFalse(result);
    }
}
