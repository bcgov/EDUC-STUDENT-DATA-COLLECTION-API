package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.IndependentSchoolFundingGroupSnapshotRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.CollectionSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentSchoolFundingGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

class CloseCollectionServiceTest {

  @Mock
  private CollectionRepository collectionRepository;

  @Mock
  private SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Mock
  private RestUtils restUtils;

  @Mock
  private IndependentSchoolFundingGroupSnapshotRepository independentSchoolFundingGroupSnapshotRepository;

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
}

