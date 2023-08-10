package ca.bc.gov.educ.studentdatacollection.api.service.v1;

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
import java.util.Arrays;
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
        sdcSchoolCollectionStudentService.updateFteColumns(fteCalculationResult, sdcSchoolCollectionStudentID);

        // Then
        // Verify that the save method is called once with the correct entity
        verify(sdcSchoolCollectionStudentRepository, times(1)).save(mockStudentEntity);

        // Assert that the FTE and FTE zero reason code are updated as expected
        assertSame(fteCalculationResult.getFte(), mockStudentEntity.getFte());
        assertSame(fteCalculationResult.getFteZeroReason(), mockStudentEntity.getFteZeroReasonCode());
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
        assertThrows(EntityNotFoundException.class, () -> {
            sdcSchoolCollectionStudentService.updateFteColumns(fteCalculationResult, sdcSchoolCollectionStudentID);
        });
    }

    @Test
    void testupdateProgramEligibilityColumns_WhenNotEligibleForAnything_UpdatesAllColumns() {
        UUID sdcSchoolCollectionStudentID = UUID.randomUUID();
        List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = Arrays.asList(
            SdcSchoolCollectionStudentProgramEligibilityIssueCode.OFFSHORE
        );

        // Create a mock SdcSchoolCollectionStudentEntity
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        mockStudentEntity.setSdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
        when(sdcSchoolCollectionStudentRepository.findById(any())).thenReturn(Optional.of(mockStudentEntity));

        sdcSchoolCollectionStudentService
            .updateProgramEligibilityColumns(errors, sdcSchoolCollectionStudentID);

        String reasonCode = SdcSchoolCollectionStudentProgramEligibilityIssueCode.OFFSHORE.getCode();
        assertSame(reasonCode, mockStudentEntity.getFrenchProgramNonEligReasonCode());
        assertSame(reasonCode, mockStudentEntity.getEllNonEligReasonCode());
        assertSame(reasonCode, mockStudentEntity.getIndigenousSupportProgramNonEligReasonCode());
        assertSame(reasonCode, mockStudentEntity.getCareerProgramNonEligReasonCode());
        assertSame(reasonCode, mockStudentEntity.getSpecialEducationNonEligReasonCode());
    }
}
