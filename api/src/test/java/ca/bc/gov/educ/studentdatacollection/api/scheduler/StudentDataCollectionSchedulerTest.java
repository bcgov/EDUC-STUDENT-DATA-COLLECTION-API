package ca.bc.gov.educ.studentdatacollection.api.scheduler;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.controller.v1.CollectionController;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeCriteriaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.schedulers.StudentDataCollectionScheduler;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StudentDataCollectionSchedulerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private RestUtils restUtils;
  @Autowired
  CollectionController controller;
  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  CollectionTypeCodeRepository collectionCodeRepository;

  @Autowired
  CollectionCodeCriteriaRepository collectionCodeCriteriaRepository;

  @Autowired
  SdcSchoolCollectionRepository sdcSchoolRepository;

  @Autowired
  SdcSchoolCollectionHistoryRepository sdcSchoolHistoryRepository;

  @Autowired
  StudentDataCollectionScheduler studentDataCollectionScheduler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @BeforeEach
  public void before() {
    CollectionTypeCodeEntity collectionCodeEntity = this.collectionCodeRepository.save(
        this.createCollectionCodeData());
    this.collectionCodeCriteriaRepository.save(
        this.createCollectionCodeCriteriaData(collectionCodeEntity));
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

  private CollectionTypeCodeEntity createCollectionCodeData() {
    return CollectionTypeCodeEntity.builder().collectionTypeCode("TEST").label("Test")
        .description("Test code").displayOrder(0).effectiveDate(
            LocalDateTime.now()).expiryDate(LocalDateTime.now().minusDays(1))
        .openDate(LocalDateTime.now())
        .closeDate(LocalDateTime.MAX).createUser("TEST").createDate(LocalDateTime.now())
        .updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }
  private CollectionCodeCriteriaEntity createCollectionCodeCriteriaData(
      CollectionTypeCodeEntity collectionCodeEntity) {
    return CollectionCodeCriteriaEntity.builder().collectionTypeCodeEntity(collectionCodeEntity)
        .schoolCategoryCode("TEST_CC").facilityTypeCode("TEST_FTC").reportingRequirementCode("TEST_RRC")
        .createUser("TEST").createDate(LocalDateTime.now())
        .updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }


}
