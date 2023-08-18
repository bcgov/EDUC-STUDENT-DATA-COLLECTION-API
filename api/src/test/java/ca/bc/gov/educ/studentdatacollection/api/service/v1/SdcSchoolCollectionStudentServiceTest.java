package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
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
        var result = sdcSchoolCollectionStudentService.updateFteColumns(fteCalculationResult, sdcSchoolCollectionStudentID);

        // Then
        // Assert that the FTE and FTE zero reason code are updated as expected
        assertSame(fteCalculationResult.getFte(), result.getFte());
        assertSame(fteCalculationResult.getFteZeroReason(), result.getFteZeroReasonCode());
    }

    @Test
    void testUpdateFteColumns_WhenStudentDoesNotExist_ThrowsError() {
        // Given
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        FteCalculationResult fteCalculationResult = new FteCalculationResult();
        fteCalculationResult.setFte(BigDecimal.ONE);
        fteCalculationResult.setFteZeroReason("SomeReason");

        // When
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.empty());

        // Then assert that an EntityNotFoundException is thrown
        assertThrows(EntityNotFoundException.class, () -> sdcSchoolCollectionStudentService.updateFteColumns(fteCalculationResult, sdcSchoolCollectionStudentID));
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenNotEligibleForAnything_UpdatesAllColumns() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = List.of(
                SdcSchoolCollectionStudentProgramEligibilityIssueCode.OFFSHORE
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        var result = sdcSchoolCollectionStudentService
            .updateProgramEligibilityColumns(errors, sdcSchoolCollectionStudentID);

        String reasonCode = SdcSchoolCollectionStudentProgramEligibilityIssueCode.OFFSHORE.getCode();
        assertSame(reasonCode, result.getFrenchProgramNonEligReasonCode());
        assertSame(reasonCode, result.getEllNonEligReasonCode());
        assertSame(reasonCode, result.getIndigenousSupportProgramNonEligReasonCode());
        assertSame(reasonCode, result.getCareerProgramNonEligReasonCode());
        assertSame(reasonCode, result.getSpecialEducationNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenNotEnrolledInFrenchPrograms_UpdatesFrenchEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = List.of(
                SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        var result = sdcSchoolCollectionStudentService
            .updateProgramEligibilityColumns(errors, sdcSchoolCollectionStudentID);

        String reasonCode = SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH.getCode();
        assertSame(reasonCode, result.getFrenchProgramNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenNotEnrolledInCareerPrograms_UpdatesCareerProgramEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = List.of(
                SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_CAREER
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        var result = sdcSchoolCollectionStudentService
            .updateProgramEligibilityColumns(errors, sdcSchoolCollectionStudentID);

        String reasonCode = SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_CAREER.getCode();
        assertSame(reasonCode, result.getCareerProgramNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenNotEnrolledInIndigenousPrograms_UpdatesIndigenousEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = List.of(
                SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        var result = sdcSchoolCollectionStudentService
            .updateProgramEligibilityColumns(errors, sdcSchoolCollectionStudentID);

        String reasonCode = SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS.getCode();
        assertSame(reasonCode, result.getCareerProgramNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenStudentDoesNotRequireSpecialEd_UpdatesSpecialEdEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = List.of(
                SdcSchoolCollectionStudentProgramEligibilityIssueCode.DOES_NOT_NEED_SPECIAL_ED
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        var result = sdcSchoolCollectionStudentService
            .updateProgramEligibilityColumns(errors, sdcSchoolCollectionStudentID);

        String reasonCode = SdcSchoolCollectionStudentProgramEligibilityIssueCode.DOES_NOT_NEED_SPECIAL_ED.getCode();
        assertSame(reasonCode, result.getSpecialEducationNonEligReasonCode());
    }

    @Test
    void testUpdateProgramEligibilityColumns_WhenStudentIsGraduatedOrAGraduatedAdult_UpdatesSpecialEdEligibilityColumn() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = List.of(
                SdcSchoolCollectionStudentProgramEligibilityIssueCode.IS_GRADUATED
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        var result = sdcSchoolCollectionStudentService
            .updateProgramEligibilityColumns(errors, sdcSchoolCollectionStudentID);

        String reasonCode = SdcSchoolCollectionStudentProgramEligibilityIssueCode.IS_GRADUATED.getCode();
        assertSame(reasonCode, result.getSpecialEducationNonEligReasonCode());
    }

  @Test
  void testClearSdcSchoolStudentProgramEligibilityColumns_WhenColumnsAreSet_SetsAllColumnsToNull() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        String reasonCode = SdcSchoolCollectionStudentProgramEligibilityIssueCode.HOMESCHOOL.getCode();

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        mockStudentEntity.setFrenchProgramNonEligReasonCode(reasonCode);
        mockStudentEntity.setEllNonEligReasonCode(reasonCode);
        mockStudentEntity.setIndigenousSupportProgramNonEligReasonCode(reasonCode);
        mockStudentEntity.setCareerProgramNonEligReasonCode(reasonCode);
        mockStudentEntity.setSpecialEducationNonEligReasonCode(reasonCode);

        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        var result = sdcSchoolCollectionStudentService.clearSdcSchoolStudentProgramEligibilityColumns(sdcSchoolCollectionStudentID);

        // Then
        assertNull(result.getFrenchProgramNonEligReasonCode());
        assertNull(result.getEllNonEligReasonCode());
        assertNull(result.getIndigenousSupportProgramNonEligReasonCode());
        assertNull(result.getCareerProgramNonEligReasonCode());
        assertNull(result.getSpecialEducationNonEligReasonCode());
  }

    @Test
    void testSoftDeleteSdcSchoolCollectionStudents_WhenStudentExists_SavesEntity() {
        // Given
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        // When
        sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudent(sdcSchoolCollectionStudentID);

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
}
