package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDuplicateRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.UnsubmitSdcDistrictCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SdcDistrictCollectionServiceTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  RestUtils restUtils;
  @Autowired
  SdcDistrictCollectionService sdcDistrictCollectionService;
  @Autowired
  SdcDuplicateRepository sdcDuplicateRepository;
  @Autowired
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  @Autowired
  SdcDuplicatesService sdcDuplicateService;
  @Autowired
  SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  @Autowired
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @AfterEach
  public void after() {
    this.sdcDuplicateRepository.deleteAll();
    this.collectionRepository.deleteAll();
    this.sdcSchoolCollectionStudentRepository.deleteAll();
    this.sdcSchoolCollectionRepository.deleteAll();
  }

  @Test
  void testUnsubmitDistrictCollection_success() {
    var collection = createMockCollectionEntity();
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcDistrictCollectionEntity = createMockSdcDistrictCollectionEntity(collection, UUID.randomUUID());
    sdcDistrictCollectionEntity.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.SUBMITTED.getCode());
    sdcDistrictCollectionRepository.save(sdcDistrictCollectionEntity);

    SdcDistrictCollectionEntity result = sdcDistrictCollectionService.unsubmitDistrictCollection(UnsubmitSdcDistrictCollection.builder().sdcDistrictCollectionID(sdcDistrictCollectionEntity.getSdcDistrictCollectionID()).updateUser("USER").build());

    assertEquals(SdcDistrictCollectionStatus.D_DUP_VRFD.getCode(), result.getSdcDistrictCollectionStatusCode());
  }

  @Test
  void testUnsubmitDistrictCollection_sdcDistrictCollectionNotFound() {
    var payload = UnsubmitSdcDistrictCollection.builder().sdcDistrictCollectionID(UUID.randomUUID()).updateUser("USER").build();
    assertThrows(EntityNotFoundException.class, () -> sdcDistrictCollectionService.unsubmitDistrictCollection(payload));
  }

  @Test
  void testUnsubmitDistrictCollection_GivenSdcDistrictCollectionNotSubmittedStatus_ShouldThrowException() {
    var collection = createMockCollectionEntity();
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcDistrictCollectionEntity = createMockSdcDistrictCollectionEntity(collection, UUID.randomUUID());
    sdcDistrictCollectionEntity.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.VERIFIED.getCode());
    sdcDistrictCollectionRepository.save(sdcDistrictCollectionEntity);

    UnsubmitSdcDistrictCollection sdcDistrictCollectionUnsubmit = UnsubmitSdcDistrictCollection.builder().sdcDistrictCollectionID(sdcDistrictCollectionEntity.getSdcDistrictCollectionID()).updateUser("USER").build();
    assertThrows(InvalidPayloadException.class, () -> sdcDistrictCollectionService.unsubmitDistrictCollection(sdcDistrictCollectionUnsubmit));
  }
}
