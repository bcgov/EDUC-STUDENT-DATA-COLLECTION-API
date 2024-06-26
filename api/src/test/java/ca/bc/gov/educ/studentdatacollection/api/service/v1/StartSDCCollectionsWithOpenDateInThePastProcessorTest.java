package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartSDCCollectionsWithOpenDateInThePastProcessorTest extends
    BaseStudentDataCollectionAPITest {

  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  CollectionTypeCodeRepository collectionTypeCodeRepository;
  @Autowired
  SdcSchoolCollectionRepository sdcSchoolRepository;
  @Autowired
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  @Autowired
  SdcSchoolCollectionHistoryRepository sdcSchoolHistoryRepository;
  @Autowired
  SdcService sdcService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testStartSDCCollection_GivenPastOpenDate_ShouldCreateCollectionAndUpdateCollectionCodeOpenDateCloseDateAndCreateSdcSchoolWithHistory() {
    List<SchoolTombstone> listOfSchoolTombstones = new ArrayList<>();
    var school = createMockSchool();
    listOfSchoolTombstones.add(school);
    var district = createMockDistrict();
    school.setDistrictId(district.getDistrictId());
    CollectionTypeCodeEntity collectionTypeCode = this.collectionTypeCodeRepository.save(this.createMockCollectionCodeEntity());

    this.sdcService.startSDCCollection(collectionTypeCode, listOfSchoolTombstones);

    List<CollectionEntity> collectionEntities = this.collectionRepository.findAll();
    List<SdcSchoolCollectionEntity> sdcSchoolEntities = this.sdcSchoolRepository.findAll();
    List<SdcDistrictCollectionEntity> sdcDistrictEntities = this.sdcDistrictCollectionRepository.findAll();
    CollectionTypeCodeEntity nextCollectionCodeEntity = this.collectionTypeCodeRepository.findById("SEPTEMBER").get();
    List<SdcSchoolCollectionHistoryEntity> sdcSchoolHistoryEntities = this.sdcSchoolHistoryRepository.findAll();

    assertEquals(1 ,collectionEntities.size());
    assertEquals(1, sdcSchoolEntities.size());
    assertEquals(1, sdcDistrictEntities.size());
    assertEquals(school.getSchoolId(), sdcSchoolEntities.get(0).getSchoolID().toString());
    assertEquals(school.getDistrictId(), sdcDistrictEntities.get(0).getDistrictID().toString());
    assertEquals(district.getDistrictId(), sdcDistrictEntities.get(0).getDistrictID().toString());

    var collect = collectionEntities.get(0);

    assertEquals(collect.getOpenDate().plusYears(1).getYear(), nextCollectionCodeEntity.getOpenDate().getYear());
    assertEquals(collect.getOpenDate().getMonth(), nextCollectionCodeEntity.getOpenDate().getMonth());
    assertEquals(collect.getCloseDate().plusYears(1).getYear(), nextCollectionCodeEntity.getCloseDate().getYear());
    assertEquals(collect.getCloseDate().getMonth(), nextCollectionCodeEntity.getCloseDate().getMonth());

    assertEquals(sdcSchoolEntities.get(0).getSdcSchoolCollectionID(), sdcSchoolHistoryEntities.get(0).getSdcSchoolCollection().getSdcSchoolCollectionID());
  }

}
