package ca.bc.gov.educ.studentdatacollection.api.scheduler;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.controller.v1.CollectionController;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeCriteriaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionCodeCriteriaRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolHistoryRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.schedulers.StudentDataCollectionScheduler;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

class StudentDataCollectionSchedulerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private RestUtils restUtils;
  @Autowired
  CollectionController controller;
  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  CollectionCodeRepository collectionCodeRepository;

  @Autowired
  CollectionCodeCriteriaRepository collectionCodeCriteriaRepository;

  @Autowired
  SdcSchoolRepository sdcSchoolRepository;

  @Autowired
  SdcSchoolHistoryRepository sdcSchoolHistoryRepository;

  @Autowired
  StudentDataCollectionScheduler studentDataCollectionScheduler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @BeforeEach
  public void before() {
    CollectionCodeEntity collectionCodeEntity = this.collectionCodeRepository.save(
        this.createCollectionCodeData());
    this.collectionCodeCriteriaRepository.save(
        this.createCollectionCodeCriteriaData(collectionCodeEntity));
  }

  @AfterEach
  public void after() {
    this.sdcSchoolHistoryRepository.deleteAll();
    this.sdcSchoolRepository.deleteAll();
    this.collectionRepository.deleteAll();
    this.collectionCodeCriteriaRepository.deleteAll();
    this.collectionCodeRepository.deleteAll();
  }

  @Test
  void testStudentDataCollectionScheduler_WithCollectionToBeOpened_ShouldSaveCollectionWithTwoSchools() {
    List<School> schoolList = new ArrayList<>();
    schoolList.add(School.builder().schoolId(UUID.randomUUID().toString()).build());
    schoolList.add(School.builder().schoolId(UUID.randomUUID().toString()).build());

    when(this.restUtils.getSchoolListGivenCriteria(any(), any())).thenReturn(schoolList);

    studentDataCollectionScheduler.startSDCCollectionsWithOpenDateInThePast();

    assertEquals(2 ,this.sdcSchoolRepository.findAll().size());
  }

  private CollectionCodeEntity createCollectionCodeData() {
    return CollectionCodeEntity.builder().collectionCode("TEST").label("Test")
        .description("Test code").displayOrder(0).effectiveDate(
            LocalDateTime.now()).expiryDate(LocalDateTime.now().minusDays(1))
        .openDate(LocalDateTime.now())
        .closeDate(LocalDateTime.MAX).createUser("TEST").createDate(LocalDateTime.now())
        .updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }
  private CollectionCodeCriteriaEntity createCollectionCodeCriteriaData(
      CollectionCodeEntity collectionCodeEntity) {
    return CollectionCodeCriteriaEntity.builder().collectionCodeEntity(collectionCodeEntity)
        .schoolCategoryCode("TEST_CC").facilityTypeCode("TEST_FTC").reportingRequirementCode("TEST_RRC")
        .createUser("TEST").createDate(LocalDateTime.now())
        .updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }


}
