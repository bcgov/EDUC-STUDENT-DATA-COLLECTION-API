package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.UnsubmitSdcSchoolCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
