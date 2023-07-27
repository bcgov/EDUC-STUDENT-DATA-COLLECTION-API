package ca.bc.gov.educ.studentdatacollection.api;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
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

import java.time.LocalDateTime;
import java.util.UUID;

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

  @BeforeEach
  public void before() {
    enrolledProgramCodeRepository.save(createEnrolledProgramCodeData());
    careerProgramCodeRepository.save(createCareerProgramCodeData());
    homeLanguageSpokenCodeRepository.save(homeLanguageSpokenCodeData());
    bandCodeRepository.save(bandCodeData());
    fundingCodeRepository.save(fundingCodeData());
    fundingCodeRepository.save(fundingCode20Data());
    enrolledGradeCodeRepository.save(enrolledGradeCodeData());
    enrolledGradeCodeRepository.save(enrolledGradeCodeHSData());
    enrolledGradeCodeRepository.save(enrolledGradeCodeEightData());
    specialEducationCategoryRepository.save(specialEducationCategoryCodeData());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode14Data());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode33Data());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode41Data());
    enrolledProgramCodeRepository.save(createEnrolledProgramCode40Data());
    genderCodeRepository.save(createGenderCodeData());
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
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());
    return sdcEntity;
  }

  public SdcSchoolCollectionEntity createMockSdcSchoolCollectionEntity(CollectionEntity entity, UUID schoolID, UUID districtID){
    SdcSchoolCollectionEntity sdcEntity = new SdcSchoolCollectionEntity();
    sdcEntity.setCollectionEntity(entity);
    sdcEntity.setSchoolID(schoolID == null ? UUID.randomUUID() : schoolID);
    sdcEntity.setDistrictID(districtID == null ? UUID.randomUUID() : districtID);
    sdcEntity.setUploadDate(LocalDateTime.now());
    sdcEntity.setUploadFileName("abc.txt");
    sdcEntity.setUploadReportDate(null);
    sdcEntity.setSdcSchoolCollectionStatusCode("NEW");
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());

    return sdcEntity;
  }

  public SdcSchoolCollectionStudentEntity createMockSchoolStudentEntity(SdcSchoolCollectionEntity sdcSchoolCollectionEntity){
    SdcSchoolCollectionStudentEntity sdcEntity = new SdcSchoolCollectionStudentEntity();
    sdcEntity.setSdcSchoolCollectionID(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
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
    sdcEntity.setEnrolledProgramCodes("4000000000000005");
    sdcEntity.setCareerProgramCode("XA");
    sdcEntity.setNumberOfCourses(null);
    sdcEntity.setBandCode("0500");
    sdcEntity.setPostalCode("V0V0V0");
    sdcEntity.setSdcSchoolCollectionStudentStatusCode("LOADED");
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());
    return sdcEntity;
  }

  public SdcSchoolCollectionStudentValidationIssueEntity createMockSdcSchoolCollectionStudentValidationIssueEntity(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity){
    SdcSchoolCollectionStudentValidationIssueEntity sdcSchoolCollectionStudentValidationIssueEntity = new SdcSchoolCollectionStudentValidationIssueEntity();
    sdcSchoolCollectionStudentValidationIssueEntity.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentEntity);

    sdcSchoolCollectionStudentValidationIssueEntity.setValidationIssueSeverityCode("ERROR");
    sdcSchoolCollectionStudentValidationIssueEntity.setValidationIssueCode("LEGALFIRSTNAMECHARFIX");
    sdcSchoolCollectionStudentValidationIssueEntity.setValidationIssueFieldCode("LEGAL_FIRST_NAME");
    sdcSchoolCollectionStudentValidationIssueEntity.setCreateUser("ABC");
    sdcSchoolCollectionStudentValidationIssueEntity.setCreateDate(LocalDateTime.now());
    sdcSchoolCollectionStudentValidationIssueEntity.setUpdateUser("ABC");
    sdcSchoolCollectionStudentValidationIssueEntity.setUpdateDate(LocalDateTime.now());
    return sdcSchoolCollectionStudentValidationIssueEntity;
  }

  @SneakyThrows
  protected SdcSagaEntity createMockSaga(final SdcSchoolCollectionStudent student) {
    return SdcSagaEntity.builder()
      .sagaId(UUID.randomUUID())
      .updateDate(LocalDateTime.now().minusMinutes(15))
      .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
      .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
      .createDate(LocalDateTime.now().minusMinutes(15))
      .sagaName(SagaEnum.STUDENT_DATA_COLLECTION_STUDENT_PROCESSING_SAGA.toString())
      .status(SagaStatusEnum.IN_PROGRESS.toString())
      .sagaState(EventType.INITIATED.toString())
      .payload(JsonUtil.getJsonStringFromObject(createMockStudentSagaData(student, createMockSchool())))
      .build();
  }

  public School createMockSchool() {
    final School school = new School();
    school.setSchoolId(UUID.randomUUID().toString());
    school.setDistrictId(UUID.randomUUID().toString());
    school.setDisplayName("Marco's school");
    school.setMincode("03636018");
    school.setOpenedDate("1964-09-01T00:00:00");
    school.setSchoolCategoryCode("PUBLIC");
    school.setSchoolReportingRequirementCode("CSF");
    school.setFacilityTypeCode("Standard");
    return school;
  }

  public SdcStudentSagaData createMockStudentSagaData(final SdcSchoolCollectionStudent student, final School school) {
    final SdcStudentSagaData sdcStudentSagaData = new SdcStudentSagaData();
    sdcStudentSagaData.setSchool(school);
    sdcStudentSagaData.setCollectionTypeCode("SEPTEMBER");
    sdcStudentSagaData.setSdcSchoolCollectionStudent(student);
    return sdcStudentSagaData;
  }
  public CollectionTypeCodeEntity createMockCollectionCodeEntity() {
    return CollectionTypeCodeEntity.builder().collectionTypeCode("SEPTEMBER").label("Test")
        .description("Test code").displayOrder(0).effectiveDate(
            LocalDateTime.now()).expiryDate(LocalDateTime.now().plusDays(7))
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

  public static String asJsonString(final Object obj) {
    try {
      ObjectMapper om = new ObjectMapper();
      om.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      return om.writeValueAsString(obj);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCodeData() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("05").description("Programme Francophone")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Francophone").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCode14Data() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("14").description("14")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("14").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledProgramCodeEntity createEnrolledProgramCode40Data() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("40").description("40")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("40").createDate(LocalDateTime.now())
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
  public BandCodeEntity bandCodeData() {
    return BandCodeEntity.builder().bandCode("0500").description("KWANLIN DUN")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("KWANLIN DUN").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
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

  public EnrolledGradeCodeEntity enrolledGradeCodeData() {
    return EnrolledGradeCodeEntity.builder().enrolledGradeCode("01").description("Grade 1")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Grade 1").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledGradeCodeEntity enrolledGradeCodeHSData() {
    return EnrolledGradeCodeEntity.builder().enrolledGradeCode("HS").description("Home School")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Home School").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public EnrolledGradeCodeEntity enrolledGradeCodeEightData() {
    return EnrolledGradeCodeEntity.builder().enrolledGradeCode("08").description("Grade 8")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Grade 8").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public SpecialEducationCategoryCodeEntity specialEducationCategoryCodeData() {
    return SpecialEducationCategoryCodeEntity.builder().specialEducationCategoryCode("A").description("PHYS DEPEND")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("PHYS DEPEND").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

}
