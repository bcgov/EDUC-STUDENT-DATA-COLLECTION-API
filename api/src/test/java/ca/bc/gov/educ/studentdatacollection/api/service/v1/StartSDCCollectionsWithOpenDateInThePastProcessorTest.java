package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

class StartSDCCollectionsWithOpenDateInThePastProcessorTest extends
    BaseStudentDataCollectionAPITest {

  @Autowired
  StartSDCCollectionsWithOpenDateInThePastProcessingHandler service;

  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  CollectionTypeCodeRepository collectionCodeRepository;
  @Autowired
  SdcSchoolCollectionRepository sdcSchoolRepository;

  @Autowired
  SdcSchoolCollectionHistoryRepository sdcSchoolHistoryRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @BeforeEach
  public void before() {
    this.collectionCodeRepository.save(this.createCollectionCodeData());
  }

  @Test
  void testStartSDCCollection_GivenPastOpenDate_ShouldCreateCollectionAndUpdateCollectionCodeOpenDateCloseDateAndCreateSdcSchoolWithHistory() {
    List<String> listOfSchoolIDs = new ArrayList<>();
    UUID schoolID = UUID.randomUUID();
    listOfSchoolIDs.add(schoolID.toString());

    CollectionTypeCodeEntity collectionCode = this.collectionCodeRepository.findById("TEST").get();

    this.service.startSDCCollection(collectionCode, listOfSchoolIDs);

    List<CollectionEntity> collectionEntities = this.collectionRepository.findAll();
    List<SdcSchoolCollectionEntity> sdcSchoolEntities = this.sdcSchoolRepository.findAll();
    CollectionTypeCodeEntity collectionCodeEntity = this.collectionCodeRepository.findById("TEST").get();
    List<SdcSchoolCollectionHistoryEntity> sdcSchoolHistoryEntities = this.sdcSchoolHistoryRepository.findAll();

    assertEquals(1 ,collectionEntities.size());
    assertEquals(1, sdcSchoolEntities.size());
    assertEquals(schoolID, sdcSchoolEntities.get(0).getSchoolID());
    assertEquals(LocalDateTime.now().plusYears(1).getYear(), collectionCodeEntity.getOpenDate().getYear());
    assertEquals(LocalDateTime.now().getMonth(), collectionCodeEntity.getOpenDate().getMonth());
    assertEquals(LocalDateTime.now().plusYears(1).getYear(), collectionCodeEntity.getCloseDate().getYear());
    assertEquals(LocalDateTime.now().getMonth(), collectionCodeEntity.getCloseDate().getMonth());
    assertEquals(sdcSchoolEntities.get(0).getSdcSchoolCollectionID(), sdcSchoolHistoryEntities.get(0).getSdcSchoolCollectionID());
  }

  private CollectionTypeCodeEntity createCollectionCodeData() {
    return CollectionTypeCodeEntity.builder().collectionTypeCode("TEST").label("Test")
        .description("Test code").displayOrder(0).effectiveDate(
            LocalDateTime.now()).expiryDate(LocalDateTime.MAX).openDate(LocalDateTime.now().minusDays(1))
        .closeDate(LocalDateTime.now().plusDays(7)).createUser("TEST").createDate(LocalDateTime.now())
        .updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }

}
