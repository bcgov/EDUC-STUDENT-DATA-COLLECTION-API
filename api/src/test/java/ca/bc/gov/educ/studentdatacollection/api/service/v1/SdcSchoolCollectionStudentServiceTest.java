package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorChainProcessor;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentHistoryRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentValidationIssueRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityRulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.rules.RulesProcessor;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

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
    RestUtils restUtils;

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
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
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
    void testSoftDeleteSdcSchoolCollectionStudents_WhenStudentExists_SavesEntity() {
        // Given
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();

        // Create a mock SdcSchoolCollectionStudentEntity
        CollectionEntity collection = new CollectionEntity();
        collection.setCollectionID(UUID.randomUUID());
        SdcSchoolCollectionEntity sdcColl = new SdcSchoolCollectionEntity();
        sdcColl.setSdcSchoolCollectionID(UUID.randomUUID());
        sdcColl.setCollectionEntity(collection);
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollection(sdcColl);
        mockStudentEntity.setSdcSchoolCollectionStudentID(UUID.randomUUID());
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        SdcSchoolCollectionStudentValidationIssueEntity mockValidationError = new SdcSchoolCollectionStudentValidationIssueEntity();
        when(sdcSchoolCollectionStudentValidationIssueRepository.findById(any())).thenReturn(Optional.of(mockValidationError));
        SdcSchoolCollectionStudentHistoryEntity studentHistoryEntity = new SdcSchoolCollectionStudentHistoryEntity();
        when(sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(any(), any())).thenReturn(studentHistoryEntity);

        when(sdcSchoolCollectionStudentRepository.save(any())).thenReturn(mockStudentEntity);

        // When
        sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudent(mockStudentEntity.getSdcSchoolCollectionStudentID());

        // Then
        // Verify that the save method is called once with the correct entity
        verify(sdcSchoolCollectionStudentRepository, times(1)).save(mockStudentEntity);

        // Assert that the status has been updated to DELETED
        assertSame(mockStudentEntity.getSdcSchoolCollectionStudentStatusCode(), SdcSchoolStudentStatus.DELETED.toString());
    }

  @Test
  void testSoftDeleteSdcSchoolCollectionStudent_WhenStudentDoesNotExist_ThrowsError() {
      // Given
      UUID sdcSchoolCollectionStudentID = UUID.randomUUID();

      // When
      when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.empty());

      // Then assert that an EntityNotFoundException is thrown
      assertThrows(EntityNotFoundException.class, () -> {
          sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudent(sdcSchoolCollectionStudentID);
      });
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
    void testSavingStudentWithoutDuplicate_savesSuccessfully() {
        // Given
        UUID studentID = UUID.randomUUID();
        UUID schoolCollectionID = UUID.randomUUID();
        UUID collectionID = UUID.randomUUID();
        UUID schoolID = UUID.randomUUID();

        SdcSchoolCollectionStudentEntity studentEntity = new SdcSchoolCollectionStudentEntity();
        studentEntity.setAssignedStudentId(studentID);

        CollectionEntity collectionEntity = new CollectionEntity();
        collectionEntity.setCollectionID(collectionID);
        collectionEntity.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());

        SdcSchoolCollectionEntity schoolCollection = new SdcSchoolCollectionEntity();
        schoolCollection.setCollectionEntity(collectionEntity);
        schoolCollection.setSdcSchoolCollectionID(schoolCollectionID);
        schoolCollection.setSchoolID(schoolID);

        studentEntity.setSdcSchoolCollection(schoolCollection);

        // Mock FTE Calculator Processor
        FteCalculationResult fteCalculationResult = new FteCalculationResult();
        fteCalculationResult.setFte(new BigDecimal("1.0"));
        when(fteCalculatorChainProcessor.processFteCalculator(any(StudentRuleData.class))).thenReturn(fteCalculationResult);

        // Mock repository responses
        when(sdcSchoolCollectionRepository.findById(schoolCollectionID)).thenReturn(Optional.of(schoolCollection));
        when(sdcSchoolCollectionStudentRepository.findAllDuplicateStudentsByCollectionID(collectionID, Collections.singletonList(studentID)))
                .thenReturn(Collections.emptyList());

        // When
        SdcSchoolCollectionStudentEntity result = sdcSchoolCollectionStudentService.validateAndProcessSdcSchoolCollectionStudent(studentEntity, studentEntity, false);

        // Assert
        assertNotSame(studentEntity, result);
        verify(sdcSchoolCollectionStudentRepository, times(1)).save(any(SdcSchoolCollectionStudentEntity.class));
    }

    @Test
    void testSavingStudentWithDuplicateAssignedId_doesNotSave() {
        // Given
        UUID studentID = UUID.randomUUID();
        UUID schoolCollectionID = UUID.randomUUID();
        UUID collectionID = UUID.randomUUID();
        UUID schoolID = UUID.randomUUID();

        SdcSchoolCollectionStudentEntity studentEntity = new SdcSchoolCollectionStudentEntity();
        studentEntity.setAssignedStudentId(studentID);

        SdcSchoolCollectionStudentEntity existingDuplicate = new SdcSchoolCollectionStudentEntity();
        existingDuplicate.setAssignedStudentId(studentID);

        CollectionEntity collectionEntity = new CollectionEntity();
        collectionEntity.setCollectionID(collectionID);
        collectionEntity.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());

        SdcSchoolCollectionEntity schoolCollection = new SdcSchoolCollectionEntity();
        schoolCollection.setCollectionEntity(collectionEntity);
        schoolCollection.setSdcSchoolCollectionID(schoolCollectionID);
        schoolCollection.setSchoolID(schoolID);

        studentEntity.setSdcSchoolCollection(schoolCollection);

        // Mock FTE Calculator Processor
        FteCalculationResult fteCalculationResult = new FteCalculationResult();
        fteCalculationResult.setFte(new BigDecimal("1.0"));
        when(fteCalculatorChainProcessor.processFteCalculator(any(StudentRuleData.class))).thenReturn(fteCalculationResult);

        // Mock repository responses to simulate the existing duplicate
        when(sdcSchoolCollectionRepository.findById(schoolCollectionID)).thenReturn(Optional.of(schoolCollection));
        when(sdcSchoolCollectionStudentRepository.findAllDuplicateStudentsByCollectionID(collectionID, Collections.singletonList(studentID)))
                .thenReturn(Collections.singletonList(existingDuplicate));

        // When and assert
        InvalidPayloadException thrown = assertThrows(InvalidPayloadException.class, () -> {
            sdcSchoolCollectionStudentService.validateAndProcessSdcSchoolCollectionStudent(studentEntity, studentEntity, false);
        }, "SdcSchoolCollectionStudent was not saved to the database because it would create provincial duplicate.");

        assertNotNull(thrown.getError());
        assertEquals("SdcSchoolCollectionStudent was not saved to the database because it would create provincial duplicate.", thrown.getError().getMessage());

        verify(sdcSchoolCollectionStudentRepository, never()).save(studentEntity);
    }
}
