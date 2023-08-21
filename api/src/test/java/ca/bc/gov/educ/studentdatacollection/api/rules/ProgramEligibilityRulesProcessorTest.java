package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProgramEligibilityRulesProcessorTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private ProgramEligibilityRulesProcessor rulesProcessor;

  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  @Autowired
  SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  @AfterEach
  void purgeData() {
    collectionRepository.deleteAll();
    sdcSchoolCollectionRepository.deleteAll();
    sdcSchoolCollectionRepository.deleteAll();
  }

  @Test
  void testNoHomeschoolStudentsRule() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    schoolStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.GRADE07.getCode());
    schoolStudentEntity.setIsAdult(true);
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithoutError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );
    assertThat(listWithoutError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.HOMESCHOOL)
    )).isFalse();

    schoolStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.HOMESCHOOL.getCode());
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithHSError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );
    assertThat(listWithHSError.size()).isNotZero();
    assertThat(listWithHSError.stream().anyMatch(e ->
      StringUtils.equals(e.toString(), SdcSchoolCollectionStudentProgramEligibilityIssueCode.HOMESCHOOL.toString())
    )).isTrue();
  }

  @Test
  void testNoOutOfProvinceStudentsRule() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    schoolStudentEntity.setSchoolFundingCode(SchoolFundingCodes.STATUS_FIRST_NATION.getCode());
    schoolStudentEntity.setIsAdult(true);
    assertThat(rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    ).stream().anyMatch(e ->
        e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.OUT_OF_PROVINCE)
      )).isFalse();

    schoolStudentEntity.setSchoolFundingCode(SchoolFundingCodes.OUT_OF_PROVINCE.getCode());
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithOutOfProvinceError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );
    assertThat(listWithOutOfProvinceError.size()).isNotZero();
    assertThat(listWithOutOfProvinceError.stream().anyMatch(e ->
      StringUtils.equals(
        e.toString(),
        SdcSchoolCollectionStudentProgramEligibilityIssueCode.OUT_OF_PROVINCE.toString()
      )
    )).isTrue();
  }

  @Test
  void testNoOffshoreStudentsRule() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setIsAdult(false);

    School localSchool = createMockSchool();
    localSchool.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithoutError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );
    assertThat(listWithoutError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.OFFSHORE)
    )).isFalse();

    School offshoreSchool = createMockSchool();
    offshoreSchool.setSchoolCategoryCode(SchoolCategoryCodes.OFFSHORE.getCode());
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithOffshoreError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        offshoreSchool
      )
    );
    assertThat(listWithOffshoreError.size()).isNotZero();
    assertThat(listWithOffshoreError.stream().anyMatch(e ->
      StringUtils.equals(e.toString(), SdcSchoolCollectionStudentProgramEligibilityIssueCode.OFFSHORE.toString())
    )).isTrue();
  }

  @Test
  void testNoInactiveOnlineAdultStudentsRule() {
    UUID assignedStudentID = UUID.randomUUID();
    LocalDateTime studentCreateDate = LocalDateTime.now();

    School school = this.createMockSchool();
    school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());

    CollectionEntity oneYearOldCollection = collectionRepository.save(createMockCollectionEntity());
    CollectionEntity twoYearOldCollection = collectionRepository.save(createMockCollectionEntity());

    SdcSchoolCollectionEntity oneYearOldSchoolCollection = createMockSdcSchoolCollectionEntity(
      oneYearOldCollection,
      UUID.fromString(school.getSchoolId()),
      UUID.fromString(school.getDistrictId())
    );
    oneYearOldSchoolCollection.setCreateDate(studentCreateDate.minusYears(1));
    this.sdcSchoolCollectionRepository.save(oneYearOldSchoolCollection);
    SdcSchoolCollectionStudentEntity oneYearOldStudentEntity = createMockSchoolStudentEntity(oneYearOldSchoolCollection);
    oneYearOldStudentEntity.setAssignedStudentId(assignedStudentID);
    this.sdcSchoolCollectionStudentRepository.save(oneYearOldStudentEntity);

    SdcSchoolCollectionEntity twoYearOldSchoolCollection = createMockSdcSchoolCollectionEntity(
      twoYearOldCollection,
      UUID.fromString(school.getSchoolId()),
      UUID.fromString(school.getDistrictId())
    );
    twoYearOldSchoolCollection.setCreateDate(studentCreateDate.minusYears(2));
    this.sdcSchoolCollectionRepository.save(twoYearOldSchoolCollection);
    SdcSchoolCollectionStudentEntity twoYearOldStudentEntity = createMockSchoolStudentEntity(twoYearOldSchoolCollection);
    twoYearOldStudentEntity.setAssignedStudentId(assignedStudentID);
    this.sdcSchoolCollectionStudentRepository.save(twoYearOldStudentEntity);

    CollectionEntity currentCollection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity currentSchoolCollection = sdcSchoolCollectionRepository.save(
      createMockSdcSchoolCollectionEntity(
        currentCollection,
        UUID.fromString(school.getSchoolId()),
        UUID.fromString(school.getDistrictId())
      )
    );

    SdcSchoolCollectionStudentEntity schoolStudentEntity = createMockSchoolStudentEntity(currentSchoolCollection);
    schoolStudentEntity.setIsAdult(true);
    schoolStudentEntity.setIsSchoolAged(false);
    schoolStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    schoolStudentEntity.setNumberOfCourses("0");
    schoolStudentEntity.setAssignedStudentId(assignedStudentID);

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = rulesProcessor.processRules(
      createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity), school)
    );

    assertThat(errors.stream().anyMatch(e ->
      StringUtils.equals(e.toString(), SdcSchoolCollectionStudentProgramEligibilityIssueCode.INACTIVE_ADULT.toString())
    )).isTrue();
  }

  @Test
  void testNoInactiveOnlineMinorStudentsRule() {
    UUID assignedStudentID = UUID.randomUUID();
    LocalDateTime studentCreateDate = LocalDateTime.now();

    School school = this.createMockSchool();
    school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());

    CollectionEntity oneYearOldCollection = collectionRepository.save(createMockCollectionEntity());
    CollectionEntity twoYearOldCollection = collectionRepository.save(createMockCollectionEntity());

    SdcSchoolCollectionEntity oneYearOldSchoolCollection = createMockSdcSchoolCollectionEntity(
      oneYearOldCollection,
      UUID.fromString(school.getSchoolId()),
      UUID.fromString(school.getDistrictId())
    );
    oneYearOldSchoolCollection.setCreateDate(studentCreateDate.minusYears(1));
    this.sdcSchoolCollectionRepository.save(oneYearOldSchoolCollection);
    SdcSchoolCollectionStudentEntity oneYearOldStudentEntity = createMockSchoolStudentEntity(oneYearOldSchoolCollection);
    oneYearOldStudentEntity.setAssignedStudentId(assignedStudentID);
    this.sdcSchoolCollectionStudentRepository.save(oneYearOldStudentEntity);

    SdcSchoolCollectionEntity twoYearOldSchoolCollection = createMockSdcSchoolCollectionEntity(
      twoYearOldCollection,
      UUID.fromString(school.getSchoolId()),
      UUID.fromString(school.getDistrictId())
    );
    twoYearOldSchoolCollection.setCreateDate(studentCreateDate.minusYears(2));
    this.sdcSchoolCollectionRepository.save(twoYearOldSchoolCollection);
    SdcSchoolCollectionStudentEntity twoYearOldStudentEntity = createMockSchoolStudentEntity(twoYearOldSchoolCollection);
    twoYearOldStudentEntity.setAssignedStudentId(assignedStudentID);
    this.sdcSchoolCollectionStudentRepository.save(twoYearOldStudentEntity);

    CollectionEntity currentCollection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity currentSchoolCollection = sdcSchoolCollectionRepository.save(
      createMockSdcSchoolCollectionEntity(
        currentCollection,
        UUID.fromString(school.getSchoolId()),
        UUID.fromString(school.getDistrictId())
      )
    );

    SdcSchoolCollectionStudentEntity schoolStudentEntity = createMockSchoolStudentEntity(currentSchoolCollection);
    schoolStudentEntity.setIsSchoolAged(true);
    schoolStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    schoolStudentEntity.setNumberOfCourses("0");
    schoolStudentEntity.setAssignedStudentId(assignedStudentID);

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = rulesProcessor.processRules(
      createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity), school)
    );

    assertThat(errors.stream().anyMatch(e ->
      StringUtils.equals(e.toString(), SdcSchoolCollectionStudentProgramEligibilityIssueCode.INACTIVE_SCHOOL_AGE.toString())
    )).isTrue();
  }

  @Test
  void testFrenchStudentsMustBeEnrolled() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH)
    )).isFalse();

    schoolStudentEntity.setEnrolledProgramCodes("4000000000000017");

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithEnrollmentError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listWithEnrollmentError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH)
    )).isTrue();
  }

  @Test
  void testCareerProgramStudentsMustBeEnrolled() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_CAREER)
    )).isFalse();

    schoolStudentEntity.setEnrolledProgramCodes("3900000000000017");

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithEnrollmentError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listWithEnrollmentError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_CAREER)
    )).isTrue();
  }

  @Test
  void testIndigenousStudentsMustBeEnrolled() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    schoolStudentEntity.setEnrolledProgramCodes("3900000000002917");
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS)
    )).isFalse();

    schoolStudentEntity.setEnrolledProgramCodes("3900000000000017");

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithEnrollmentError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listWithEnrollmentError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS)
    )).isTrue();
  }

  @Test
  void testSpecialEdStudentsMustRequireSpecialEd() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.DOES_NOT_NEED_SPECIAL_ED)
    )).isFalse();

    schoolStudentEntity.setSpecialEducationCategoryCode(null);
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithEnrollmentError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listWithEnrollmentError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.DOES_NOT_NEED_SPECIAL_ED)
    )).isTrue();
  }

  @Test
  void testSpecialEdStudentsMustBeSchoolAgeOrNotGraduated() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listFromSchoolAgedStudent = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listFromSchoolAgedStudent.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.IS_GRADUATED)
    )).isFalse();

    schoolStudentEntity.setIsAdult(true);
    schoolStudentEntity.setIsSchoolAged(false);
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listFromUngraduatedAdult = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listFromUngraduatedAdult.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.IS_GRADUATED)
    )).isFalse();

    schoolStudentEntity.setIsGraduated(true);
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithGraduatedAdultError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listWithGraduatedAdultError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.IS_GRADUATED)
    )).isTrue();

    schoolStudentEntity.setIsAdult(false);
    schoolStudentEntity.setIsSchoolAged(true);
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithGraduatedMinorError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );

    assertThat(listWithGraduatedMinorError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.IS_GRADUATED)
    )).isTrue();
  }

  @Test
  void testIndigenousStudentsMustBeSchoolAged() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("3900000000002917");
    schoolStudentEntity.setNativeAncestryInd("Y");

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithoutAgeError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );
    assertThat(listWithoutAgeError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.INDIGENOUS_ADULT)
    )).isFalse();

    schoolStudentEntity.setIsAdult(true);
    schoolStudentEntity.setIsSchoolAged(false);
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithAgeError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );
    assertThat(listWithAgeError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.INDIGENOUS_ADULT)
    )).isTrue();
  }

  @Test
  void testIndigenousStudentsMustHaveIndigenousAncestry() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("3900000000002917");
    schoolStudentEntity.setNativeAncestryInd("Y");

    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithoutAncestryError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );
    assertThat(listWithoutAncestryError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY)
    )).isFalse();

    schoolStudentEntity.setNativeAncestryInd(null);
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithNullAncestryError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );
    assertThat(listWithNullAncestryError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY)
    )).isTrue();

    schoolStudentEntity.setNativeAncestryInd("N");
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> listWithNoAncestryError = rulesProcessor.processRules(
      createMockStudentSagaData(
        SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(schoolStudentEntity),
        createMockSchool()
      )
    );
    assertThat(listWithNoAncestryError.stream().anyMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY)
    )).isTrue();
  }

}
