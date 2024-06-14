package ca.bc.gov.educ.studentdatacollection.api;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.grad.v1.GradStatusResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchRecord;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.support.StudentDataCollectionTestUtils;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest(classes = {StudentDataCollectionApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class BaseStudentDataCollectionAPITest {

  @Autowired
  protected StudentDataCollectionTestUtils studentDataCollectionTestUtils;

  @Autowired
  EnrolledProgramCodeRepository enrolledProgramCodeRepository;
  @Autowired
  CareerProgramCodeRepository careerProgramCodeRepository;
  @Autowired
  HomeLanguageSpokenCodeRepository homeLanguageSpokenCodeRepository;
  @Autowired
  BandCodeRepository bandCodeRepository;
  @Autowired
  FundingCodeRepository fundingCodeRepository;
  @Autowired
  EnrolledGradeCodeRepository enrolledGradeCodeRepository;
  @Autowired
  SpecialEducationCategoryRepository specialEducationCategoryRepository;
  @Autowired
  GenderCodeRepository genderCodeRepository;
  @Autowired
  SchoolGradeCodeRepository schoolGradeCodeRepository;
  @Autowired
  SchoolFundingGroupCodeRepository schoolFundingGroupCodeRepository;
  @Autowired
  public CollectionRepository collectionRepository;
  @Autowired
  public SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  @Autowired
  public SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  @Autowired
  CollectionTypeCodeRepository collectionTypeCodeRepository;
  @Autowired
  ProgramDuplicateTypeCodeRepository programDuplicateTypeCodeRepository;
  @Autowired
  SdcSchoolCollectionStatusCodeRepository sdcSchoolCollectionStatusCodeRepository;
  @Autowired
  SdcDuplicateRepository sdcDuplicateRepository;

  @BeforeEach
  public void before() {
    enrolledProgramCodeRepository.save(createEnrolledProgramCode05Data());
    careerProgramCodeRepository.save(createCareerProgramCodeData());
    homeLanguageSpokenCodeRepository.save(homeLanguageSpokenCodeData());
    bandCodeRepository.saveAll(bandCodeData());
    fundingCodeRepository.save(fundingCodeData());
    fundingCodeRepository.save(fundingCode16Data());
    fundingCodeRepository.save(fundingCode20Data());
    enrolledGradeCodeRepository.save(enrolledGradeCode01Data());
    enrolledGradeCodeRepository.save(enrolledGradeCodeHSData());
    enrolledGradeCodeRepository.save(enrolledGradeCode08Data());
    enrolledGradeCodeRepository.save(enrolledGradeCode10Data());
    specialEducationCategoryRepository.save(specialEducationCategoryCodeData());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode08Data());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode14Data());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode17Data());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode33Data());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode41Data());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode40Data());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode35ExpiredData());
    genderCodeRepository.save(createGenderCodeData());
    schoolGradeCodeRepository.save(createSchoolGradeCodeData());
    schoolFundingGroupCodeRepository.save(createSchoolFundingGroupCodeData());
    collectionTypeCodeRepository.save(createCollectionTypeCodeData());
    programDuplicateTypeCodeRepository.save(createProgramDuplicateTypeCodeData());
    sdcSchoolCollectionStatusCodeRepository.save(createSdcSchoolCollectionStatusCodeData());
  }

  @AfterEach
  public void resetState() {
    this.studentDataCollectionTestUtils.cleanDB();
  }

  public CollectionEntity createMockCollectionEntity(){
    CollectionEntity sdcEntity = new CollectionEntity();
    sdcEntity.setCollectionTypeCode("SEPTEMBER");
    sdcEntity.setOpenDate(LocalDateTime.now());
    sdcEntity.setCloseDate(LocalDateTime.now().plusDays(5));
    sdcEntity.setCollectionStatusCode(CollectionStatus.INPROGRESS.getCode());
    sdcEntity.setSnapshotDate(LocalDate.of(sdcEntity.getOpenDate().getYear(), 9, 29));
    sdcEntity.setSubmissionDueDate(sdcEntity.getSnapshotDate().plusDays(3));
    sdcEntity.setDuplicationResolutionDueDate(sdcEntity.getSnapshotDate().plusDays(6));
    sdcEntity.setSignOffDueDate(sdcEntity.getSnapshotDate().plusDays(9));
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());
    return sdcEntity;
  }

  public SchoolGradeCodeEntity createSchoolGradeCodeData() {
    return SchoolGradeCodeEntity.builder().schoolGradeCode("GRADE01").label("Grade1").legacyCode("01").description("G1")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Business").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public SchoolFundingGroupCodeEntity createSchoolFundingGroupCodeData() {
    return SchoolFundingGroupCodeEntity.builder().schoolFundingGroupCode("GROUP1").label("Group1").legacyCode("01").description("G1")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Business").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public CollectionTypeCodeEntity createCollectionTypeCodeData() {
    return CollectionTypeCodeEntity.builder().collectionTypeCode("SEPTEMBER").label("September").description("September collection")
            .displayOrder(10).effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.now().plusYears(10)).openDate(LocalDateTime.now().minusMonths(1))
            .closeDate(LocalDateTime.now().plusMonths(1)).createUser("TEST").snapshotDate(LocalDate.now()).updateUser("TEST").build();
  }

  public ProgramDuplicateTypeCodeEntity createProgramDuplicateTypeCodeData() {
    return ProgramDuplicateTypeCodeEntity.builder().programDuplicateTypeCode("SPECIAL_ED").label("Special Education").description("Special Education duplicate")
            .displayOrder(10).effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.now().plusYears(10)).createUser("TEST").updateUser("TEST").build();
  }

  public SdcSchoolCollectionStatusCodeEntity createSdcSchoolCollectionStatusCodeData() {
    return SdcSchoolCollectionStatusCodeEntity.builder().sdcSchoolCollectionStatusCode("NEW").label("New").description("Collection record has been created")
            .displayOrder(10).effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.now().plusYears(10)).createUser("TEST").updateUser("TEST").build();
  }

  public IndependentSchoolFundingGroupSnapshotEntity createMockIndependentSchoolFundingGroupSnapshotEntity(UUID schoolID, UUID collectionID){
    IndependentSchoolFundingGroupSnapshotEntity independentSchoolFundingGroupEntity = new IndependentSchoolFundingGroupSnapshotEntity();
    independentSchoolFundingGroupEntity.setSchoolID(schoolID);
    independentSchoolFundingGroupEntity.setCollectionID(collectionID);
    independentSchoolFundingGroupEntity.setSchoolFundingGroupCode("GROUP1");
    independentSchoolFundingGroupEntity.setSchoolGradeCode("GRADE01");
    independentSchoolFundingGroupEntity.setCreateUser("ABC");
    independentSchoolFundingGroupEntity.setCreateDate(LocalDateTime.now());
    independentSchoolFundingGroupEntity.setUpdateUser("ABC");
    independentSchoolFundingGroupEntity.setUpdateDate(LocalDateTime.now());
    return independentSchoolFundingGroupEntity;
  }

  public SdcDistrictCollectionEntity createMockSdcDistrictCollectionEntity(CollectionEntity entity, UUID districtID){
    SdcDistrictCollectionEntity sdcEntity = new SdcDistrictCollectionEntity();
    sdcEntity.setCollectionEntity(entity);
    sdcEntity.setSdcDistrictCollectionStatusCode("NEW");
    sdcEntity.setDistrictID(districtID == null ? UUID.randomUUID() : districtID);
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());

    return sdcEntity;
  }

  public SdcSchoolCollectionEntity createMockSdcSchoolCollectionEntity(CollectionEntity entity, UUID schoolID){
    SdcSchoolCollectionEntity sdcEntity = new SdcSchoolCollectionEntity();
    sdcEntity.setCollectionEntity(entity);
    sdcEntity.setSchoolID(schoolID == null ? UUID.randomUUID() : schoolID);
    sdcEntity.setUploadDate(LocalDateTime.now());
    sdcEntity.setUploadFileName("abc.txt");
    sdcEntity.setUploadReportDate(null);
    sdcEntity.setSdcSchoolCollectionStatusCode("NEW");
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());
    sdcEntity.setSdcSchoolCollectionHistoryEntities(new HashSet<>());
    sdcEntity.setSdcSchoolStudentEntities(new HashSet<>());

    return sdcEntity;
  }

  public SdcSchoolCollectionStudentEntity createMockSchoolStudentEntity(SdcSchoolCollectionEntity sdcSchoolCollectionEntity){
    SdcSchoolCollectionStudentEntity sdcEntity = new SdcSchoolCollectionStudentEntity();
    sdcEntity.setSdcSchoolCollection(sdcSchoolCollectionEntity);
    sdcEntity.setLocalID("A11111111");
    sdcEntity.setStudentPen("120164447");
    sdcEntity.setLegalFirstName("JIM");
    sdcEntity.setLegalMiddleNames("BOB");
    sdcEntity.setLegalLastName("DANDY");
    sdcEntity.setUsualFirstName("JIMMY");
    sdcEntity.setUsualMiddleNames("BOBBY");
    sdcEntity.setUsualLastName("DANDY");
    sdcEntity.setDob("20160101");
    sdcEntity.setGender("M");
    sdcEntity.setSpecialEducationCategoryCode("A");
    sdcEntity.setSchoolFundingCode("20");
    sdcEntity.setNativeAncestryInd("N");
    sdcEntity.setHomeLanguageSpokenCode("001");
    sdcEntity.setOtherCourses(null);
    sdcEntity.setSupportBlocks(null);
    sdcEntity.setEnrolledGradeCode("08");
    sdcEntity.setEnrolledProgramCodes("");
    sdcEntity.setCareerProgramCode("");
    sdcEntity.setNumberOfCourses("0400");
    sdcEntity.setBandCode("0500");
    sdcEntity.setPostalCode("V0V0V0");
    sdcEntity.setSdcSchoolCollectionStudentStatusCode("LOADED");
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());
    sdcEntity.setIsSchoolAged(true);
    sdcEntity.setIsAdult(false);
    sdcEntity.setIsGraduated(false);
    sdcEntity.setSdcStudentEnrolledProgramEntities(new HashSet<>());
    sdcEntity.setSdcStudentValidationIssueEntities(new HashSet<>());
    return sdcEntity;
  }

  public SdcSchoolCollectionStudentEntity createMockSchoolStudentForSagaEntity(SdcSchoolCollectionEntity sdcSchoolCollectionEntity){
    SdcSchoolCollectionStudentEntity sdcEntity = new SdcSchoolCollectionStudentEntity();
    sdcEntity.setSdcSchoolCollection(sdcSchoolCollectionEntity);
    sdcEntity.setLocalID("LOCAL123");
    sdcEntity.setStudentPen("PEN123456");
    sdcEntity.setLegalFirstName("John");
    sdcEntity.setLegalMiddleNames("Michael");
    sdcEntity.setLegalLastName("Doe");
    sdcEntity.setUsualFirstName("John");
    sdcEntity.setUsualMiddleNames("Mike");
    sdcEntity.setUsualLastName("Doe");
    sdcEntity.setDob("20050515");
    sdcEntity.setGender("M");
    sdcEntity.setSpecialEducationCategoryCode("SPED01");
    sdcEntity.setSchoolFundingCode("FUND02");
    sdcEntity.setNativeAncestryInd("N");
    sdcEntity.setHomeLanguageSpokenCode("ENG");
    sdcEntity.setOtherCourses("0");
    sdcEntity.setSupportBlocks("1");
    sdcEntity.setEnrolledGradeCode("10");
    sdcEntity.setEnrolledProgramCodes("PROG001,PROG002");
    sdcEntity.setCareerProgramCode("CAREER001");
    sdcEntity.setNumberOfCourses("6");
    sdcEntity.setBandCode("");
    sdcEntity.setPostalCode("V6G 1A1");
    sdcEntity.setSdcSchoolCollectionStudentStatusCode("ACTIVE");
    sdcEntity.setIsAdult(false);
    sdcEntity.setIsSchoolAged(true);
    sdcEntity.setIsGraduated(false);
    sdcEntity.setAssignedStudentId(UUID.randomUUID());
    sdcEntity.setAssignedPen("120164447");
    sdcEntity.setIsAdult(false);
    sdcEntity.setFte(new BigDecimal(0.875));
    sdcEntity.setFteZeroReasonCode("REASON001");
    sdcEntity.setFrenchProgramNonEligReasonCode("REASON002");
    sdcEntity.setEllNonEligReasonCode("REASON003");
    sdcEntity.setIndigenousSupportProgramNonEligReasonCode("REASON004");
    sdcEntity.setCareerProgramNonEligReasonCode("REASON005");
    sdcEntity.setSpecialEducationNonEligReasonCode("REASON006");
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());
    sdcEntity.setIsSchoolAged(true);
    sdcEntity.setIsAdult(false);
    sdcEntity.setIsGraduated(false);
    sdcEntity.setSdcStudentEnrolledProgramEntities(new HashSet<>());
    sdcEntity.setSdcStudentValidationIssueEntities(new HashSet<>());
    return sdcEntity;
  }

  public SdcStudentEllEntity createMockStudentEllEntity(SdcSchoolCollectionStudentEntity student) {
    SdcStudentEllEntity entity = new SdcStudentEllEntity();
    entity.setStudentID(student.getAssignedStudentId());
    entity.setCreateDate(LocalDateTime.now());
    entity.setCreateUser("ABC");
    entity.setUpdateDate(LocalDateTime.now());
    entity.setUpdateUser("ABC");
    entity.setYearsInEll(0);
    return entity;
  }

  public SdcSchoolCollectionStudentValidationIssueEntity createMockSdcSchoolCollectionStudentValidationIssueEntity(
    SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity,
    StudentValidationIssueSeverityCode issueSeverityCode
  ){
    SdcSchoolCollectionStudentValidationIssueEntity sdcSchoolCollectionStudentValidationIssueEntity = new SdcSchoolCollectionStudentValidationIssueEntity();
    sdcSchoolCollectionStudentValidationIssueEntity.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentEntity);

    sdcSchoolCollectionStudentValidationIssueEntity.setValidationIssueSeverityCode(issueSeverityCode.toString());
    sdcSchoolCollectionStudentValidationIssueEntity.setValidationIssueCode("LEGALFIRSTNAMECHARFIX");
    sdcSchoolCollectionStudentValidationIssueEntity.setValidationIssueFieldCode("LEGAL_FIRST_NAME");
    sdcSchoolCollectionStudentValidationIssueEntity.setCreateUser("ABC");
    sdcSchoolCollectionStudentValidationIssueEntity.setCreateDate(LocalDateTime.now());
    sdcSchoolCollectionStudentValidationIssueEntity.setUpdateUser("ABC");
    sdcSchoolCollectionStudentValidationIssueEntity.setUpdateDate(LocalDateTime.now());
    return sdcSchoolCollectionStudentValidationIssueEntity;
  }

  @SneakyThrows
  protected SdcSagaEntity createMockSaga(final SdcSchoolCollectionStudentEntity student) {
    return SdcSagaEntity.builder()
      .updateDate(LocalDateTime.now().minusMinutes(15))
      .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
      .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
      .createDate(LocalDateTime.now().minusMinutes(15))
      .sagaName(SagaEnum.STUDENT_DATA_COLLECTION_STUDENT_PROCESSING_SAGA.toString())
      .status(SagaStatusEnum.IN_PROGRESS.toString())
      .sagaState(EventType.INITIATED.toString())
      .payload(JsonUtil.getJsonStringFromObject(SdcStudentSagaData.builder().sdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolCollectionStudentWithValidationIssues(student)).school(createMockSchool()).build()))
      .build();
  }

  public SchoolTombstone createMockSchool() {
    final SchoolTombstone schoolTombstone = new SchoolTombstone();
    schoolTombstone.setSchoolId(UUID.randomUUID().toString());
    schoolTombstone.setDistrictId(UUID.randomUUID().toString());
    schoolTombstone.setDisplayName("Marco's school");
    schoolTombstone.setMincode("03636018");
    schoolTombstone.setOpenedDate("1964-09-01T00:00:00");
    schoolTombstone.setSchoolCategoryCode("PUBLIC");
    schoolTombstone.setSchoolReportingRequirementCode("REGULAR");
    schoolTombstone.setFacilityTypeCode("STANDARD");
    return schoolTombstone;
  }

  public District createMockDistrict() {
    final District district = District.builder().build();
    district.setDistrictId(UUID.randomUUID().toString());
    district.setDisplayName("Marco's district");
    district.setDistrictNumber("036");
    district.setDistrictStatusCode("ACTIVE");
    district.setPhoneNumber("123456789");
    return district;
  }

  public StudentRuleData createMockStudentRuleData(final SdcSchoolCollectionStudentEntity student, final SchoolTombstone schoolTombstone) {
    final StudentRuleData studentRuleData = new StudentRuleData();
    studentRuleData.setSchool(schoolTombstone);
    studentRuleData.setSdcSchoolCollectionStudentEntity(student);
    return studentRuleData;
  }
  public CollectionTypeCodeEntity createMockCollectionCodeEntity() {
    return CollectionTypeCodeEntity.builder().collectionTypeCode("SEPTEMBER").label("Test")
        .description("Test code").displayOrder(0).effectiveDate(
            LocalDateTime.now().minusDays(1)).expiryDate(LocalDateTime.now().plusDays(7))
        .openDate(LocalDateTime.now())
        .closeDate(LocalDateTime.now().plusDays(7)).createUser("TEST").createDate(LocalDateTime.now())
        .updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }
  public CollectionCodeCriteriaEntity createMockCollectionCodeCriteriaEntity(
      CollectionTypeCodeEntity collectionCodeEntity) {
    return CollectionCodeCriteriaEntity.builder().collectionTypeCodeEntity(collectionCodeEntity)
        .schoolCategoryCode("TEST_CC").facilityTypeCode("TEST_FTC").reportingRequirementCode("TEST_RRC")
        .createUser("TEST").createDate(LocalDateTime.now())
        .updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }

  public UUID createMockHistoricalCollection(int yearsAgo, UUID schoolID, LocalDateTime currentCollectionOpenDate, String collectionTypeCode){
    LocalDateTime historicalOpenDate = currentCollectionOpenDate.minusYears(yearsAgo);
    CollectionEntity historicalCollectionEntity = createMockCollectionEntity();
    historicalCollectionEntity.setOpenDate(historicalOpenDate);
    collectionRepository.save(historicalCollectionEntity);

    SdcSchoolCollectionEntity historicalSdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(historicalCollectionEntity, schoolID);
    historicalSdcSchoolCollectionEntity.setCreateDate(historicalOpenDate);
    historicalSdcSchoolCollectionEntity.setSchoolID(schoolID);
    historicalCollectionEntity.setCollectionTypeCode(collectionTypeCode);
    sdcSchoolCollectionRepository.save(historicalSdcSchoolCollectionEntity);

    SdcSchoolCollectionStudentEntity historicalSdcSchoolCollectionStudentEntity = createMockSchoolStudentEntity(historicalSdcSchoolCollectionEntity);
    sdcSchoolCollectionStudentRepository.save(historicalSdcSchoolCollectionStudentEntity);

    return historicalSdcSchoolCollectionEntity.getSdcSchoolCollectionID();
  }

  public SdcDuplicateEntity createMockSdcDuplicateEntity(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity1, SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity2) {
    var sdcDuplicateEntity = SdcDuplicateEntity.builder().sdcDuplicateID(UUID.randomUUID())
            .duplicateErrorDescriptionCode(DuplicateErrorDescriptionCode.K_TO_9_DUP.getCode())
            .duplicateLevelCode(DuplicateLevelCode.IN_DIST.getCode())
            .duplicateSeverityCode(DuplicateSeverityCode.NON_ALLOWABLE.getCode())
            .duplicateTypeCode(DuplicateTypeCode.ENROLLMENT.getCode()).build();
    var student1 = createMockSdcDuplicateStudentEntity(sdcSchoolCollectionStudentEntity1, sdcDuplicateEntity);
    var student2 = createMockSdcDuplicateStudentEntity(sdcSchoolCollectionStudentEntity2, sdcDuplicateEntity);
    sdcDuplicateEntity.getSdcDuplicateStudentEntities().addAll(Arrays.asList(student1, student2));
    return sdcDuplicateEntity;
  }

  private static SdcDuplicateStudentEntity createMockSdcDuplicateStudentEntity(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity, SdcDuplicateEntity sdcDuplicateEntity) {
    return SdcDuplicateStudentEntity.builder()
            .sdcDuplicateStudentID(UUID.randomUUID())
            .sdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentEntity)
            .sdcDistrictCollectionID(sdcSchoolCollectionStudentEntity.getSdcSchoolCollection().getSdcDistrictCollectionID())
            .sdcDuplicateEntity(sdcDuplicateEntity)
            .build();
  }

  public static String asJsonString(final Object obj) {
    try {
      ObjectMapper om = new ObjectMapper();
      om.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      return om.writeValueAsString(obj);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCode05Data() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("05").description("Programme Francophone")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Francophone").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCode08Data() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("08").description("08")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("08").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCode14Data() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("14").description("14")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("14").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCode17Data() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("17").description("17")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("17").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCode40Data() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("40").description("40")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("40").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCode35ExpiredData() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("35").description("35")
            .effectiveDate(LocalDateTime.now().minusDays(10)).expiryDate(LocalDateTime.now().minusDays(1)).displayOrder(1).label("35").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCode33Data() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("33").description("33")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("33").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCode41Data() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("41").description("41")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("41").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }
  public CareerProgramCodeEntity createCareerProgramCodeData() {
    return CareerProgramCodeEntity.builder().careerProgramCode("XA").description("Business")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Business").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }
  public GenderCodeEntity createGenderCodeData() {
    return GenderCodeEntity.builder().genderCode("M").description("Male")
        .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Male").createDate(LocalDateTime.now())
        .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }
  public HomeLanguageSpokenCodeEntity homeLanguageSpokenCodeData() {
    return HomeLanguageSpokenCodeEntity.builder().homeLanguageSpokenCode("001").description("Portuguese")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Portuguese").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }
  public List<BandCodeEntity> bandCodeData() {
    List<BandCodeEntity> bandCodeList = new ArrayList<>();
    bandCodeList.add(BandCodeEntity.builder().bandCode("0600").description("SPLATSIN")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("SPLATSIN").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build());
    bandCodeList.add(BandCodeEntity.builder().bandCode("0700").description("BOOTHROYD")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("BOOTHROYD - AFA").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build());
    bandCodeList.add(BandCodeEntity.builder().bandCode("0500").description("KWANLIN DUN")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("KWANLIN DUN").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build());

    return bandCodeList;
  }
  public SchoolFundingCodeEntity fundingCodeData() {
    return SchoolFundingCodeEntity.builder().schoolFundingCode("14").description("OUT-OF-PROVINCE/INTERNATIONAL")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("OUT-OF-PROVINCE/INTERNATIONAL").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public SchoolFundingCodeEntity fundingCode20Data() {
    return SchoolFundingCodeEntity.builder().schoolFundingCode("20").description("STATUS INDIAN ON RESERVE")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("STATUS INDIAN ON RESERVE").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public SchoolFundingCodeEntity fundingCode16Data() {
    return SchoolFundingCodeEntity.builder().schoolFundingCode("16").description("NEWCOMER/REFUGEE")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("NEWCOMER/REFUGEE").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledGradeCodeEntity enrolledGradeCode01Data() {
    return EnrolledGradeCodeEntity.builder().enrolledGradeCode("01").description("Grade 1")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Grade 1").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledGradeCodeEntity enrolledGradeCodeHSData() {
    return EnrolledGradeCodeEntity.builder().enrolledGradeCode("HS").description("Home School")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Home School").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledGradeCodeEntity enrolledGradeCode08Data() {
    return EnrolledGradeCodeEntity.builder().enrolledGradeCode("08").description("Grade 8")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Grade 8").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledGradeCodeEntity enrolledGradeCode10Data() {
    return EnrolledGradeCodeEntity.builder().enrolledGradeCode("10").description("Grade 10")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Grade 10").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public SpecialEducationCategoryCodeEntity specialEducationCategoryCodeData() {
    return SpecialEducationCategoryCodeEntity.builder().specialEducationCategoryCode("A").description("PHYS DEPEND")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("PHYS DEPEND").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public PenMatchResult getPenMatchResult(){
      PenMatchResult penMatchResult = new PenMatchResult();
      PenMatchRecord penMatchRecord = new PenMatchRecord();
      penMatchRecord.setMatchingPEN("123456789");
      penMatchRecord.setStudentID(UUID.randomUUID().toString());
      penMatchResult.setMatchingRecords(Arrays.asList(penMatchRecord));
      penMatchResult.setPenStatus("AA");
      penMatchResult.setPenStatusMessage("ABC");
      return penMatchResult;
  }

  public GradStatusResult getGradStatusResult(){
      GradStatusResult gradStatusResult = new GradStatusResult();
      gradStatusResult.setException(null);
      gradStatusResult.setProgram("ABC");
      gradStatusResult.setProgramCompletionDate("2023-08-09");
      return gradStatusResult;
  }

}
