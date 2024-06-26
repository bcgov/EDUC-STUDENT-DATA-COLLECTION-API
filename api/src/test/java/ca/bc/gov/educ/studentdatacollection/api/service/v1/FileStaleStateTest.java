package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.batch.service.SdcFileService;
import ca.bc.gov.educ.studentdatacollection.api.controller.v1.SdcFileController;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class FileStaleStateTest {
  @Mock
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Mock
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  @Mock
  SdcFileService sdcFileService;

  @InjectMocks
  SdcFileController sdcFileController;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testProcessSdcFileTest_givenInvalidPayloadForDates_ShouldReturnStatusBadRequest() throws Exception {
    var collection = createMockCollectionEntity();
    var school = this.createMockSchool();
    var districtID = UUID.randomUUID();
    var districtCollection = createMockSdcDistrictCollectionEntity(collection, districtID);
    school.setDistrictId(districtID.toString());
    var schoolColl = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));

    when(sdcDistrictCollectionRepository.findBySdcDistrictCollectionID(any(UUID.class))).thenReturn(Optional.ofNullable(districtCollection));
    when(sdcSchoolCollectionRepository.findActiveCollectionBySchoolId(any(UUID.class))).thenReturn(Optional.ofNullable(schoolColl));

    when(sdcFileService.runDistrictFileLoad(any(SdcFileUpload.class), anyString())).thenThrow(ObjectOptimisticLockingFailureException.class);

    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().createUser("ABC").fileContents(fileContents).fileName("SampleUpload.std").build();

    assert districtCollection != null;
    assertThat(districtCollection.getSdcDistrictCollectionID()).isNotNull();
    assertThrows(InvalidPayloadException.class, () -> sdcFileController.processDistrictSdcBatchFile(body, "" + districtCollection.getSdcDistrictCollectionID(), ""));
  }

  public CollectionEntity createMockCollectionEntity(){
    CollectionEntity sdcEntity = new CollectionEntity();
    sdcEntity.setCollectionTypeCode("SEPTEMBER");
    sdcEntity.setOpenDate(LocalDateTime.now());
    sdcEntity.setCloseDate(LocalDateTime.now().plusDays(5));
    return sdcEntity;
  }

  public SchoolTombstone createMockSchool() {
    final SchoolTombstone schoolTombstone = new SchoolTombstone();
    schoolTombstone.setSchoolId(UUID.randomUUID().toString());
    schoolTombstone.setDistrictId(UUID.randomUUID().toString());
    schoolTombstone.setMincode("03636018");
    return schoolTombstone;
  }

  public SdcDistrictCollectionEntity createMockSdcDistrictCollectionEntity(CollectionEntity entity, UUID districtID){
    SdcDistrictCollectionEntity sdcEntity = new SdcDistrictCollectionEntity();
    sdcEntity.setCollectionEntity(entity);
    sdcEntity.setSdcDistrictCollectionID(UUID.randomUUID());
    sdcEntity.setSdcDistrictCollectionStatusCode("NEW");
    sdcEntity.setDistrictID(districtID == null ? UUID.randomUUID() : districtID);
    return sdcEntity;
  }

  public SdcSchoolCollectionEntity createMockSdcSchoolCollectionEntity(CollectionEntity entity, UUID schoolID){
    SdcSchoolCollectionEntity sdcEntity = new SdcSchoolCollectionEntity();
    sdcEntity.setCollectionEntity(entity);
    sdcEntity.setSchoolID(schoolID == null ? UUID.randomUUID() : schoolID);
    return sdcEntity;
  }

}
