package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDuplicateRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.grad.v1.GradStatusResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchRecord;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicatesByInstituteID;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SoftDeleteRecordSet;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

class SdcDuplicateServiceTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  RestUtils restUtils;
  @Autowired
  SdcDuplicateRepository sdcDuplicateRepository;
  @Autowired
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  @Autowired
  SdcDuplicatesService sdcDuplicateService;
  @Autowired
  SdcDuplicateResolutionService sdcDuplicateResolutionService;
  @Autowired
  SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  @Autowired
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  @Autowired
  SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;
  private static final SdcDuplicateMapper duplicateMapper = SdcDuplicateMapper.mapper;

  @AfterEach
  public void after() {
    this.sdcDuplicateRepository.deleteAll();
    this.sdcSchoolCollectionStudentRepository.deleteAll();
    this.sdcSchoolCollectionRepository.deleteAll();
    this.collectionRepository.deleteAll();
  }

  @Test
  void testUpdateStudentAndResolveDistrictDuplicates_typePROGRAM_WithNoValidationError_shouldSetDuplicateStatus_RESOLVED() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());
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
    sdcDuplicateResolutionService.updateStudentAndResolveProgramDuplicates(students);

    val resolvedDuplicate = sdcDuplicateRepository.findBySdcDuplicateID(UUID.fromString(programDupe.get().getSdcDuplicateID()));
    assertTrue(resolvedDuplicate.isEmpty());
  }

  @Test
  void testUpdateStudentAndResolveDistrictDuplicates_typePROGRAM_WithOutstandingDuplicateProgramCode_shouldUpdateStudentButNotDuplicate() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SchoolTombstone schoolTombstone1 = createMockSchool();
    schoolTombstone1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    schoolTombstone1.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    SchoolTombstone schoolTombstone2 = createMockSchool();
    schoolTombstone2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    schoolTombstone2.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(schoolTombstone1.getSchoolId())).thenReturn(Optional.of(schoolTombstone1));
    when(this.restUtils.getSchoolBySchoolID(schoolTombstone2.getSchoolId())).thenReturn(Optional.of(schoolTombstone2));
    when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setIsAdult(true);
    student1.setAssignedStudentId(studentID);
    student1.setSpecialEducationCategoryCode(null);
    student1.setEnrolledProgramCodes("0817");
    student1.setIsAdult(false);
    sdcSchoolCollectionStudentRepository.save(student1).getSdcSchoolCollectionStudentID();
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setAssignedStudentId(studentID);
    student2.setSpecialEducationCategoryCode(null);
    student2.setEnrolledProgramCodes("0817");
    student2.setIsAdult(false);
    student2.setEnrolledGradeCode("10");
    sdcSchoolCollectionStudentRepository.save(student2);

    val sdcDuplicates = sdcDuplicateService.getAllInDistrictCollectionDuplicates(sdcDistrictCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();
    PenMatchRecord rec = new PenMatchRecord();
    rec.setStudentID(studentID.toString());
    when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().penStatus("AA").matchingRecords(Arrays.asList(rec)).build());
    assertThat(sdcDuplicates).hasSize(2);

    val programDupe = sdcDuplicates.stream().filter(duplicate -> duplicate.getDuplicateTypeCode().equalsIgnoreCase("PROGRAM")).findFirst();
    val student1Entity = programDupe.get().getSdcSchoolCollectionStudent1Entity();
    val student2Entity = programDupe.get().getSdcSchoolCollectionStudent2Entity();
    student1Entity.setEnrolledProgramCodes("08");

    List<SdcSchoolCollectionStudent> students = new ArrayList<>();
    students.add(student1Entity);
    students.add(student2Entity);
    sdcDuplicateResolutionService.updateStudentAndResolveProgramDuplicates(students);
    val resolvedDupe = sdcDuplicateRepository.findBySdcDuplicateID(UUID.fromString(programDupe.get().getSdcDuplicateID()));
    var resolvedDuplicate = resolvedDupe.get();
    assertThat(resolvedDuplicate.getDuplicateResolutionCode()).isNull();
    var updatedStudent = resolvedDuplicate.getSdcDuplicateStudentEntities().stream().map(SdcDuplicateStudentEntity::getSdcSchoolCollectionStudentEntity).filter(sdcSchoolCollectionStudentEntity -> sdcSchoolCollectionStudentEntity.getSdcSchoolCollectionStudentID().equals(UUID.fromString(student1Entity.getSdcSchoolCollectionStudentID()))).toList().get(0);
    assertThat(updatedStudent.getEnrolledProgramCodes()).isEqualTo("08");
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

    PenMatchRecord rec = new PenMatchRecord();
    rec.setStudentID(studentID.toString());
    when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().penStatus("AA").matchingRecords(Arrays.asList(rec)).build());

    assertThat(sdcDuplicates).hasSize(2);

    val programDupe = sdcDuplicates.stream().filter(duplicate -> duplicate.getDuplicateTypeCode().equalsIgnoreCase("PROGRAM")).findFirst();
    val student1Entity = programDupe.get().getSdcSchoolCollectionStudent1Entity();
    val student2Entity = programDupe.get().getSdcSchoolCollectionStudent2Entity();
    student1Entity.setSpecialEducationCategoryCode(null);
    student1Entity.setEnrolledGradeCode("012");

    List<SdcSchoolCollectionStudent> students = new ArrayList<>();
    students.add(student1Entity);
    students.add(student2Entity);


    sdcDuplicateResolutionService.updateStudentAndResolveProgramDuplicates(students);
    val resolvedDupe = sdcDuplicateRepository.findBySdcDuplicateID(UUID.fromString(programDupe.get().getSdcDuplicateID()));
    var resolvedDuplicate = resolvedDupe.get();
    assertThat(resolvedDuplicate.getDuplicateResolutionCode()).isNull();
  }

  @Test
  void testUpdateStudentAndResolveDistrictDuplicates_typeDELETE_ENROLLMENT_DUPLICATE_shouldSetDuplicateStatus_RELEASED() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());
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
    val enrollmentDupe = sdcDuplicates.stream().filter(duplicate -> duplicate.getDuplicateTypeCode().equalsIgnoreCase("ENROLLMENT")).findFirst();
    val student1Entity = enrollmentDupe.get().getSdcSchoolCollectionStudent1Entity();
    val student2Entity = enrollmentDupe.get().getSdcSchoolCollectionStudent2Entity();

    SoftDeleteRecordSet softDeleteRecordSet = new SoftDeleteRecordSet();
    softDeleteRecordSet.setSoftDeleteStudentIDs(Arrays.asList(UUID.fromString(student1Entity.getSdcSchoolCollectionStudentID())));
    softDeleteRecordSet.setUpdateUser("ABC");

    sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudents(softDeleteRecordSet);
    val resolvedDuplicate = sdcDuplicateRepository.findBySdcDuplicateID(UUID.fromString(enrollmentDupe.get().getSdcDuplicateID()));
    assertThat(resolvedDuplicate.get().getDuplicateResolutionCode()).isEqualTo("RELEASED");
    assertThat(resolvedDuplicate.get().getRetainedSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().toString()).isEqualTo(student2Entity.getSdcSchoolCollectionStudentID());

    val deletedStudent = sdcSchoolCollectionStudentRepository.findById(UUID.fromString(student1Entity.getSdcSchoolCollectionStudentID()));
    assertThat(deletedStudent.get().getSdcSchoolCollectionStudentStatusCode()).isEqualTo("DELETED");

    val duplicate = sdcDuplicateRepository.findBySdcDuplicateID(UUID.fromString(enrollmentDupe.get().getSdcDuplicateID()));
    assertThat(duplicate.get().getRetainedSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().toString()).isEqualTo(student2Entity.getSdcSchoolCollectionStudentID());
  }

  @Test
  @Transactional
  void testSavingStudentWithoutDuplicate_savesSuccessfully() {

    CollectionEntity collectionEntity = createMockCollectionEntity();
    collectionEntity.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());
    CollectionEntity savedCollectionEntity = collectionRepository.save(collectionEntity);

    SchoolTombstone school = createMockSchool();
    SdcSchoolCollectionEntity schoolCollection = createMockSdcSchoolCollectionEntity(savedCollectionEntity, UUID.fromString(school.getSchoolId()));
    SdcSchoolCollectionEntity savedSchoolCollection = sdcSchoolCollectionRepository.save(schoolCollection);

    SdcSchoolCollectionStudentEntity studentEntity = createMockSchoolStudentEntity(savedSchoolCollection);
    sdcSchoolCollectionStudentRepository.save(studentEntity);
    SdcSchoolCollectionStudentEntity newStudentEntity = createMockSchoolStudentEntity(savedSchoolCollection);
    newStudentEntity.setNumberOfCourses("1000");
    newStudentEntity.setSdcSchoolCollectionStudentID(studentEntity.getSdcSchoolCollectionStudentID());

    when(restUtils.getSchoolBySchoolID(any(String.class))).thenReturn(Optional.of(school));
    PenMatchResult penMatchResult = new PenMatchResult();
    PenMatchRecord penMatchRecord = new PenMatchRecord(UUID.randomUUID().toString(), studentEntity.getSdcSchoolCollectionStudentID().toString());
    penMatchResult.setMatchingRecords(List.of(penMatchRecord));
    penMatchResult.setPenStatus("AA");
    penMatchResult.setPenStatusMessage("blah");
    when(restUtils.getPenMatchResult(any(UUID.class), any(SdcSchoolCollectionStudentEntity.class), any(String.class))).thenReturn(penMatchResult);

    GradStatusResult gradStatusResult = new GradStatusResult();
    when(restUtils.getGradStatusResult(any(UUID.class), any(SdcSchoolCollectionStudent.class))).thenReturn(gradStatusResult);

    sdcSchoolCollectionStudentService.validateAndProcessNewSdcSchoolCollectionStudent(newStudentEntity, false);

    Optional<SdcSchoolCollectionStudentEntity> savedStudent = sdcSchoolCollectionStudentRepository.findById(studentEntity.getSdcSchoolCollectionStudentID());

    assertThat(savedStudent.get().getNumberOfCourses()).isEqualTo("1000");
  }

  @Test
  void testSavingStudentWithDuplicateAssignedId_doesNotSave() {
    CollectionEntity collectionEntity = createMockCollectionEntity();
    collectionEntity.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());
    CollectionEntity savedCollectionEntity = collectionRepository.save(collectionEntity);

    SchoolTombstone school = createMockSchool();
    SdcSchoolCollectionEntity schoolCollection = createMockSdcSchoolCollectionEntity(savedCollectionEntity, UUID.fromString(school.getSchoolId()));
    SdcSchoolCollectionEntity savedSchoolCollection = sdcSchoolCollectionRepository.save(schoolCollection);

    SdcSchoolCollectionStudentEntity studentEntity = createMockSchoolStudentEntity(savedSchoolCollection);
    sdcSchoolCollectionStudentRepository.save(studentEntity);

    SdcSchoolCollectionStudentEntity dupeStudentEntity = createMockSchoolStudentEntity(savedSchoolCollection);
    dupeStudentEntity.setAssignedStudentId(studentEntity.getAssignedStudentId());
    sdcSchoolCollectionStudentRepository.save(dupeStudentEntity);

    when(restUtils.getSchoolBySchoolID(any(String.class))).thenReturn(Optional.of(school));
    PenMatchResult penMatchResult = new PenMatchResult();
    PenMatchRecord penMatchRecord = new PenMatchRecord(UUID.randomUUID().toString(), studentEntity.getSdcSchoolCollectionStudentID().toString());
    penMatchResult.setMatchingRecords(List.of(penMatchRecord));
    penMatchResult.setPenStatus("AA");
    penMatchResult.setPenStatusMessage("blah");
    when(restUtils.getPenMatchResult(any(UUID.class), any(SdcSchoolCollectionStudentEntity.class), any(String.class))).thenReturn(penMatchResult);

    GradStatusResult gradStatusResult = new GradStatusResult();
    when(restUtils.getGradStatusResult(any(UUID.class), any(SdcSchoolCollectionStudent.class))).thenReturn(gradStatusResult);

    assertThrows(IllegalTransactionStateException.class, () -> {
      sdcSchoolCollectionStudentService.validateAndProcessNewSdcSchoolCollectionStudent(studentEntity, false);
    }, "SdcSchoolCollectionStudent was not saved to the database because it would create a duplicate.");
  }

  @Test
  void testSavingStudentWithDuplicateAssignedId_resolvesExistingDuplicate() {
    CollectionEntity collectionEntity = createMockCollectionEntity();
    collectionEntity.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());
    CollectionEntity savedCollectionEntity = collectionRepository.save(collectionEntity);

    SchoolTombstone school = createMockSchool();
    SdcSchoolCollectionEntity schoolCollection = createMockSdcSchoolCollectionEntity(savedCollectionEntity, UUID.fromString(school.getSchoolId()));
    SdcSchoolCollectionEntity savedSchoolCollection = sdcSchoolCollectionRepository.save(schoolCollection);

    var stud = createMockSchoolStudentEntity(savedSchoolCollection);
    stud.setAssignedStudentId(UUID.randomUUID());
    SdcSchoolCollectionStudentEntity studentEntity = sdcSchoolCollectionStudentRepository.save(stud);

    SdcSchoolCollectionStudentEntity editedStudentEntity = createMockSchoolStudentEntity(savedSchoolCollection);
    editedStudentEntity.setAssignedStudentId(studentEntity.getAssignedStudentId());
    editedStudentEntity.setSdcSchoolCollectionStudentID(studentEntity.getSdcSchoolCollectionStudentID());
    editedStudentEntity.setSpecialEducationCategoryCode(null);

    SdcSchoolCollectionStudentEntity dupeStudentEntity = createMockSchoolStudentEntity(savedSchoolCollection);
    dupeStudentEntity.setAssignedStudentId(studentEntity.getAssignedStudentId());
    sdcSchoolCollectionStudentRepository.save(dupeStudentEntity);

    SdcDuplicateEntity duplicate = createMockSdcDuplicateEntity(studentEntity, dupeStudentEntity, savedCollectionEntity.getCollectionID());
    duplicate.setDuplicateTypeCode("ENROLLMENT");
    SdcDuplicateEntity savedDupe = sdcDuplicateRepository.save(duplicate);

    when(restUtils.getSchoolBySchoolID(any(String.class))).thenReturn(Optional.of(school));
    PenMatchResult penMatchResult = new PenMatchResult();
    PenMatchRecord penMatchRecord = new PenMatchRecord(UUID.randomUUID().toString(), stud.getAssignedStudentId().toString());
    penMatchResult.setMatchingRecords(List.of(penMatchRecord));
    penMatchResult.setPenStatus("AA");
    penMatchResult.setPenStatusMessage("blah");
    when(restUtils.getPenMatchResult(any(UUID.class), any(SdcSchoolCollectionStudentEntity.class), any(String.class))).thenReturn(penMatchResult);

    GradStatusResult gradStatusResult = new GradStatusResult();
    when(restUtils.getGradStatusResult(any(UUID.class), any(SdcSchoolCollectionStudent.class))).thenReturn(gradStatusResult);

    sdcSchoolCollectionStudentService.updateSdcSchoolCollectionStudent(editedStudentEntity);

    Optional<SdcDuplicateEntity> resolvedDupe = sdcDuplicateRepository.findBySdcDuplicateID(savedDupe.getSdcDuplicateID());

    assertThat(resolvedDupe.get().getDuplicateResolutionCode()).isEqualTo("RELEASED");

  }

  @Test
  void testCreatingNewStudentThatWouldCreateDupe_ShouldThrowError(){
    CollectionEntity collectionEntity = createMockCollectionEntity();
    collectionEntity.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());
    CollectionEntity savedCollectionEntity = collectionRepository.save(collectionEntity);

    SchoolTombstone school = createMockSchool();
    SdcSchoolCollectionEntity schoolCollection = createMockSdcSchoolCollectionEntity(savedCollectionEntity, UUID.fromString(school.getSchoolId()));
    SdcSchoolCollectionEntity savedSchoolCollection = sdcSchoolCollectionRepository.save(schoolCollection);

    UUID assignedStudentID = UUID.randomUUID();
    SdcSchoolCollectionStudentEntity studentEntity = createMockSchoolStudentEntity(savedSchoolCollection);
    studentEntity.setAssignedStudentId(assignedStudentID);

    SdcSchoolCollectionStudentEntity potentialDupeStudent = createMockSchoolStudentEntity(savedSchoolCollection);
    potentialDupeStudent.setAssignedStudentId(assignedStudentID);
    sdcSchoolCollectionStudentRepository.save(potentialDupeStudent);

    when(restUtils.getSchoolBySchoolID(any(String.class))).thenReturn(Optional.of(school));
    PenMatchResult penMatchResult = new PenMatchResult();
    PenMatchRecord penMatchRecord = new PenMatchRecord(UUID.randomUUID().toString(), assignedStudentID.toString());
    penMatchResult.setMatchingRecords(List.of(penMatchRecord));
    penMatchResult.setPenStatus("AA");
    penMatchResult.setPenStatusMessage("blah");
    when(restUtils.getPenMatchResult(any(UUID.class), any(SdcSchoolCollectionStudentEntity.class), any(String.class))).thenReturn(penMatchResult);

    GradStatusResult gradStatusResult = new GradStatusResult();
    when(restUtils.getGradStatusResult(any(UUID.class), any(SdcSchoolCollectionStudent.class))).thenReturn(gradStatusResult);

    assertThrows(InvalidPayloadException.class, () -> {
      sdcSchoolCollectionStudentService.createSdcSchoolCollectionStudent(studentEntity);
    }, "SdcSchoolCollectionStudent was not saved to the database because it would create a duplicate.");

  }

  @Test
  void testIdentifyingStudentToEdit_ShouldCorrectlyIdentifyStudent(){
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone1 = createMockSchool();
    schoolTombstone1.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone1.getSchoolId()));

    SchoolTombstone schoolTombstone2 = createMockSchool();
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone2.getSchoolId()));

    SchoolTombstone schoolTombstone3 = createMockSchool();
    schoolTombstone3.setFacilityTypeCode(FacilityTypeCodes.POST_SEC.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity3 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone3.getSchoolId()));

    SchoolTombstone schoolTombstone4 = createMockSchool();
    schoolTombstone4.setSchoolCategoryCode(SchoolCategoryCodes.INDP_FNS.getCode());
    schoolTombstone4.setMincode("03636019");
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity4 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone4.getSchoolId()));

    SdcSchoolCollectionStudentLightEntity student1 = createMockSchoolStudentLightEntity(sdcSchoolCollectionEntity1);
    student1.setNumberOfCoursesDec(BigDecimal.valueOf(10.00));

    SdcSchoolCollectionStudentLightEntity student2 = createMockSchoolStudentLightEntity(sdcSchoolCollectionEntity2);
    student2.setNumberOfCoursesDec(BigDecimal.valueOf(12.00));

    SdcSchoolCollectionStudentLightEntity student3 = createMockSchoolStudentLightEntity(sdcSchoolCollectionEntity2);
    student3.setNumberOfCoursesDec(BigDecimal.valueOf(10.00));

    SdcSchoolCollectionStudentLightEntity student4 = createMockSchoolStudentLightEntity(sdcSchoolCollectionEntity3);
    student4.setNumberOfCoursesDec(BigDecimal.valueOf(10.00));

    SdcSchoolCollectionStudentLightEntity student5 = createMockSchoolStudentLightEntity(sdcSchoolCollectionEntity4);
    student5.setNumberOfCoursesDec(BigDecimal.valueOf(10.00));

    SdcSchoolCollectionStudentLightEntity studentToEdit1 = sdcDuplicateResolutionService.identifyStudentToEdit(student1, student2, schoolTombstone1, schoolTombstone2);
    assertThat(studentToEdit1.getSdcSchoolCollectionStudentID()).isEqualTo(student1.getSdcSchoolCollectionStudentID());

    SdcSchoolCollectionStudentLightEntity studentToEdit2 = sdcDuplicateResolutionService.identifyStudentToEdit(student1, student3, schoolTombstone1, schoolTombstone2);
    assertThat(studentToEdit2.getSdcSchoolCollectionStudentID()).isEqualTo(student3.getSdcSchoolCollectionStudentID());

    SdcSchoolCollectionStudentLightEntity studentToEdit3 = sdcDuplicateResolutionService.identifyStudentToEdit(student3, student1, schoolTombstone2, schoolTombstone1);
    assertThat(studentToEdit3.getSdcSchoolCollectionStudentID()).isEqualTo(student3.getSdcSchoolCollectionStudentID());

    SdcSchoolCollectionStudentLightEntity studentToEdit4 = sdcDuplicateResolutionService.identifyStudentToEdit(student3, student4, schoolTombstone2, schoolTombstone3);
    assertThat(studentToEdit4.getSdcSchoolCollectionStudentID()).isEqualTo(student4.getSdcSchoolCollectionStudentID());

    SdcSchoolCollectionStudentLightEntity studentToEdit5 = sdcDuplicateResolutionService.identifyStudentToEdit(student4, student3, schoolTombstone3, schoolTombstone2);
    assertThat(studentToEdit5.getSdcSchoolCollectionStudentID()).isEqualTo(student4.getSdcSchoolCollectionStudentID());

    SdcSchoolCollectionStudentLightEntity studentToEdit6 = sdcDuplicateResolutionService.identifyStudentToEdit(student5, student1, schoolTombstone4, schoolTombstone1);
    assertThat(studentToEdit6.getSdcSchoolCollectionStudentID()).isEqualTo(student5.getSdcSchoolCollectionStudentID());

  }

  @Test
  void testGetInFlightProvincialDuplicates_ActiveCollectionNotFound() {
    UUID collectionID = UUID.randomUUID();

    assertThrows(InvalidParameterException.class, () -> {
      sdcDuplicateService.getInFlightProvincialDuplicates(collectionID, false);
    });
  }

  @Test
  void testGetInFlightProvincialDuplicates_InvalidCollectionID() {
    UUID collectionID = UUID.randomUUID();

    CollectionEntity collection = createMockCollectionEntity();
    collectionRepository.save(collection);

    assertThrows(InvalidParameterException.class, () -> {
      sdcDuplicateService.getInFlightProvincialDuplicates(collectionID, false);
    });
  }

  @Test
  void testGetInFlightProvincialDuplicates_DuplicatesAlreadyRun() {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());
    UUID collectionID = collectionRepository.save(collection).getCollectionID();

    assertThrows(InvalidParameterException.class, () -> {
      sdcDuplicateService.getInFlightProvincialDuplicates(collectionID, false);
    });
  }

  @Test
  void testGetInFlightProvincialDuplicates_Districts_Success() {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID2 = sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict3 = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID3 = sdcDistrictCollectionRepository.save(sdcMockDistrict3).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID2);

    SchoolTombstone school3 = createMockSchool();
    school3.setDistrictId(sdcMockDistrict3.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity3 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school3.getSchoolId()));
    sdcSchoolCollectionEntity3.setSdcDistrictCollectionID(sdcDistrictCollectionID3);


    SchoolTombstone school4 = createMockSchool(); //Same district as school1
    school4.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity4 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school4.getSchoolId()));
    sdcSchoolCollectionEntity4.setSdcDistrictCollectionID(sdcDistrictCollectionID);

    SchoolTombstone indySchool = createMockSchool();
    indySchool.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    SdcSchoolCollectionEntity indySchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(indySchool.getSchoolId()));

    SchoolTombstone indySchool2 = createMockSchool();
    indySchool.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    SdcSchoolCollectionEntity indySchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(indySchool2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2, sdcSchoolCollectionEntity3, sdcSchoolCollectionEntity4, indySchoolCollectionEntity, indySchoolCollectionEntity2));

    //Same district, so not provincial dup
    var assignedStudentID1 = UUID.randomUUID();
    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    sdcSchoolCollectionStudent1.setAssignedStudentId(assignedStudentID1);
    var sdcSchoolCollectionStudent4 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity4);
    sdcSchoolCollectionStudent4.setAssignedStudentId(assignedStudentID1);

    //Is provincial dup
    var assignedStudentID2 = UUID.randomUUID();
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudent2.setAssignedStudentId(assignedStudentID2);
    var sdcSchoolCollectionStudent3 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity3);
    sdcSchoolCollectionStudent3.setAssignedStudentId(assignedStudentID2);

    //Both indy, so not a dup
    var assignedStudentID3 = UUID.randomUUID();
    var indySchoolStudent = createMockSchoolStudentEntity(indySchoolCollectionEntity);
    indySchoolStudent.setAssignedStudentId(assignedStudentID3);
    var indySchoolStudent2 = createMockSchoolStudentEntity(indySchoolCollectionEntity2);
    indySchoolStudent2.setAssignedStudentId(assignedStudentID3);

    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2, sdcSchoolCollectionStudent3, sdcSchoolCollectionStudent4, indySchoolStudent, indySchoolStudent2));

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
    when(this.restUtils.getSchoolBySchoolID(school3.getSchoolId())).thenReturn(Optional.of(school3));
    when(this.restUtils.getSchoolBySchoolID(school4.getSchoolId())).thenReturn(Optional.of(school4));
    when(this.restUtils.getSchoolBySchoolID(indySchool.getSchoolId())).thenReturn(Optional.of(indySchool));
    when(this.restUtils.getSchoolBySchoolID(indySchool2.getSchoolId())).thenReturn(Optional.of(indySchool2));

    Map<UUID, SdcDuplicatesByInstituteID> result = sdcDuplicateService.getInFlightProvincialDuplicates(collection.getCollectionID(), false);

    assertThat(result).hasSize(2);
    assertThat(result.get(sdcDistrictCollectionID2)).isNotNull();
    assertThat(result.get(sdcDistrictCollectionID2).getNumEnrollmentDuplicates()).isEqualTo(1);
    assertThat(result.get(sdcDistrictCollectionID3)).isNotNull();
    assertThat(result.get(sdcDistrictCollectionID3).getNumEnrollmentDuplicates()).isEqualTo(1);
  }

  @Test
  void testGetInFlightProvincialDuplicates_IndySchools_Success() {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID2 = sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict3 = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID3 = sdcDistrictCollectionRepository.save(sdcMockDistrict3).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID2);

    SchoolTombstone school3 = createMockSchool();
    school3.setDistrictId(sdcMockDistrict3.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity3 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school3.getSchoolId()));
    sdcSchoolCollectionEntity3.setSdcDistrictCollectionID(sdcDistrictCollectionID3);


    SchoolTombstone school4 = createMockSchool(); //Same district as school1
    school4.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity4 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school4.getSchoolId()));
    sdcSchoolCollectionEntity4.setSdcDistrictCollectionID(sdcDistrictCollectionID);

    SchoolTombstone indySchool = createMockSchool();
    indySchool.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    SdcSchoolCollectionEntity indySchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(indySchool.getSchoolId()));

    SchoolTombstone indySchool2 = createMockSchool();
    indySchool.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    SdcSchoolCollectionEntity indySchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(indySchool2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2, sdcSchoolCollectionEntity3, sdcSchoolCollectionEntity4, indySchoolCollectionEntity, indySchoolCollectionEntity2));

    //Same district, so not provincial dup
    var assignedStudentID1 = UUID.randomUUID();
    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    sdcSchoolCollectionStudent1.setAssignedStudentId(assignedStudentID1);
    var sdcSchoolCollectionStudent4 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity4);
    sdcSchoolCollectionStudent4.setAssignedStudentId(assignedStudentID1);

    //Is provincial dup
    var assignedStudentID2 = UUID.randomUUID();
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudent2.setAssignedStudentId(assignedStudentID2);
    var sdcSchoolCollectionStudent3 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity3);
    sdcSchoolCollectionStudent3.setAssignedStudentId(assignedStudentID2);

    //Both indy, so not a dup
    var assignedStudentID3 = UUID.randomUUID();
    var indySchoolStudent = createMockSchoolStudentEntity(indySchoolCollectionEntity);
    indySchoolStudent.setAssignedStudentId(assignedStudentID3);
    var indySchoolStudent2 = createMockSchoolStudentEntity(indySchoolCollectionEntity2);
    indySchoolStudent2.setAssignedStudentId(assignedStudentID3);

    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2, sdcSchoolCollectionStudent3, sdcSchoolCollectionStudent4, indySchoolStudent, indySchoolStudent2));

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
    when(this.restUtils.getSchoolBySchoolID(school3.getSchoolId())).thenReturn(Optional.of(school3));
    when(this.restUtils.getSchoolBySchoolID(school4.getSchoolId())).thenReturn(Optional.of(school4));
    when(this.restUtils.getSchoolBySchoolID(indySchool.getSchoolId())).thenReturn(Optional.of(indySchool));
    when(this.restUtils.getSchoolBySchoolID(indySchool2.getSchoolId())).thenReturn(Optional.of(indySchool2));

    Map<UUID, SdcDuplicatesByInstituteID> result = sdcDuplicateService.getInFlightProvincialDuplicates(collection.getCollectionID(), true);

    assertThat(result).hasSize(2);
    assertThat(result.get(indySchoolCollectionEntity.getSdcSchoolCollectionID())).isNotNull();
    assertThat(result.get(indySchoolCollectionEntity.getSdcSchoolCollectionID()).getNumEnrollmentDuplicates()).isEqualTo(1);
    assertThat(result.get(indySchoolCollectionEntity2.getSdcSchoolCollectionID())).isNotNull();
    assertThat(result.get(indySchoolCollectionEntity2.getSdcSchoolCollectionID()).getNumEnrollmentDuplicates()).isEqualTo(1);
  }


  @Test
  void testSoftDeleteSdcSchoolCollectionStudents_WhenStudentExists_SavesEntity() {
    var collection = collectionRepository.save(createMockCollectionEntity());
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID()));
    var mockStudentEntity = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));
    SoftDeleteRecordSet softDeleteRecordSet = new SoftDeleteRecordSet();
    softDeleteRecordSet.setSoftDeleteStudentIDs(Arrays.asList(mockStudentEntity.getSdcSchoolCollectionStudentID()));
    softDeleteRecordSet.setUpdateUser("ABC");
    sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudents(softDeleteRecordSet);

    var updatedStudentEntity = sdcSchoolCollectionStudentRepository.findById(mockStudentEntity.getSdcSchoolCollectionStudentID());
    assertThat(updatedStudentEntity.get().getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.DELETED.toString());
  }

}
