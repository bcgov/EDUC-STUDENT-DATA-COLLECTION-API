package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolBatchEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolBatchRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolHistoryRepository;
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
  SdcRepository collectionRepository;
  @Autowired
  CollectionCodeRepository collectionCodeRepository;
  @Autowired
  SdcSchoolBatchRepository sdcSchoolRepository;

  @Autowired
  SdcSchoolHistoryRepository sdcSchoolHistoryRepository;

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

    CollectionCodeEntity collectionCode = this.collectionCodeRepository.findById("TEST").get();

    this.service.startSDCCollection(collectionCode, listOfSchoolIDs);

    List<SdcEntity> collectionEntities = this.collectionRepository.findAll();
    List<SdcSchoolBatchEntity> sdcSchoolEntities = this.sdcSchoolRepository.findAll();
    CollectionCodeEntity collectionCodeEntity = this.collectionCodeRepository.findById("TEST").get();
    List<SdcSchoolHistoryEntity> sdcSchoolHistoryEntities = this.sdcSchoolHistoryRepository.findAll();

    assertEquals(1 ,collectionEntities.size());
    assertEquals(1, sdcSchoolEntities.size());
    assertEquals(schoolID, sdcSchoolEntities.get(0).getSchoolID());
    assertEquals(LocalDateTime.now().plusYears(1).getYear(), collectionCodeEntity.getOpenDate().getYear());
    assertEquals(LocalDateTime.now().getMonth(), collectionCodeEntity.getOpenDate().getMonth());
    assertEquals(LocalDateTime.now().plusYears(1).getYear(), collectionCodeEntity.getCloseDate().getYear());
    assertEquals(LocalDateTime.now().getMonth(), collectionCodeEntity.getCloseDate().getMonth());
    assertEquals(sdcSchoolEntities.get(0).getSdcSchoolBatchID(), sdcSchoolHistoryEntities.get(0).getSdcSchoolBatchID());
  }

  private CollectionCodeEntity createCollectionCodeData() {
    return CollectionCodeEntity.builder().collectionCode("TEST").label("Test")
        .description("Test code").displayOrder(0).effectiveDate(
            LocalDateTime.now()).expiryDate(LocalDateTime.MAX).openDate(LocalDateTime.now().minusDays(1))
        .closeDate(LocalDateTime.now().plusDays(7)).createUser("TEST").createDate(LocalDateTime.now())
        .updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }

}
