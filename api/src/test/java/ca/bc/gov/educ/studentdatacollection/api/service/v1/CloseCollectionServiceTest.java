package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.IndependentSchoolFundingGroupSnapshotRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcStudentEllRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.CollectionSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.UpdateStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentSchoolFundingGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class CloseCollectionServiceTest {

  @Mock
  private CollectionRepository collectionRepository;

  @Mock
  private SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Mock
  private RestUtils restUtils;

  @Mock
  private IndependentSchoolFundingGroupSnapshotRepository independentSchoolFundingGroupSnapshotRepository;

  @Mock
  private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  @Mock
  private SdcSchoolCollectionStudentStorageService sdcSchoolCollectionStudentStorageService;

  @Mock
  private ValidationRulesService validationRulesService;

  @Mock
  private SdcStudentEllRepository sdcStudentEllRepository;

  @InjectMocks
  private CloseCollectionService closeCollectionService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testSaveIndependentSchoolFundingGroupSnapshot_Success() {
    CollectionEntity collectionEntity = new CollectionEntity();
    UUID existingCollectionID = UUID.randomUUID();
    collectionEntity.setCollectionID(existingCollectionID);

    when(collectionRepository.findById(existingCollectionID)).thenReturn(Optional.of(collectionEntity));

    SdcSchoolCollectionEntity schoolEntity = new SdcSchoolCollectionEntity();
    UUID schoolID = UUID.randomUUID();
    schoolEntity.setSchoolID(schoolID);

    when(sdcSchoolCollectionRepository.findSchoolsInCollectionWithStatus(collectionEntity.getCollectionID()))
            .thenReturn(List.of(schoolEntity));

    IndependentSchoolFundingGroup fundingGroup = new IndependentSchoolFundingGroup();

    fundingGroup.setSchoolFundingGroupID(String.valueOf(schoolID));
    fundingGroup.setSchoolID(String.valueOf(schoolID));
    fundingGroup.setSchoolGradeCode("10");
    fundingGroup.setSchoolFundingGroupCode("A");

    when(restUtils.getSchoolFundingGroupsBySchoolID(String.valueOf(schoolID)))
            .thenReturn(List.of(fundingGroup));

    CollectionSagaData sagaData = new CollectionSagaData();
    sagaData.setExistingCollectionID(String.valueOf(existingCollectionID));

    closeCollectionService.saveIndependentSchoolFundingGroupSnapshot(sagaData);

    verify(collectionRepository).findById(existingCollectionID);
    verify(sdcSchoolCollectionRepository).findSchoolsInCollectionWithStatus(collectionEntity.getCollectionID());
    verify(restUtils).getSchoolFundingGroupsBySchoolID(String.valueOf(schoolID));
    verify(independentSchoolFundingGroupSnapshotRepository).saveAll(any());
  }

  @Test
  void testSaveIndependentSchoolFundingGroupSnapshot_CollectionNotFound() {
    UUID existingCollectionID = UUID.randomUUID();
    when(collectionRepository.findById(existingCollectionID))
            .thenReturn(Optional.empty());

    CollectionSagaData sagaData = new CollectionSagaData();
    sagaData.setExistingCollectionID(String.valueOf(existingCollectionID));

    assertThrows(EntityNotFoundException.class, () -> closeCollectionService.saveIndependentSchoolFundingGroupSnapshot(sagaData));

    verify(sdcSchoolCollectionRepository, never()).findSchoolsInCollectionWithStatus(any());
    verify(restUtils, never()).getSchoolFundingGroupsBySchoolID(anyString());
    verify(independentSchoolFundingGroupSnapshotRepository, never()).saveAll(any());
  }

  @Test
  void testUpdateELLAndMarkStudentAsCompleted_IncrementsEllYearsForIndependentSchoolReason() {
    final var student = createEllStudent(ProgramEligibilityIssueCode.ELL_INDY_SCHOOL.getCode(), 4);
    final var existingEllRecord = SdcStudentEllEntity.builder()
            .studentID(student.getAssignedStudentId())
            .yearsInEll(4)
            .build();

    when(sdcSchoolCollectionStudentRepository.findById(student.getSdcSchoolCollectionStudentID())).thenReturn(Optional.of(student));
    when(validationRulesService.splitEnrolledProgramsString(student.getEnrolledProgramCodes())).thenReturn(List.of("17"));
    when(validationRulesService.getStudentYearsInEll(student.getAssignedStudentId())).thenReturn(Optional.of(existingEllRecord));

    closeCollectionService.updateELLAndMarkStudentAsCompleted(createUpdateStudentSagaData(student));

    verify(sdcStudentEllRepository).save(argThat(ell -> ell.getStudentID().equals(student.getAssignedStudentId()) && ell.getYearsInEll() == 5));
    verify(sdcSchoolCollectionStudentStorageService).saveSdcStudentWithHistory(argThat((SdcSchoolCollectionStudentEntity savedStudent) ->
            SdcSchoolStudentStatus.COMPLETED.getCode().equals(savedStudent.getSdcSchoolCollectionStudentStatusCode())
    ));
  }

  @Test
  void testUpdateELLAndMarkStudentAsCompleted_IncrementsEllYearsForFiveYearReason() {
    final var student = createEllStudent(ProgramEligibilityIssueCode.YEARS_IN_ELL.getCode(), 5);
    final var existingEllRecord = SdcStudentEllEntity.builder()
            .studentID(student.getAssignedStudentId())
            .yearsInEll(5)
            .build();

    when(sdcSchoolCollectionStudentRepository.findById(student.getSdcSchoolCollectionStudentID())).thenReturn(Optional.of(student));
    when(validationRulesService.splitEnrolledProgramsString(student.getEnrolledProgramCodes())).thenReturn(List.of("17"));
    when(validationRulesService.getStudentYearsInEll(student.getAssignedStudentId())).thenReturn(Optional.of(existingEllRecord));

    closeCollectionService.updateELLAndMarkStudentAsCompleted(createUpdateStudentSagaData(student));

    verify(sdcStudentEllRepository).save(argThat(ell -> ell.getStudentID().equals(student.getAssignedStudentId()) && ell.getYearsInEll() == 6));
    verify(sdcSchoolCollectionStudentStorageService).saveSdcStudentWithHistory(any(SdcSchoolCollectionStudentEntity.class));
  }

  @Test
  void testUpdateELLAndMarkStudentAsCompleted_CreatesEllRecordFromSnapshotYearsWhenMissing() {
    final var student = createEllStudent(ProgramEligibilityIssueCode.YEARS_IN_ELL.getCode(), 5);

    when(sdcSchoolCollectionStudentRepository.findById(student.getSdcSchoolCollectionStudentID())).thenReturn(Optional.of(student));
    when(validationRulesService.splitEnrolledProgramsString(student.getEnrolledProgramCodes())).thenReturn(List.of("17"));
    when(validationRulesService.getStudentYearsInEll(student.getAssignedStudentId())).thenReturn(Optional.empty());

    closeCollectionService.updateELLAndMarkStudentAsCompleted(createUpdateStudentSagaData(student));

    verify(sdcStudentEllRepository).save(argThat(ell ->
            ell.getStudentID().equals(student.getAssignedStudentId())
                    && ell.getYearsInEll() == 6
                    && ell.getCreateUser() != null
                    && ell.getUpdateUser() != null
    ));
  }

  @Test
  void testUpdateELLAndMarkStudentAsCompleted_DoesNotIncrementEllYearsForOtherNonEligibleReason() {
    final var student = createEllStudent(ProgramEligibilityIssueCode.ZERO_COURSES_ADULT_ELL.getCode(), 5);

    when(sdcSchoolCollectionStudentRepository.findById(student.getSdcSchoolCollectionStudentID())).thenReturn(Optional.of(student));
    when(validationRulesService.splitEnrolledProgramsString(student.getEnrolledProgramCodes())).thenReturn(List.of("17"));

    closeCollectionService.updateELLAndMarkStudentAsCompleted(createUpdateStudentSagaData(student));

    verify(validationRulesService, never()).getStudentYearsInEll(any(UUID.class));
    verify(sdcStudentEllRepository, never()).save(any(SdcStudentEllEntity.class));
    verify(sdcSchoolCollectionStudentStorageService).saveSdcStudentWithHistory(argThat((SdcSchoolCollectionStudentEntity savedStudent) ->
            SdcSchoolStudentStatus.COMPLETED.getCode().equals(savedStudent.getSdcSchoolCollectionStudentStatusCode())
    ));
  }

  private SdcSchoolCollectionStudentEntity createEllStudent(String ellNonEligReasonCode, Integer yearsInEll) {
    final var student = new SdcSchoolCollectionStudentEntity();
    student.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    student.setAssignedStudentId(UUID.randomUUID());
    student.setEnrolledProgramCodes("17");
    student.setEllNonEligReasonCode(ellNonEligReasonCode);
    student.setYearsInEll(yearsInEll);
    student.setSdcSchoolCollectionStudentStatusCode("LOADED");
    return student;
  }

  private UpdateStudentSagaData createUpdateStudentSagaData(SdcSchoolCollectionStudentEntity student) {
    final var sagaData = new UpdateStudentSagaData();
    sagaData.setSdcSchoolCollectionStudentID(student.getSdcSchoolCollectionStudentID().toString());
    return sagaData;
  }
}
