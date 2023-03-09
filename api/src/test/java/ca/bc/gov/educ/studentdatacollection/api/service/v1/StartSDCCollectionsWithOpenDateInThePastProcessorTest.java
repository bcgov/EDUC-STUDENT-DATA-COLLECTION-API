package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import static org.junit.Assert.assertEquals;

import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolRepository;
import ca.bc.gov.educ.studentdatacollection.api.support.TestRedisConfiguration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {TestRedisConfiguration.class, StudentDataCollectionApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class StartSDCCollectionsWithOpenDateInThePastProcessorTest {

  @Autowired
  StartSDCCollectionsWithOpenDateInThePastProcessingHandler service;

  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  CollectionCodeRepository collectionCodeRepository;
  @Autowired
  SdcSchoolRepository sdcSchoolRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @BeforeEach
  public void before() {
    this.collectionCodeRepository.save(this.createCollectionCodeData());
  }

  @AfterEach
  public void after() {
    this.sdcSchoolRepository.deleteAll();
    this.collectionRepository.deleteAll();
    this.collectionCodeRepository.deleteAll();
  }

  @Test
  public void testStartSDCCollection_GivenPastOpenDate_ShouldCreateCollectionAndUpdateCollectionCodeOpenDateCloseDate() {
    List<String> listOfSchoolIDs = new ArrayList<>();
    UUID schoolID = UUID.randomUUID();
    listOfSchoolIDs.add(schoolID.toString());

    CollectionCodeEntity collectionCode = this.collectionCodeRepository.findById("TEST").get();

    this.service.startSDCCollection(collectionCode, listOfSchoolIDs);

    List<CollectionEntity> collectionEntities = this.collectionRepository.findAll();
    List<SdcSchoolEntity> sdcSchoolEntities = this.sdcSchoolRepository.findAll();
    CollectionCodeEntity collectionCodeEntity = this.collectionCodeRepository.findById("TEST").get();

    assertEquals(collectionEntities.size(), 1);
    assertEquals(sdcSchoolEntities.size(), 1);
    assertEquals(sdcSchoolEntities.get(0).getSchoolID(), schoolID);
    assertEquals(collectionCodeEntity.getOpenDate().getYear(), LocalDateTime.now().plusYears(1).getYear());
    assertEquals(collectionCodeEntity.getOpenDate().getMonth(), LocalDateTime.now().getMonth());
    assertEquals(collectionCodeEntity.getCloseDate().getYear(), LocalDateTime.now().plusYears(1).getYear());
    assertEquals(collectionCodeEntity.getCloseDate().getMonth(), LocalDateTime.now().getMonth());
  }

  private CollectionCodeEntity createCollectionCodeData() {
    return CollectionCodeEntity.builder().collectionCode("TEST").label("Test")
        .description("Test code").displayOrder(0).effectiveDate(
            LocalDateTime.now()).expiryDate(LocalDateTime.MAX).openDate(LocalDateTime.now().minusDays(1))
        .closeDate(LocalDateTime.now().plusDays(7)).createUser("TEST").createDate(LocalDateTime.now())
        .updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }

}
