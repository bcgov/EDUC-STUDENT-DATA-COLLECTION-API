package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.ReprocessSdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ReportZeroEnrollmentSdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.UnsubmitSdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValidationIssueTypeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SdcSchoolCollectionServiceTest {

  @Mock
  private SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Mock
  private SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  @Mock
  private SdcDistrictCollectionService sdcDistrictCollectionService;

  @Mock
  private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  @Mock
  SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;

  @Mock
  SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService;

  @Mock
  SdcSchoolCollectionStudentValidationIssueRepository sdcSchoolCollectionStudentValidationIssueRepository;

  @Mock
  SdcDuplicateRepository sdcDuplicateRepository;

  @Mock
  SagaRepository sagaRepository;

  @Mock
  SdcSchoolCollectionStudentStorageService sdcSchoolCollectionStudentStorageService;

  @InjectMocks
  private SdcSchoolCollectionService sdcSchoolCollectionService;

  @Test
  void testUnsubmitSchoolCollection_success() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    UUID sdcDistrictCollectionID = UUID.randomUUID();

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());

    SdcDistrictCollectionEntity sdcDistrictCollectionEntity = new SdcDistrictCollectionEntity();
    sdcDistrictCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcDistrictCollectionEntity.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.SUBMITTED.getCode());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));
    when(sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID)).thenReturn(Optional.of(sdcDistrictCollectionEntity));

    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.unsubmitSchoolCollection(UnsubmitSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build());

    assertEquals(SdcSchoolCollectionStatus.DUP_VRFD.getCode(), result.getSdcSchoolCollectionStatusCode());
    assertEquals(SdcDistrictCollectionStatus.LOADED.getCode(), sdcDistrictCollectionEntity.getSdcDistrictCollectionStatusCode());

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verify(sdcDistrictCollectionRepository, times(1)).findById(sdcDistrictCollectionID);
    verify(sdcSchoolCollectionRepository, times(1)).save(any(SdcSchoolCollectionEntity.class));
    verify(sdcDistrictCollectionService, times(1)).updateSdcDistrictCollection(any(SdcDistrictCollectionEntity.class));
  }

  @Test
  void testUnsubmitSchoolCollection_sdcSchoolCollectionNotFound() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.empty());

    UnsubmitSdcSchoolCollection sdcSchoolCollectionUnsubmit = UnsubmitSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build();

    assertThrows(EntityNotFoundException.class, () -> sdcSchoolCollectionService.unsubmitSchoolCollection(sdcSchoolCollectionUnsubmit));

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verifyNoMoreInteractions(sdcSchoolCollectionRepository, sdcDistrictCollectionRepository, sdcDistrictCollectionService);
  }

  @Test
  void testUnsubmitSchoolCollection_sdcDistrictCollectionNotFound() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    UUID sdcDistrictCollectionID = UUID.randomUUID();

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));
    when(sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID)).thenReturn(Optional.empty());

    UnsubmitSdcSchoolCollection sdcSchoolCollectionUnsubmit = UnsubmitSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build();

    assertThrows(EntityNotFoundException.class, () -> sdcSchoolCollectionService.unsubmitSchoolCollection(sdcSchoolCollectionUnsubmit));

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verify(sdcDistrictCollectionRepository, times(1)).findById(sdcDistrictCollectionID);
    verifyNoMoreInteractions(sdcSchoolCollectionRepository, sdcDistrictCollectionRepository, sdcDistrictCollectionService);
  }

  @Test
  void testUnsubmitSchoolCollection_GivenSdcDistrictCollectionNotSubmittedStatus_ShouldThrowException() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    UUID sdcDistrictCollectionID = UUID.randomUUID();

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.VERIFIED.getCode());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    UnsubmitSdcSchoolCollection sdcSchoolCollectionUnsubmit = UnsubmitSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build();
    assertThrows(InvalidPayloadException.class, () -> sdcSchoolCollectionService.unsubmitSchoolCollection(sdcSchoolCollectionUnsubmit));

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verifyNoMoreInteractions(sdcSchoolCollectionRepository, sdcDistrictCollectionRepository, sdcDistrictCollectionService);
  }

  @Test
  void testUnsubmitSchoolCollection_noStatusUpdateNeeded() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    UUID sdcDistrictCollectionID = UUID.randomUUID();

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());

    SdcDistrictCollectionEntity sdcDistrictCollectionEntity = new SdcDistrictCollectionEntity();
    sdcDistrictCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcDistrictCollectionEntity.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.LOADED.getCode());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));
    when(sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID)).thenReturn(Optional.of(sdcDistrictCollectionEntity));

    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.unsubmitSchoolCollection(UnsubmitSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build());

    assertEquals(SdcSchoolCollectionStatus.DUP_VRFD.getCode(), result.getSdcSchoolCollectionStatusCode());
    assertEquals(SdcDistrictCollectionStatus.LOADED.getCode(), sdcDistrictCollectionEntity.getSdcDistrictCollectionStatusCode());

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verify(sdcDistrictCollectionRepository, times(1)).findById(sdcDistrictCollectionID);
    verify(sdcSchoolCollectionRepository, times(1)).save(any(SdcSchoolCollectionEntity.class));
    verifyNoMoreInteractions(sdcDistrictCollectionService);
  }

  @Test
  void testUnsubmitSchoolCollection_indySchool() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.unsubmitSchoolCollection(UnsubmitSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build());

    assertEquals(SdcSchoolCollectionStatus.SCH_C_VRFD.getCode(), result.getSdcSchoolCollectionStatusCode());

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verify(sdcDistrictCollectionRepository, times(0)).findById(any());
    verify(sdcSchoolCollectionRepository, times(1)).save(any(SdcSchoolCollectionEntity.class));
    verifyNoMoreInteractions(sdcDistrictCollectionService);
  }

  @Test
  void testReportZeroEnrollment_NonExistentSchoolCollection() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.empty());

    ReportZeroEnrollmentSdcSchoolCollection input = ReportZeroEnrollmentSdcSchoolCollection.builder()
            .sdcSchoolCollectionID(sdcSchoolCollectionID)
            .updateUser("USER")
            .build();

    assertThrows(EntityNotFoundException.class, () -> sdcSchoolCollectionService.reportZeroEnrollment(input));
  }

  @Test
  void testReportZeroEnrollment_EmptyStudentSet() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolStudentEntities(new HashSet<>());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.reportZeroEnrollment(ReportZeroEnrollmentSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build());

    assertTrue(result.getSDCSchoolStudentEntities().isEmpty());
  }

  @Test
  void testReportZeroEnrollment_SetToSubmitted() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.reportZeroEnrollment(ReportZeroEnrollmentSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build());

    assertEquals(SdcSchoolCollectionStatus.SUBMITTED.getCode(), result.getSdcSchoolCollectionStatusCode());
  }

  @Test
  void testReportZeroEnrollment_RemoveStudents() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);

    Set<SdcSchoolCollectionStudentEntity> studentEntities = new HashSet<>();
    SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
    mockStudentEntity.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    studentEntities.add(mockStudentEntity);
    sdcSchoolCollectionEntity.setSdcSchoolStudentEntities(studentEntities);

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.reportZeroEnrollment(ReportZeroEnrollmentSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build());

    assertTrue(result.getSDCSchoolStudentEntities().isEmpty());
  }

  @Test
  void testReportZeroEnrollment_HistoryIsWritten() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    String updateUser = "USER";

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionHistoryEntities(new HashSet<>());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    SdcSchoolCollectionHistoryEntity mockHistoryEntity = new SdcSchoolCollectionHistoryEntity();
    when(sdcSchoolCollectionHistoryService.createSDCSchoolHistory(sdcSchoolCollectionEntity, updateUser)).thenReturn(mockHistoryEntity);

    ReportZeroEnrollmentSdcSchoolCollection input = ReportZeroEnrollmentSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser(updateUser).build();
    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.reportZeroEnrollment(input);

    verify(sdcSchoolCollectionHistoryService).createSDCSchoolHistory(sdcSchoolCollectionEntity, updateUser);

    assertTrue(result.getSdcSchoolCollectionHistoryEntities().contains(mockHistoryEntity), "The history entity should be added to the school collection");
  }

  @Test
  void testGetStudentValidationIssueCodes_withValidCodes() {
    List<String> issueCodes = List.of("LOCALIDBLANK", "DOBBLANK");
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    when(sdcSchoolCollectionStudentValidationIssueRepository.findAllValidationIssueCodesBySdcSchoolCollectionID(sdcSchoolCollectionID)).thenReturn(issueCodes);

    List<ValidationIssueTypeCode> result = sdcSchoolCollectionService.getStudentValidationIssueCodes(sdcSchoolCollectionID);

    assertEquals(2, result.size());
    assertEquals("DOBBLANK", result.get(0).getValidationIssueTypeCode());
    assertEquals("Birthdate cannot be blank.", result.get(0).getMessage());
    assertEquals("ERROR", result.get(0).getSeverityTypeCode());
    assertEquals("LOCALIDBLANK", result.get(1).getValidationIssueTypeCode());
    assertEquals("Local identifier number is blank.", result.get(1).getMessage());
    assertEquals("INFO_WARNING", result.get(1).getSeverityTypeCode());

  }

  @Test
  void testGetStudentValidationIssueCodes_withNullCodes() {
    List<String> issueCodes = List.of("LOCALIDBLANK", "DOBBLANK", "FAKECODE");
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    when(sdcSchoolCollectionStudentValidationIssueRepository.findAllValidationIssueCodesBySdcSchoolCollectionID(sdcSchoolCollectionID)).thenReturn(issueCodes);

    List<ValidationIssueTypeCode> result = sdcSchoolCollectionService.getStudentValidationIssueCodes(sdcSchoolCollectionID);

    assertEquals(2, result.size());
    assertEquals("DOBBLANK", result.get(0).getValidationIssueTypeCode());
    assertEquals("Birthdate cannot be blank.", result.get(0).getMessage());
    assertEquals("ERROR", result.get(0).getSeverityTypeCode());
    assertEquals("LOCALIDBLANK", result.get(1).getValidationIssueTypeCode());
    assertEquals("Local identifier number is blank.", result.get(1).getMessage());
    assertEquals("INFO_WARNING", result.get(1).getSeverityTypeCode());
  }

  @Test
  void testGetStudentValidationIssueCodes_withEmptyList() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    when(sdcSchoolCollectionStudentValidationIssueRepository.findAllValidationIssueCodesBySdcSchoolCollectionID(sdcSchoolCollectionID)).thenReturn(List.of());

    List<ValidationIssueTypeCode> result = sdcSchoolCollectionService.getStudentValidationIssueCodes(sdcSchoolCollectionID);

    assertEquals(0, result.size());
  }

  @Test
  void testReprocessSchoolCollection_sdcSchoolCollectionNotFound() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.empty());

    ReprocessSdcSchoolCollection sdcSchoolCollectionReprocess = ReprocessSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build();

    assertThrows(EntityNotFoundException.class, () -> sdcSchoolCollectionService.reprocessSchoolCollection(sdcSchoolCollectionReprocess));

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verifyNoMoreInteractions(sdcSchoolCollectionRepository, sdcDistrictCollectionRepository, sdcDistrictCollectionService);
  }

  @Test
  void testReprocessSchoolCollection_indySchool() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());
    sdcSchoolCollectionEntity.setSdcSchoolStudentEntities(new HashSet<>());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.reprocessSchoolCollection(ReprocessSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build());

    assertEquals(sdcSchoolCollectionID, result.getSdcSchoolCollectionID());

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verify(sdcDistrictCollectionRepository, times(0)).findById(any());
    verify(sdcSchoolCollectionStudentStorageService, times(1)).saveAllSDCStudentsWithHistory(any());
  }

  @Test
  void testReprocessSchoolCollection_withStudents_notLoadedStudents_ShouldProcessNonDeletedStudents() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    String updateUser = "TEST_USER";

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());

    SdcSchoolCollectionStudentEntity errorStudent = new SdcSchoolCollectionStudentEntity();
    errorStudent.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    errorStudent.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.getCode());

    SdcSchoolCollectionStudentEntity deletedStudent = new SdcSchoolCollectionStudentEntity();
    deletedStudent.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    deletedStudent.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.getCode());

    Set<SdcSchoolCollectionStudentEntity> students = new HashSet<>();
    students.add(errorStudent);
    students.add(deletedStudent);
    sdcSchoolCollectionEntity.setSdcSchoolStudentEntities(students);

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    sdcSchoolCollectionService.reprocessSchoolCollection(
        ReprocessSdcSchoolCollection.builder()
            .sdcSchoolCollectionID(sdcSchoolCollectionID)
            .updateUser(updateUser)
            .build()
    );

    assertEquals(SdcSchoolStudentStatus.LOADED.getCode(), errorStudent.getSdcSchoolCollectionStudentStatusCode());
    assertEquals(updateUser, errorStudent.getUpdateUser());

    assertEquals(SdcSchoolStudentStatus.DELETED.getCode(), deletedStudent.getSdcSchoolCollectionStudentStatusCode());
    assertNotEquals(updateUser, deletedStudent.getUpdateUser());

    verify(sdcSchoolCollectionStudentStorageService, times(1)).saveAllSDCStudentsWithHistory(any());
  }

  @Test
  void testReprocessSchoolCollection_withLoadedStudents_ShouldThrowLoadedStudentException() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    String updateUser = "TEST_USER";

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());

    SdcSchoolCollectionStudentEntity loadedStudent = new SdcSchoolCollectionStudentEntity();
    loadedStudent.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    loadedStudent.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.LOADED.getCode());

    SdcSchoolCollectionStudentEntity errorStudent = new SdcSchoolCollectionStudentEntity();
    errorStudent.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    errorStudent.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.getCode());

    SdcSchoolCollectionStudentEntity deletedStudent = new SdcSchoolCollectionStudentEntity();
    deletedStudent.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    deletedStudent.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.getCode());

    Set<SdcSchoolCollectionStudentEntity> students = new HashSet<>();
    students.add(loadedStudent);
    students.add(errorStudent);
    students.add(deletedStudent);
    sdcSchoolCollectionEntity.setSdcSchoolStudentEntities(students);

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    ReprocessSdcSchoolCollection sdcSchoolCollectionReprocess = ReprocessSdcSchoolCollection.builder()
            .sdcSchoolCollectionID(sdcSchoolCollectionID)
            .updateUser("USER")
            .build();

    assertThrows(InvalidPayloadException.class, () -> sdcSchoolCollectionService.reprocessSchoolCollection(sdcSchoolCollectionReprocess));
  }

  @Test
  void testReprocessSchoolCollection_withOnlyDeletedStudents_ShouldNotProcessAnyStudents() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    String updateUser = "TEST_USER";

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());

    SdcSchoolCollectionStudentEntity deletedStudent1 = new SdcSchoolCollectionStudentEntity();
    deletedStudent1.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    deletedStudent1.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.getCode());

    SdcSchoolCollectionStudentEntity deletedStudent2 = new SdcSchoolCollectionStudentEntity();
    deletedStudent2.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    deletedStudent2.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.getCode());

    Set<SdcSchoolCollectionStudentEntity> students = new HashSet<>();
    students.add(deletedStudent1);
    students.add(deletedStudent2);
    sdcSchoolCollectionEntity.setSdcSchoolStudentEntities(students);

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    sdcSchoolCollectionService.reprocessSchoolCollection(
        ReprocessSdcSchoolCollection.builder()
            .sdcSchoolCollectionID(sdcSchoolCollectionID)
            .updateUser(updateUser)
            .build()
    );

    assertEquals(SdcSchoolStudentStatus.DELETED.getCode(), deletedStudent1.getSdcSchoolCollectionStudentStatusCode());
    assertEquals(SdcSchoolStudentStatus.DELETED.getCode(), deletedStudent2.getSdcSchoolCollectionStudentStatusCode());
    assertNotEquals(updateUser, deletedStudent1.getUpdateUser());
    assertNotEquals(updateUser, deletedStudent2.getUpdateUser());

    verify(sdcSchoolCollectionStudentStorageService, times(1)).saveAllSDCStudentsWithHistory(any());
  }

  @Test
  void testReprocessSchoolCollection_GivenProvincialDuplicatesStatus_ShouldThrowException() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.P_DUP_POST.getCode());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    ReprocessSdcSchoolCollection sdcSchoolCollectionReprocess = ReprocessSdcSchoolCollection.builder()
        .sdcSchoolCollectionID(sdcSchoolCollectionID)
        .updateUser("USER")
        .build();

    assertThrows(InvalidPayloadException.class, () -> sdcSchoolCollectionService.reprocessSchoolCollection(sdcSchoolCollectionReprocess));

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verifyNoMoreInteractions(sdcSchoolCollectionRepository, sdcDistrictCollectionRepository, sdcDistrictCollectionService);
  }

  @Test
  void testReprocessSchoolCollection_GivenProvincialDuplicatesVerifiedStatus_ShouldThrowException() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.P_DUP_VRFD.getCode());

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    ReprocessSdcSchoolCollection sdcSchoolCollectionReprocess = ReprocessSdcSchoolCollection.builder()
        .sdcSchoolCollectionID(sdcSchoolCollectionID)
        .updateUser("USER")
        .build();

    assertThrows(InvalidPayloadException.class, () -> sdcSchoolCollectionService.reprocessSchoolCollection(sdcSchoolCollectionReprocess));

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verifyNoMoreInteractions(sdcSchoolCollectionRepository, sdcDistrictCollectionRepository, sdcDistrictCollectionService);
  }

  @Test
  void testReprocessSchoolCollection_ShouldClearCalculatedFields() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    String updateUser = "TEST_USER";

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());

    SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
    student.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.VERIFIED.getCode());
    student.setFte(BigDecimal.ONE);
    student.setFteZeroReasonCode("REASON");

    Set<SdcSchoolCollectionStudentEntity> students = new HashSet<>();
    students.add(student);
    sdcSchoolCollectionEntity.setSdcSchoolStudentEntities(students);

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    sdcSchoolCollectionService.reprocessSchoolCollection(
        ReprocessSdcSchoolCollection.builder()
            .sdcSchoolCollectionID(sdcSchoolCollectionID)
            .updateUser(updateUser)
            .build()
    );

    assertEquals(SdcSchoolStudentStatus.LOADED.getCode(), student.getSdcSchoolCollectionStudentStatusCode());
    assertEquals(updateUser, student.getUpdateUser());

    verify(sdcSchoolCollectionStudentStorageService, times(1)).saveAllSDCStudentsWithHistory(any());
  }

  @Test
  void testReprocessSchoolCollection_ShouldUpdateTimestamps() {
    UUID sdcSchoolCollectionID = UUID.randomUUID();
    String updateUser = "TEST_USER";

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = new SdcSchoolCollectionEntity();
    sdcSchoolCollectionEntity.setSdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());

    SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
    student.setSdcSchoolCollectionStudentID(UUID.randomUUID());
    student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.getCode());

    Set<SdcSchoolCollectionStudentEntity> students = new HashSet<>();
    students.add(student);
    sdcSchoolCollectionEntity.setSdcSchoolStudentEntities(students);

    when(sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID)).thenReturn(Optional.of(sdcSchoolCollectionEntity));

    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.reprocessSchoolCollection(
        ReprocessSdcSchoolCollection.builder()
            .sdcSchoolCollectionID(sdcSchoolCollectionID)
            .updateUser(updateUser)
            .build()
    );

    assertEquals(updateUser, result.getUpdateUser());
    assertEquals(updateUser, student.getUpdateUser());
    assertNotNull(student.getUpdateDate());

    verify(sdcSchoolCollectionStudentStorageService, times(1)).saveAllSDCStudentsWithHistory(any());
  }
}
