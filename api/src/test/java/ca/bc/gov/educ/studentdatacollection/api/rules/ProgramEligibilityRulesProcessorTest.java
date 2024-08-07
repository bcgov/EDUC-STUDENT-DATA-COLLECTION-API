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
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
  @Autowired
  RestUtils restUtils;

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
    .save(createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID()));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    schoolStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.GRADE07.getCode());
    schoolStudentEntity.setIsAdult(true);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);

    schoolStudentEntity.setSchoolFundingCode(SchoolFundingCodes.STATUS_FIRST_NATION.getCode());
    schoolStudentEntity.setIsAdult(true);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setIsAdult(false);

    SchoolTombstone localSchoolTombstone = createMockSchool();
    localSchoolTombstone.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
    List<ProgramEligibilityIssueCode> listWithoutError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );
    assertThat(listWithoutError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.OFFSHORE)
    )).isFalse();

    SchoolTombstone offshoreSchoolTombstone = createMockSchool();
    offshoreSchoolTombstone.setSchoolCategoryCode(SchoolCategoryCodes.OFFSHORE.getCode());
    List<ProgramEligibilityIssueCode> listWithOffshoreError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
              offshoreSchoolTombstone
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
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
    SchoolTombstone schoolTombstone = this.createMockSchool();
    schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());

    CollectionEntity oneYearOldCollection = collectionRepository.save(createMockCollectionEntity());
    CollectionEntity twoYearOldCollection = collectionRepository.save(createMockCollectionEntity());

    SdcSchoolCollectionEntity oneYearOldSchoolCollection = createMockSdcSchoolCollectionEntity(
      oneYearOldCollection,
      UUID.fromString(schoolTombstone.getSchoolId())
    );
    oneYearOldSchoolCollection.setCreateDate(studentCreateDate.minusYears(1));
    this.sdcSchoolCollectionRepository.save(oneYearOldSchoolCollection);
    SdcSchoolCollectionStudentEntity oneYearOldStudentEntity = createMockSchoolStudentEntity(oneYearOldSchoolCollection);
    oneYearOldStudentEntity.setAssignedStudentId(assignedStudentID);
    oneYearOldStudentEntity.setNumberOfCourses("0");
    this.sdcSchoolCollectionStudentRepository.save(oneYearOldStudentEntity);

    SdcSchoolCollectionEntity twoYearOldSchoolCollection = createMockSdcSchoolCollectionEntity(
      twoYearOldCollection,
      UUID.fromString(schoolTombstone.getSchoolId())
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
        UUID.fromString(schoolTombstone.getSchoolId())
      )
    );

    SdcSchoolCollectionStudentEntity schoolStudentEntity = createMockSchoolStudentEntity(currentSchoolCollection);
    schoolStudentEntity.setIsAdult(true);
    schoolStudentEntity.setIsSchoolAged(false);
    schoolStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    schoolStudentEntity.setNumberOfCourses("0");
    schoolStudentEntity.setAssignedStudentId(assignedStudentID);

    List<ProgramEligibilityIssueCode> errors = rulesProcessor.processRules(
      createMockStudentRuleData(schoolStudentEntity, schoolTombstone)
    );

    assertThat(errors.stream().anyMatch(e ->
      StringUtils.equals(e.toString(), ProgramEligibilityIssueCode.INACTIVE_ADULT.toString())
    )).isTrue();
  }

  @Test
  void testNoInactiveOnlineMinorStudentsRule() {
    UUID assignedStudentID = UUID.randomUUID();
    LocalDateTime studentCreateDate = LocalDateTime.now();
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
    SchoolTombstone schoolTombstone = this.createMockSchool();
    schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());

    CollectionEntity oneYearOldCollection = collectionRepository.save(createMockCollectionEntity());
    CollectionEntity twoYearOldCollection = collectionRepository.save(createMockCollectionEntity());

    SdcSchoolCollectionEntity oneYearOldSchoolCollection = createMockSdcSchoolCollectionEntity(
      oneYearOldCollection,
      UUID.fromString(schoolTombstone.getSchoolId())
    );
    oneYearOldSchoolCollection.setCreateDate(studentCreateDate.minusYears(1));
    this.sdcSchoolCollectionRepository.save(oneYearOldSchoolCollection);
    SdcSchoolCollectionStudentEntity oneYearOldStudentEntity = createMockSchoolStudentEntity(oneYearOldSchoolCollection);
    oneYearOldStudentEntity.setAssignedStudentId(assignedStudentID);
    oneYearOldStudentEntity.setNumberOfCourses("0");
    this.sdcSchoolCollectionStudentRepository.save(oneYearOldStudentEntity);

    SdcSchoolCollectionEntity twoYearOldSchoolCollection = createMockSdcSchoolCollectionEntity(
      twoYearOldCollection,
      UUID.fromString(schoolTombstone.getSchoolId())
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
        UUID.fromString(schoolTombstone.getSchoolId())
      )
    );

    SdcSchoolCollectionStudentEntity schoolStudentEntity = createMockSchoolStudentEntity(currentSchoolCollection);
    schoolStudentEntity.setIsSchoolAged(true);
    schoolStudentEntity.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    schoolStudentEntity.setNumberOfCourses("0");
    schoolStudentEntity.setAssignedStudentId(assignedStudentID);

    List<ProgramEligibilityIssueCode> errors = rulesProcessor.processRules(
      createMockStudentRuleData(schoolStudentEntity, schoolTombstone)
    );

    assertThat(errors.stream().anyMatch(e ->
      StringUtils.equals(e.toString(), ProgramEligibilityIssueCode.INACTIVE_SCHOOL_AGE.toString())
    )).isTrue();
  }

  @Test
  void testTooYoungStudentsRule() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setIsAdult(false);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
    SchoolTombstone localSchoolTombstone = createMockSchool();
    localSchoolTombstone.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
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
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("05");
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("4017000000000005");
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setYearsInEll(4);
    schoolStudentEntity.setEnrolledProgramCodes("4017000000000005");
    schoolStudentEntity.setIsSchoolAged(false);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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

    UUID studentID = UUID.randomUUID();
    schoolStudentEntity.setAssignedStudentId(studentID);
    SdcStudentEllEntity entity = createMockStudentEllEntity(schoolStudentEntity);
    entity.setYearsInEll(5);
    entity = sdcStudentEllRepository.save(entity);
    schoolStudentEntity.setAssignedStudentId(studentID);
    schoolStudentEntity.setYearsInEll(5);

    listWithoutEnrollmentError = rulesProcessor.processRules(
      createMockStudentRuleData(
        schoolStudentEntity,
        createMockSchool()
      )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
      e.equals(ProgramEligibilityIssueCode.YEARS_IN_ELL)
    )).isTrue();

    schoolStudentEntity.setYearsInEll(4);
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
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("0540");

    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);

    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(createMockStudentRuleData(schoolStudentEntity, createMockSchool()));

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e -> e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_CAREER))).isFalse();

    schoolStudentEntity.setEnrolledProgramCodes("3900000000000017");

    List<ProgramEligibilityIssueCode> listWithEnrollmentError = rulesProcessor.processRules(createMockStudentRuleData(schoolStudentEntity, createMockSchool()));

    assertThat(listWithEnrollmentError.stream().anyMatch(e -> e.equals(ProgramEligibilityIssueCode.NOT_ENROLLED_CAREER))).isTrue();
  }

  @Test
  void testCareerProgramStudentsMustNotBeIndySchool() {
    SchoolTombstone schoolTombstone = this.createMockSchool();
    schoolTombstone.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
            .save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId())));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("0540");
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(createMockStudentRuleData(schoolStudentEntity, schoolTombstone));
    assertThat(listWithoutEnrollmentError.stream().anyMatch(e -> e.equals(ProgramEligibilityIssueCode.ENROLLED_CAREER_INDY_SCHOOL))).isTrue();

    schoolTombstone.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
    listWithoutEnrollmentError = rulesProcessor.processRules(createMockStudentRuleData(schoolStudentEntity, schoolTombstone));
    assertThat(listWithoutEnrollmentError.stream().anyMatch(e -> e.equals(ProgramEligibilityIssueCode.ENROLLED_CAREER_INDY_SCHOOL))).isFalse();

    schoolTombstone.setSchoolCategoryCode(SchoolCategoryCodes.INDP_FNS.getCode());
    listWithoutEnrollmentError = rulesProcessor.processRules(createMockStudentRuleData(schoolStudentEntity, schoolTombstone));
    assertThat(listWithoutEnrollmentError.stream().anyMatch(e -> e.equals(ProgramEligibilityIssueCode.ENROLLED_CAREER_INDY_SCHOOL))).isTrue();
  }

  @Test
  void testNullEnrolledProgramCode() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
            .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes(null);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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
  void testIndigenousStudentsMustBeNotBeInINDEPENDSchool() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
            .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    SchoolTombstone schoolTombstone = createMockSchool();
    schoolTombstone.setSchoolCategoryCode("INDEPEND");
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
    schoolStudentEntity.setEnrolledProgramCodes("29");
    schoolStudentEntity.setNativeAncestryInd("Y");
    List<ProgramEligibilityIssueCode> error = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    schoolTombstone
            )
    );

    assertThat(error.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.INDIGENOUS_INDY_SCHOOL)
    )).isTrue();
  }

  @Test
  void testIndigenousStudentsMustBeNotBeInINDP_FNSSchool() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
            .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    SchoolTombstone schoolTombstone = createMockSchool();
    schoolTombstone.setSchoolCategoryCode("INDP_FNS");
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
    schoolStudentEntity.setEnrolledProgramCodes("29");
    schoolStudentEntity.setNativeAncestryInd("Y");
    List<ProgramEligibilityIssueCode> error = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    schoolTombstone
            )
    );

    assertThat(error.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.INDIGENOUS_INDY_SCHOOL)
    )).isTrue();
  }

  @Test
  void testEllStudentsMustBeNotBeInINDP_FNSSchool() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
            .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("4017000000000005");
    schoolStudentEntity.setIsSchoolAged(true);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
    UUID studentID = UUID.randomUUID();
    schoolStudentEntity.setAssignedStudentId(studentID);
    SdcStudentEllEntity entity = createMockStudentEllEntity(schoolStudentEntity);
    entity.setYearsInEll(4);
    sdcStudentEllRepository.save(entity);
    schoolStudentEntity.setAssignedStudentId(studentID);

    SchoolTombstone schoolTombstone = createMockSchool();
    schoolTombstone.setSchoolCategoryCode("INDP_FNS");

    List<ProgramEligibilityIssueCode> error = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    schoolTombstone
            )
    );

    assertThat(error.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.ELL_INDY_SCHOOL)
    )).isTrue();
  }

  @Test
  void testEllStudentsMustBeNotBeInINDEPENDSchool() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
            .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("4017000000000005");
    schoolStudentEntity.setIsSchoolAged(true);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
    UUID studentID = UUID.randomUUID();
    schoolStudentEntity.setAssignedStudentId(studentID);
    SdcStudentEllEntity entity = createMockStudentEllEntity(schoolStudentEntity);
    entity.setYearsInEll(4);
    sdcStudentEllRepository.save(entity);
    schoolStudentEntity.setAssignedStudentId(studentID);

    SchoolTombstone schoolTombstone = createMockSchool();
    schoolTombstone.setSchoolCategoryCode("INDEPEND");

    List<ProgramEligibilityIssueCode> error = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    schoolTombstone
            )
    );

    assertThat(error.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.ELL_INDY_SCHOOL)
    )).isTrue();
  }

  @Test
  void testSpecialEdStudentsMustRequireSpecialEd() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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
  void testSpecialEdStudentSchoolAgedAdultGANotEligible() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
            .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setDob("20040101");
    schoolStudentEntity.setSpecialEducationCategoryCode("A");
    schoolStudentEntity.setEnrolledGradeCode("GA");
    schoolStudentEntity.setNumberOfCourses("1");
    schoolStudentEntity.setIsSchoolAged(false);
    schoolStudentEntity.setIsAdult(true);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    createMockSchool()
            )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.NON_ELIG_SPECIAL_EDUCATION)
    )).isTrue();
  }

  @Test
  void testIndigenousStudentsMustBeSchoolAged() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("3900000000002917");
    schoolStudentEntity.setNativeAncestryInd("Y");
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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
    .save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    schoolStudentEntity.setEnrolledProgramCodes("3900000000002917");
    schoolStudentEntity.setNativeAncestryInd("Y");
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);
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
