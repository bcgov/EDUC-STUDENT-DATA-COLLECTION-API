package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcStudentEllRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
  @Autowired
  SdcStudentEllRepository sdcStudentEllRepository;

  @AfterEach
  void purgeData() {
    collectionRepository.deleteAll();
    sdcSchoolCollectionRepository.deleteAll();
    sdcSchoolCollectionRepository.deleteAll();
    sdcStudentEllRepository.deleteAll();
  }

  @Test
  void testNoHomeschoolStudentsRule() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    schoolStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.GRADE07.getCode());
    schoolStudentEntity.setIsAdult(true);
    List<ProgramEligibilityIssueCode> listWithoutError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );
    assertThat(listWithoutError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.HOMESCHOOL)
    )).isFalse();

    schoolStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.HOMESCHOOL.getCode());
    List<ProgramEligibilityIssueCode> listWithHSError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );
    assertThat(listWithHSError.size()).isNotZero();
    assertThat(listWithHSError.stream().anyMatch(e ->
      StringUtils.equals(e.toString(), ProgramEligibilityIssueCode.HOMESCHOOL.toString())
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
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    ).stream().anyMatch(e ->
        e.equals(ProgramEligibilityIssueCode.OUT_OF_PROVINCE)
      )).isFalse();

    schoolStudentEntity.setSchoolFundingCode(SchoolFundingCodes.OUT_OF_PROVINCE.getCode());
    List<ProgramEligibilityIssueCode> listWithOutOfProvinceError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );
    assertThat(listWithOutOfProvinceError.size()).isNotZero();
    assertThat(listWithOutOfProvinceError.stream().anyMatch(e ->
      StringUtils.equals(
        e.toString(),
        ProgramEligibilityIssueCode.OUT_OF_PROVINCE.toString()
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
    List<ProgramEligibilityIssueCode> listWithoutError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );
    assertThat(listWithoutError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.OFFSHORE)
    )).isFalse();

    School offshoreSchool = createMockSchool();
    offshoreSchool.setSchoolCategoryCode(SchoolCategoryCodes.OFFSHORE.getCode());
    List<ProgramEligibilityIssueCode> listWithOffshoreError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        offshoreSchool
      )
    );
    assertThat(listWithOffshoreError.size()).isNotZero();
    assertThat(listWithOffshoreError.stream().anyMatch(e ->
      StringUtils.equals(e.toString(), ProgramEligibilityIssueCode.OFFSHORE.toString())
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
    oneYearOldStudentEntity.setNumberOfCourses("0");
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
    twoYearOldStudentEntity.setNumberOfCourses("0");
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

    List<ProgramEligibilityIssueCode> errors = rulesProcessor.processRules(
      createMockStudentRuleData(schoolStudentEntity, school)
    );

    assertThat(errors.stream().anyMatch(e ->
      StringUtils.equals(e.toString(), ProgramEligibilityIssueCode.INACTIVE_ADULT.toString())
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
    oneYearOldStudentEntity.setNumberOfCourses("0");
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
    twoYearOldStudentEntity.setNumberOfCourses("0");
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

    List<ProgramEligibilityIssueCode> errors = rulesProcessor.processRules(
      createMockStudentRuleData(schoolStudentEntity, school)
    );

    assertThat(errors.stream().anyMatch(e ->
      StringUtils.equals(e.toString(), ProgramEligibilityIssueCode.INACTIVE_SCHOOL_AGE.toString())
    )).isTrue();
  }

  @Test
  void testTooYoungStudentsRule() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setIsAdult(false);

    School localSchool = createMockSchool();
    localSchool.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
    List<ProgramEligibilityIssueCode> listWithoutError = rulesProcessor.processRules(createMockStudentRuleData(schoolStudentEntity, createMockSchool()));
    assertThat(listWithoutError.stream().anyMatch(e -> e.equals(ProgramEligibilityIssueCode.TOO_YOUNG))).isFalse();

    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
    schoolStudentEntity.setDob(format.format(LocalDateTime.now().minusYears(1)));
    List<ProgramEligibilityIssueCode> listWithTooYoungStudentError = rulesProcessor.processRules(createMockStudentRuleData(schoolStudentEntity, createMockSchool()));
    assertThat(listWithTooYoungStudentError.size()).isNotZero();
    assertThat(listWithTooYoungStudentError.stream().anyMatch(e -> StringUtils.equals(e.toString(), ProgramEligibilityIssueCode.TOO_YOUNG.toString()))).isTrue();
  }

  @Test
  void testFrenchStudentsMustBeEnrolled() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("05");

    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH)
    )).isFalse();

    schoolStudentEntity.setEnrolledProgramCodes("4000000000000017");

    List<ProgramEligibilityIssueCode> listWithEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH)
    )).isTrue();
  }

  @Test
  void testEllStudentsMustBeEnrolled() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("4017000000000005");

    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_ELL)
    )).isFalse();

    schoolStudentEntity.setEnrolledProgramCodes("4000000000000005");

    List<ProgramEligibilityIssueCode> listWithEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_ELL)
    )).isTrue();
  }

@Test
  void testEllStudentsEligibility() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("4017000000000005");
    schoolStudentEntity.setIsSchoolAged(false);

    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.YEARS_IN_ELL)
    )).isTrue();

    schoolStudentEntity.setIsSchoolAged(true);
    listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.YEARS_IN_ELL)
    )).isFalse();

    SdcStudentEllEntity entity = new SdcStudentEllEntity();
    var studentID = UUID.randomUUID();
    entity.setStudentID(studentID);
    entity.setCreateDate(LocalDateTime.now());
    entity.setCreateUser("ABC");
    entity.setUpdateDate(LocalDateTime.now());
    entity.setUpdateUser("ABC");
    entity.setYearsInEll(5);
    entity = sdcStudentEllRepository.save(entity);
    schoolStudentEntity.setAssignedStudentId(studentID);

    listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.YEARS_IN_ELL)
    )).isTrue();

    entity.setYearsInEll(4);
    sdcStudentEllRepository.save(entity);

    listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.YEARS_IN_ELL)
    )).isFalse();
  }

  @Test
  void testCareerProgramStudentsMustBeEnrolled() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("0540");
    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(createMockStudentRuleData(schoolStudentEntity, createMockSchool()));

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e -> e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_CAREER))).isFalse();

    schoolStudentEntity.setEnrolledProgramCodes("3900000000000017");

    List<ProgramEligibilityIssueCode> listWithEnrollmentError = rulesProcessor.processRules(createMockStudentRuleData(schoolStudentEntity, createMockSchool()));

    assertThat(listWithEnrollmentError.stream().anyMatch(e -> e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_CAREER))).isTrue();
  }

  @Test
  void testNullEnrolledProgramCode() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
            .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes(null);

    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    createMockSchool()
            )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_CAREER)
    )).isTrue();

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_ELL)
    )).isTrue();

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH)
    )).isTrue();

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS)
    )).isTrue();

  }

  @Test
  void testIndigenousStudentsMustBeEnrolled() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    schoolStudentEntity.setEnrolledProgramCodes("3900000000002917");
    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS)
    )).isFalse();

    schoolStudentEntity.setEnrolledProgramCodes("3900000000000017");

    List<ProgramEligibilityIssueCode> listWithEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS)
    )).isTrue();
  }

  @Test
  void testSpecialEdStudentsMustRequireSpecialEd() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_SPECIAL_ED)
    )).isFalse();

    schoolStudentEntity.setSpecialEducationCategoryCode(null);
    List<ProgramEligibilityIssueCode> listWithEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_SPECIAL_ED)
    )).isTrue();
  }

  @Test
  void testSpecialEdStudentsMustBeSchoolAgeOrNotGraduated() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    List<ProgramEligibilityIssueCode> listFromSchoolAgedStudent = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listFromSchoolAgedStudent.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NON_ELIG_SPECIAL_EDUCATION)
    )).isFalse();

    schoolStudentEntity.setIsAdult(true);
    schoolStudentEntity.setIsSchoolAged(false);
    List<ProgramEligibilityIssueCode> listFromUngraduatedAdult = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listFromUngraduatedAdult.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NON_ELIG_SPECIAL_EDUCATION)
    )).isFalse();

    schoolStudentEntity.setIsGraduated(true);
    List<ProgramEligibilityIssueCode> listWithGraduatedAdultError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithGraduatedAdultError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NON_ELIG_SPECIAL_EDUCATION)
    )).isTrue();

    schoolStudentEntity.setIsAdult(false);
    schoolStudentEntity.setIsSchoolAged(true);
    List<ProgramEligibilityIssueCode> listWithGraduatedMinorError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithGraduatedMinorError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NON_ELIG_SPECIAL_EDUCATION)
    )).isFalse();
  }

  @Test
  void testIndigenousStudentsMustBeSchoolAged() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("3900000000002917");
    schoolStudentEntity.setNativeAncestryInd("Y");

    List<ProgramEligibilityIssueCode> listWithoutAgeError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );
    assertThat(listWithoutAgeError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.INDIGENOUS_ADULT)
    )).isFalse();

    schoolStudentEntity.setIsAdult(true);
    schoolStudentEntity.setIsSchoolAged(false);
    List<ProgramEligibilityIssueCode> listWithAgeError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );
    assertThat(listWithAgeError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.INDIGENOUS_ADULT)
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

    List<ProgramEligibilityIssueCode> listWithoutAncestryError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );
    assertThat(listWithoutAncestryError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY)
    )).isFalse();

    schoolStudentEntity.setNativeAncestryInd(null);
    List<ProgramEligibilityIssueCode> listWithNullAncestryError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );
    assertThat(listWithNullAncestryError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY)
    )).isTrue();

    schoolStudentEntity.setNativeAncestryInd("N");
    List<ProgramEligibilityIssueCode> listWithNoAncestryError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );
    assertThat(listWithNoAncestryError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY)
    )).isTrue();
  }

}
