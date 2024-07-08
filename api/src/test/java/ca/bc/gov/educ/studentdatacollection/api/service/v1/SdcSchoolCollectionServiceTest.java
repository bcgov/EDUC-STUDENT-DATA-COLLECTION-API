package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ReportZeroEnrollmentSdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.UnsubmitSdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValidationIssueTypeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    when(sdcDistrictCollectionRepository.findBySdcDistrictCollectionID(sdcDistrictCollectionID)).thenReturn(Optional.of(sdcDistrictCollectionEntity));

    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.unsubmitSchoolCollection(UnsubmitSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build());

    assertEquals(SdcSchoolCollectionStatus.DUP_VRFD.getCode(), result.getSdcSchoolCollectionStatusCode());
    assertEquals(SdcDistrictCollectionStatus.LOADED.getCode(), sdcDistrictCollectionEntity.getSdcDistrictCollectionStatusCode());

    verify(sdcSchoolCollectionRepository, times(2)).findById(sdcSchoolCollectionID);
    verify(sdcDistrictCollectionRepository, times(1)).findBySdcDistrictCollectionID(sdcDistrictCollectionID);
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
    when(sdcDistrictCollectionRepository.findBySdcDistrictCollectionID(sdcDistrictCollectionID)).thenReturn(Optional.empty());

    UnsubmitSdcSchoolCollection sdcSchoolCollectionUnsubmit = UnsubmitSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build();

    assertThrows(EntityNotFoundException.class, () -> sdcSchoolCollectionService.unsubmitSchoolCollection(sdcSchoolCollectionUnsubmit));

    verify(sdcSchoolCollectionRepository, times(1)).findById(sdcSchoolCollectionID);
    verify(sdcDistrictCollectionRepository, times(1)).findBySdcDistrictCollectionID(sdcDistrictCollectionID);
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
    when(sdcDistrictCollectionRepository.findBySdcDistrictCollectionID(sdcDistrictCollectionID)).thenReturn(Optional.of(sdcDistrictCollectionEntity));

    SdcSchoolCollectionEntity result = sdcSchoolCollectionService.unsubmitSchoolCollection(UnsubmitSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcSchoolCollectionID).updateUser("USER").build());

    assertEquals(SdcSchoolCollectionStatus.DUP_VRFD.getCode(), result.getSdcSchoolCollectionStatusCode());
    assertEquals(SdcDistrictCollectionStatus.LOADED.getCode(), sdcDistrictCollectionEntity.getSdcDistrictCollectionStatusCode());

    verify(sdcSchoolCollectionRepository, times(2)).findById(sdcSchoolCollectionID);
    verify(sdcDistrictCollectionRepository, times(1)).findBySdcDistrictCollectionID(sdcDistrictCollectionID);
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

    verify(sdcSchoolCollectionRepository, times(2)).findById(sdcSchoolCollectionID);
    verify(sdcDistrictCollectionRepository, times(0)).findBySdcDistrictCollectionID(any());
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

    assertThrows(EntityNotFoundException.class, () -> {
      sdcSchoolCollectionService.reportZeroEnrollment(input);
    });
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
}
