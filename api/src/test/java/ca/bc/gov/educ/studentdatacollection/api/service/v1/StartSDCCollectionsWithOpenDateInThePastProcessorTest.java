package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionHistoryRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

class StartSDCCollectionsWithOpenDateInThePastProcessorTest extends
    BaseStudentDataCollectionAPITest {

  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  CollectionTypeCodeRepository collectionTypeCodeRepository;
  @Autowired
  SdcSchoolCollectionRepository sdcSchoolRepository;
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
    List<School> listOfSchools = new ArrayList<>();
    var school = createMockSchool();
    listOfSchools.add(school);
    CollectionTypeCodeEntity collectionTypeCode = this.collectionTypeCodeRepository.save(this.createMockCollectionCodeEntity());

    this.sdcService.startSDCCollection(collectionTypeCode, listOfSchools);

    List<CollectionEntity> collectionEntities = this.collectionRepository.findAll();
    List<SdcSchoolCollectionEntity> sdcSchoolEntities = this.sdcSchoolRepository.findAll();
    CollectionTypeCodeEntity nextCollectionCodeEntity = this.collectionTypeCodeRepository.findById("SEPTEMBER").get();
    List<SdcSchoolCollectionHistoryEntity> sdcSchoolHistoryEntities = this.sdcSchoolHistoryRepository.findAll();

    assertEquals(1 ,collectionEntities.size());
    assertEquals(1, sdcSchoolEntities.size());
    assertEquals(school.getSchoolId(), sdcSchoolEntities.get(0).getSchoolID().toString());
    assertEquals(school.getDistrictId(), sdcSchoolEntities.get(0).getDistrictID().toString());

    var collect = collectionEntities.get(0);

    assertEquals(collect.getOpenDate().plusYears(1).getYear(), nextCollectionCodeEntity.getOpenDate().getYear());
    assertEquals(collect.getOpenDate().getMonth(), nextCollectionCodeEntity.getOpenDate().getMonth());
    assertEquals(collect.getCloseDate().plusYears(1).getYear(), nextCollectionCodeEntity.getCloseDate().getYear());
    assertEquals(collect.getCloseDate().getMonth(), nextCollectionCodeEntity.getCloseDate().getMonth());

    assertEquals(sdcSchoolEntities.get(0).getSdcSchoolCollectionID(), sdcSchoolHistoryEntities.get(0).getSdcSchoolCollectionID());
  }

}
