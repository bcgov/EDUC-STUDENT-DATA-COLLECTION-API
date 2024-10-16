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
}
