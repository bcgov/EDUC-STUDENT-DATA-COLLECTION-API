package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDuplicateRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.grad.v1.GradStatusResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.UnsubmitSdcDistrictCollection;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

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
  private static final SdcDuplicateMapper duplicateMapper = SdcDuplicateMapper.mapper;

  @AfterEach
  public void after() {
    this.sdcDuplicateRepository.deleteAll();
    this.collectionRepository.deleteAll();
    this.sdcSchoolCollectionStudentRepository.deleteAll();
    this.sdcSchoolCollectionRepository.deleteAll();
  }

  @Test
  void testUpdateStudentAndResolveDistrictDuplicates_typePROGRAM_WithNoValidationError_shouldSetDuplicateStatus_RESOLVED() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SchoolTombstone schoolTombstone1 = createMockSchool();
    schoolTombstone1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    SchoolTombstone schoolTombstone2 = createMockSchool();
    schoolTombstone2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(schoolTombstone1.getSchoolId())).thenReturn(Optional.of(schoolTombstone1));
    when(this.restUtils.getSchoolBySchoolID(schoolTombstone2.getSchoolId())).thenReturn(Optional.of(schoolTombstone2));

    when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
    when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setIsAdult(true);
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    val sdcDuplicates = sdcDuplicateService.getAllInDistrictCollectionDuplicates(sdcDistrictCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();

    assertThat(sdcDuplicates).hasSize(2);

    val programDupe = sdcDuplicates.stream().filter(duplicate -> duplicate.getDuplicateTypeCode().equalsIgnoreCase("PROGRAM")).findFirst();
    val student1Entity = programDupe.get().getSdcSchoolCollectionStudent1Entity();
    val student2Entity = programDupe.get().getSdcSchoolCollectionStudent2Entity();
    student1Entity.setSpecialEducationCategoryCode(null);

    List<SdcSchoolCollectionStudent> students = new ArrayList<>();
    students.add(student1Entity);
    students.add(student2Entity);
    val resolvedDuplicate = sdcDistrictCollectionService.updateStudentAndResolveDistrictDuplicates(UUID.fromString(programDupe.get().getSdcDuplicateID()), students);
    assertThat(resolvedDuplicate.getDuplicateResolutionCode()).isEqualTo("RESOLVED");
  }

  @Test
  void testUpdateStudentAndResolveDistrictDuplicates_typePROGRAM_WithValidationError_shouldSetDuplicateStatus_NULL() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SchoolTombstone schoolTombstone1 = createMockSchool();
    schoolTombstone1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    SchoolTombstone schoolTombstone2 = createMockSchool();
    schoolTombstone2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(schoolTombstone1.getSchoolId())).thenReturn(Optional.of(schoolTombstone1));
    when(this.restUtils.getSchoolBySchoolID(schoolTombstone2.getSchoolId())).thenReturn(Optional.of(schoolTombstone2));

    when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
    when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setIsAdult(true);
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    val sdcDuplicates = sdcDuplicateService.getAllInDistrictCollectionDuplicates(sdcDistrictCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();

    assertThat(sdcDuplicates).hasSize(2);

    val programDupe = sdcDuplicates.stream().filter(duplicate -> duplicate.getDuplicateTypeCode().equalsIgnoreCase("PROGRAM")).findFirst();
    val student1Entity = programDupe.get().getSdcSchoolCollectionStudent1Entity();
    val student2Entity = programDupe.get().getSdcSchoolCollectionStudent2Entity();
    student1Entity.setSpecialEducationCategoryCode(null);
    student1Entity.setEnrolledGradeCode("012");

    List<SdcSchoolCollectionStudent> students = new ArrayList<>();
    students.add(student1Entity);
    students.add(student2Entity);
    val resolvedDuplicate = sdcDistrictCollectionService.updateStudentAndResolveDistrictDuplicates(UUID.fromString(programDupe.get().getSdcDuplicateID()), students);
    assertThat(resolvedDuplicate.getDuplicateResolutionCode()).isNull();
  }

  @Test
  void testUpdateStudentAndResolveDistrictDuplicates_typeDELETE_ENROLLMENT_DUPLICATE_shouldSetDuplicateStatus_RELEASED() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SchoolTombstone schoolTombstone1 = createMockSchool();
    schoolTombstone1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    SchoolTombstone schoolTombstone2 = createMockSchool();
    schoolTombstone2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(schoolTombstone1.getSchoolId())).thenReturn(Optional.of(schoolTombstone1));
    when(this.restUtils.getSchoolBySchoolID(schoolTombstone2.getSchoolId())).thenReturn(Optional.of(schoolTombstone2));

    when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
    when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    val sdcDuplicates = sdcDuplicateService.getAllInDistrictCollectionDuplicates(sdcDistrictCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();

    assertThat(sdcDuplicates).hasSize(1);
    val programDupe = sdcDuplicates.stream().filter(duplicate -> duplicate.getDuplicateTypeCode().equalsIgnoreCase("ENROLLMENT")).findFirst();
    val student1Entity = programDupe.get().getSdcSchoolCollectionStudent1Entity();
    val student2Entity = programDupe.get().getSdcSchoolCollectionStudent2Entity();

    val resolvedDuplicate = sdcDistrictCollectionService.softDeleteEnrollmentDuplicate(UUID.fromString(programDupe.get().getSdcDuplicateID()), student1Entity);
    assertThat(resolvedDuplicate.getDuplicateResolutionCode()).isEqualTo("RELEASED");
    assertThat(resolvedDuplicate.getRetainedSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().toString()).isEqualTo(student2Entity.getSdcSchoolCollectionStudentID());

    val deletedStudent = sdcSchoolCollectionStudentRepository.findById(UUID.fromString(student1Entity.getSdcSchoolCollectionStudentID()));
    assertThat(deletedStudent.get().getSdcSchoolCollectionStudentStatusCode()).isEqualTo("DELETED");

    val duplicate = sdcDuplicateRepository.findBySdcDuplicateID(UUID.fromString(programDupe.get().getSdcDuplicateID()));
    assertThat(duplicate.get().getRetainedSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().toString()).isEqualTo(student2Entity.getSdcSchoolCollectionStudentID());
  }

  @Test
  void testUpdateStudentAndResolveDistrictDuplicates_typeCHANGE_GRADE_WithNoValidationError_shouldSetDuplicateStatus_GRADE_CHNG() {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SchoolTombstone schoolTombstone1 = createMockSchool();
    schoolTombstone1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    SchoolTombstone schoolTombstone2 = createMockSchool();
    schoolTombstone2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(schoolTombstone1.getSchoolId())).thenReturn(Optional.of(schoolTombstone1));
    when(this.restUtils.getSchoolBySchoolID(schoolTombstone2.getSchoolId())).thenReturn(Optional.of(schoolTombstone2));

    when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
    when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    val sdcDuplicates = sdcDuplicateService.getAllInDistrictCollectionDuplicates(sdcDistrictCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();

    assertThat(sdcDuplicates).hasSize(1);

    val programDupe = sdcDuplicates.stream().filter(duplicate -> duplicate.getDuplicateTypeCode().equalsIgnoreCase("ENROLLMENT")).findFirst();
    val student1Entity = programDupe.get().getSdcSchoolCollectionStudent1Entity();
    student1Entity.setEnrolledGradeCode("10");

    val resolvedDuplicate = sdcDistrictCollectionService.changeGrade(UUID.fromString(programDupe.get().getSdcDuplicateID()), student1Entity);
    assertThat(resolvedDuplicate.getDuplicateResolutionCode()).isEqualTo("GRADE_CHNG");

    val duplicate = sdcDuplicateRepository.findBySdcDuplicateID(UUID.fromString(programDupe.get().getSdcDuplicateID()));
    assertThat(duplicate.get().getRetainedSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().toString()).isEqualTo(student1Entity.getSdcSchoolCollectionStudentID());

  }

  @Test
  void testUpdateStudentAndResolveDistrictDuplicates_typeCHANGE_GRADE_WithValidationError_shouldSetDuplicateStatus_GRADE_CHNG() {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SchoolTombstone schoolTombstone1 = createMockSchool();
    schoolTombstone1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    SchoolTombstone schoolTombstone2 = createMockSchool();
    schoolTombstone2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(schoolTombstone1.getSchoolId())).thenReturn(Optional.of(schoolTombstone1));
    when(this.restUtils.getSchoolBySchoolID(schoolTombstone2.getSchoolId())).thenReturn(Optional.of(schoolTombstone2));

    when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
    when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    val sdcDuplicates = sdcDuplicateService.getAllInDistrictCollectionDuplicates(sdcDistrictCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();

    assertThat(sdcDuplicates).hasSize(1);

    val programDupe = sdcDuplicates.stream().filter(duplicate -> duplicate.getDuplicateTypeCode().equalsIgnoreCase("ENROLLMENT")).findFirst();
    val student1Entity = programDupe.get().getSdcSchoolCollectionStudent1Entity();
    student1Entity.setEnrolledGradeCode("12");

    val resolvedDuplicate = sdcDistrictCollectionService.changeGrade(UUID.fromString(programDupe.get().getSdcDuplicateID()), student1Entity);
    assertThat(resolvedDuplicate.getDuplicateResolutionCode()).isNull();
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
