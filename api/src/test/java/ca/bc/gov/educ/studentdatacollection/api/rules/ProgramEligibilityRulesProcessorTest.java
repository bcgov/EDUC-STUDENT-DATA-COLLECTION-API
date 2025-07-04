package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
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
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  @Autowired
  RestUtils restUtils;

  @AfterEach
  void purgeData() {
    collectionRepository.deleteAll();
    sdcSchoolCollectionRepository.deleteAll();
    sdcSchoolCollectionStudentRepository.deleteAll();
    sdcDistrictCollectionRepository.deleteAll();
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
    entity.setYearsInEll(6);
    entity = sdcStudentEllRepository.save(entity);
    schoolStudentEntity.setAssignedStudentId(studentID);
    schoolStudentEntity.setYearsInEll(6);

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
  void testSpecialEdEligibility_INDPSchools_WithFundingCode20() {
    UUID assignedStudentID = UUID.randomUUID();

    var mockCollection = createMockCollectionEntity();
    mockCollection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
    mockCollection.setCloseDate(LocalDateTime.now().plusDays(2));
    CollectionEntity collection = collectionRepository.save(mockCollection);

    SchoolTombstone school = createMockSchool();
    school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    UUID schoolId = UUID.fromString(school.getSchoolId());
    doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());

    var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

    val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    entity.setAssignedStudentId(assignedStudentID);
    entity.setEnrolledGradeCode("08");
    entity.setSchoolFundingCode("20");

    PenMatchResult penMatchResult = getPenMatchResult();
    penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
    when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    entity,
                    school
            )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.INDP_FIRST_NATION_SPED)
    )).isTrue();
  }

  @Test
  void testSpecialEdEligibility_INDPSchools_WithFundingCode16() {
    UUID assignedStudentID = UUID.randomUUID();

    var mockCollection = createMockCollectionEntity();
    mockCollection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
    mockCollection.setCloseDate(LocalDateTime.now().plusDays(2));
    CollectionEntity collection = collectionRepository.save(mockCollection);

    SchoolTombstone school = createMockSchool();
    school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    UUID schoolId = UUID.fromString(school.getSchoolId());
    doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());

    var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

    val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    entity.setAssignedStudentId(assignedStudentID);
    entity.setEnrolledGradeCode("08");
    entity.setSchoolFundingCode("16");

    PenMatchResult penMatchResult = getPenMatchResult();
    penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
    when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    entity,
                    school
            )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.INDP_FIRST_NATION_SPED)
    )).isFalse();
  }

  @Test
  void testSpecialEdEligibilityInFebCollection() {
    UUID assignedStudentID = UUID.randomUUID();

    var mockCollection = createMockCollectionEntity();
    mockCollection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
    mockCollection.setCloseDate(LocalDateTime.now().plusDays(2));
    CollectionEntity collection = collectionRepository.save(mockCollection);

    var mockAuth = createMockAuthority();
    SchoolTombstone school = createMockSchool();
    District district = createMockDistrict();
    school.setDistrictId(district.getDistrictId());
    school.setIndependentAuthorityId(mockAuth.getIndependentAuthorityId());
    UUID schoolId = UUID.fromString(school.getSchoolId());
    doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
    when(restUtils.getSchoolIDsByIndependentAuthorityID(anyString())).thenReturn(Optional.of(Collections.singletonList(schoolId)));
    createHistoricalCollectionWithStudent(CollectionTypeCodes.SEPTEMBER.getTypeCode(), LocalDateTime.of(LocalDate.parse((LocalDate.now().getYear() - 1) + "-09-30"), LocalTime.MIDNIGHT), assignedStudentID, null, schoolId, new BigDecimal(0));

    SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcDistrictCollection);

    var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
    sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

    val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    entity.setAssignedStudentId(assignedStudentID);
    entity.setEnrolledGradeCode("08");
    entity.setFte(BigDecimal.ZERO);
    entity.setSchoolFundingCode(null);

    school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());

    PenMatchResult penMatchResult = getPenMatchResult();
    penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
    when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    entity,
                    school
            )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.FEB_ONLINE_WITH_HISTORICAL_FUNDING)
    )).isTrue();
  }

  @Test
  void testSpecialEdEligibilityInMayCollection() {
    UUID assignedStudentID = UUID.randomUUID();

    var mockCollection = createMockCollectionEntity();
    mockCollection.setCollectionTypeCode(CollectionTypeCodes.MAY.getTypeCode());
    mockCollection.setCloseDate(LocalDateTime.now().plusDays(2));
    CollectionEntity collection = collectionRepository.save(mockCollection);

    SchoolTombstone school = createMockSchool();
    District district = createMockDistrict();
    school.setDistrictId(district.getDistrictId());
    UUID schoolId = UUID.fromString(school.getSchoolId());
    doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
    createHistoricalCollectionWithStudent(CollectionTypeCodes.SEPTEMBER.getTypeCode(), LocalDateTime.of(LocalDate.parse((LocalDate.now().getYear() - 1) + "-09-30"), LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId, null);
    createHistoricalCollectionWithStudent(CollectionTypeCodes.FEBRUARY.getTypeCode(), LocalDateTime.of(LocalDate.parse((LocalDate.now().getYear()) + "-03-10"), LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId, null);

    SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcDistrictCollection);

    var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
    sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

    val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    entity.setAssignedStudentId(assignedStudentID);
    entity.setEnrolledGradeCode("08");

    school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
    school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());

    PenMatchResult penMatchResult = getPenMatchResult();
    penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
    when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

    List<ProgramEligibilityIssueCode> listWithoutEnrollmentError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    entity,
                    school
            )
    );

    assertThat(listWithoutEnrollmentError.stream().anyMatch(e ->
            e.equals(ProgramEligibilityIssueCode.FEB_ONLINE_WITH_HISTORICAL_FUNDING)
    )).isFalse();
  }

  private void createHistoricalCollectionWithStudent(String collectionTypeCode, LocalDateTime collectionCloseDate, UUID assignedStudentID, UUID districtID, UUID schoolID, BigDecimal fteValue) {
    var collection = createMockCollectionEntity();
    collection.setCollectionTypeCode(collectionTypeCode);
    collection.setCloseDate(collectionCloseDate);
    collection.setSnapshotDate(LocalDate.from(collectionCloseDate.minusDays(10)));
    collection.setCollectionStatusCode("COMPLETED");
    collectionRepository.save(collection);
    SdcDistrictCollectionEntity sdcDistrictCollection = null;
    if(districtID != null) {
      sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, districtID);
      sdcDistrictCollectionRepository.save(sdcDistrictCollection);
    }

    var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolID);
    sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection != null ? sdcDistrictCollection.getSdcDistrictCollectionID() : null);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);
    val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    entity.setAssignedStudentId(assignedStudentID);
    entity.setEnrolledGradeCode("08");
    entity.setFte(fteValue != null ? fteValue : BigDecimal.valueOf(1.00));
    sdcSchoolCollectionStudentRepository.save(entity);
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

  @Test
  void testZeroCoursesAdultRule() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity student = createMockSchoolStudentEntity(schoolCollection);
    student.setIsAdult(true);
    student.setDob("19830504");
    student.setNumberOfCourses("0");
    student.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);

    List<ProgramEligibilityIssueCode> errors = rulesProcessor.processRules(createMockStudentRuleData(student, createMockSchool()));

    assertThat(errors).isNotEmpty();
    assertThat(errors).contains(ProgramEligibilityIssueCode.ZERO_COURSES_ADULT);

    student.setNumberOfCourses("1");
    errors = rulesProcessor.processRules(createMockStudentRuleData(student, createMockSchool()));
    assertThat(errors).doesNotContain(ProgramEligibilityIssueCode.ZERO_COURSES_ADULT);

    student.setNumberOfCourses("0");
    student.setIsAdult(false);
    student.setDob("20150504");
    errors = rulesProcessor.processRules(createMockStudentRuleData(student, createMockSchool()));
    assertThat(errors).doesNotContain(ProgramEligibilityIssueCode.ZERO_COURSES_ADULT);

    student.setIsAdult(true);
    student.setDob("19830504");
    student.setEnrolledGradeCode(SchoolGradeCodes.KINDFULL.getCode());
    errors = rulesProcessor.processRules(createMockStudentRuleData(student, createMockSchool()));
    assertThat(errors).doesNotContain(ProgramEligibilityIssueCode.ZERO_COURSES_ADULT);

    var school = createMockSchool();
    school.setFacilityTypeCode("DIST_LEARN");
    student.setIsAdult(true);
    student.setDob("19830504");
    student.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    errors = rulesProcessor.processRules(createMockStudentRuleData(student, school));
    assertThat(errors).doesNotContain(ProgramEligibilityIssueCode.ZERO_COURSES_ADULT);
  }
  @Test
  void testZeroCoursesSchoolAgeRule() {
    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity student = createMockSchoolStudentEntity(schoolCollection);
    student.setIsSchoolAged(true);
    student.setDob("20150504");
    student.setNumberOfCourses("0");
    student.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(),any(), any())).thenReturn(penMatchResult);

    List<ProgramEligibilityIssueCode> errors = rulesProcessor.processRules(createMockStudentRuleData(student, createMockSchool()));

    assertThat(errors).isNotEmpty();
    assertThat(errors).contains(ProgramEligibilityIssueCode.ZERO_COURSES_SCHOOL_AGE);

    student.setNumberOfCourses("1");
    errors = rulesProcessor.processRules(createMockStudentRuleData(student, createMockSchool()));
    assertThat(errors).doesNotContain(ProgramEligibilityIssueCode.ZERO_COURSES_SCHOOL_AGE);

    student.setNumberOfCourses("0");
    student.setIsSchoolAged(false);
    student.setDob("19830504");
    errors = rulesProcessor.processRules(createMockStudentRuleData(student, createMockSchool()));
    assertThat(errors).doesNotContain(ProgramEligibilityIssueCode.ZERO_COURSES_SCHOOL_AGE);

    student.setIsSchoolAged(true);
    student.setDob("20150504");
    student.setEnrolledGradeCode(SchoolGradeCodes.KINDFULL.getCode());
    errors = rulesProcessor.processRules(createMockStudentRuleData(student, createMockSchool()));
    assertThat(errors).doesNotContain(ProgramEligibilityIssueCode.ZERO_COURSES_SCHOOL_AGE);

    student.setEnrolledGradeCode(SchoolGradeCodes.GRADUATED_ADULT.getCode());
    errors = rulesProcessor.processRules(createMockStudentRuleData(student, createMockSchool()));
    assertThat(errors).doesNotContain(ProgramEligibilityIssueCode.ZERO_COURSES_SCHOOL_AGE);

    var school = createMockSchool();
    school.setFacilityTypeCode("DIST_LEARN");
    student.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    errors = rulesProcessor.processRules(createMockStudentRuleData(student, school));
    assertThat(errors).doesNotContain(ProgramEligibilityIssueCode.ZERO_COURSES_SCHOOL_AGE);
  }

  @Test
  void testCrossEnrollmentRule() {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
    collectionRepository.save(collection);
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(), any(), any())).thenReturn(penMatchResult);

    SchoolTombstone standardSchool = createMockSchool();
    standardSchool.setFacilityTypeCode(FacilityTypeCodes.SUMMER.getCode());

    List<ProgramEligibilityIssueCode> listWithoutError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    standardSchool
            )
    );
    assertThat(listWithoutError)
            .isNotEmpty()
            .doesNotContain(ProgramEligibilityIssueCode.X_ENROLL);

    SchoolTombstone summerSchool = createMockSchool();
    summerSchool.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());

    List<ProgramEligibilityIssueCode> listWithError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    summerSchool
            )
    );
    assertThat(listWithError).contains(ProgramEligibilityIssueCode.X_ENROLL);
  }

  @Test
  void testSummerFacilityProgramRule() {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
    collectionRepository.save(collection);
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(), any(), any())).thenReturn(penMatchResult);

    SchoolTombstone standardSchool = createMockSchool();
    standardSchool.setFacilityTypeCode(FacilityTypeCodes.SUMMER.getCode());

    List<ProgramEligibilityIssueCode> listWithError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    standardSchool
            )
    );
    assertThat(listWithError).contains(ProgramEligibilityIssueCode.SUMMER_SCHOOL_CAREER, ProgramEligibilityIssueCode.SUMMER_SCHOOL_FRENCH);


    SchoolTombstone summerSchool = createMockSchool();
    summerSchool.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());

    List<ProgramEligibilityIssueCode> listWithoutError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    summerSchool
            )
    );
    assertThat(listWithoutError)
            .isNotEmpty()
            .doesNotContain(ProgramEligibilityIssueCode.SUMMER_SCHOOL_CAREER, ProgramEligibilityIssueCode.SUMMER_SCHOOL_FRENCH);
  }

  @Test
  void testPRPorYouthRule() {
    CollectionEntity collection = createMockCollectionEntity();
    collectionRepository.save(collection);
    SdcSchoolCollectionEntity schoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
    SdcSchoolCollectionStudentEntity schoolStudentEntity = this.createMockSchoolStudentEntity(schoolCollection);
    PenMatchResult penMatchResult = getPenMatchResult();
    when(this.restUtils.getPenMatchResult(any(), any(), any())).thenReturn(penMatchResult);

    SchoolTombstone shortPRPSchool = createMockSchool();
    shortPRPSchool.setFacilityTypeCode(FacilityTypeCodes.SHORT_PRP.getCode());

    List<ProgramEligibilityIssueCode> listWithErrorShortPRP = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    shortPRPSchool
            )
    );
    assertThat(listWithErrorShortPRP).contains(ProgramEligibilityIssueCode.PRP_YOUTH);

    SchoolTombstone longPRPSchool = createMockSchool();
    longPRPSchool.setFacilityTypeCode(FacilityTypeCodes.LONG_PRP.getCode());

    List<ProgramEligibilityIssueCode> listWithErrorLongPRP = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    longPRPSchool
            )
    );
    assertThat(listWithErrorLongPRP).contains(ProgramEligibilityIssueCode.PRP_YOUTH);

    SchoolTombstone youthSchool = createMockSchool();
    youthSchool.setFacilityTypeCode(FacilityTypeCodes.SHORT_PRP.getCode());

    List<ProgramEligibilityIssueCode> listWithErrorYouth = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    youthSchool
            )
    );
    assertThat(listWithErrorYouth).contains(ProgramEligibilityIssueCode.PRP_YOUTH);

    SchoolTombstone standardSchool = createMockSchool();
    standardSchool.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());

    List<ProgramEligibilityIssueCode> listWithoutError = rulesProcessor.processRules(
            createMockStudentRuleData(
                    schoolStudentEntity,
                    standardSchool
            )
    );
    assertThat(listWithoutError)
            .isNotEmpty()
            .doesNotContain(ProgramEligibilityIssueCode.PRP_YOUTH);
  }
}
