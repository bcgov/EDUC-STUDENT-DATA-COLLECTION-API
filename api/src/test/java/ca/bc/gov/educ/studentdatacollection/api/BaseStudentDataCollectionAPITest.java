package ca.bc.gov.educ.studentdatacollection.api;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeCriteriaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
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

  public SdcSchoolCollectionEntity createMockSdcSchoolCollectionEntity(CollectionEntity entity, UUID schoolID){
    SdcSchoolCollectionEntity sdcEntity = new SdcSchoolCollectionEntity();
    sdcEntity.setCollectionEntity(entity);
    sdcEntity.setSchoolID(schoolID == null ? UUID.randomUUID() : schoolID);
    sdcEntity.setUploadDate(LocalDateTime.now());
    sdcEntity.setUploadFileName("abc.txt");
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
    sdcEntity.setStudentPen("123456789");
    sdcEntity.setLegalFirstName("JIM");
    sdcEntity.setLegalMiddleNames("BOB");
    sdcEntity.setLegalLastName("DANDY");
    sdcEntity.setUsualFirstName("JIMMY");
    sdcEntity.setUsualMiddleNames("BOBBY");
    sdcEntity.setUsualLastName("DANDY");
    sdcEntity.setDob("19990101");
    sdcEntity.setGender("M");
    sdcEntity.setSpecialEducationCategoryCode("B");
    sdcEntity.setSchoolFundingCode("05");
    sdcEntity.setNativeAncestryInd("N");
    sdcEntity.setHomeLanguageSpokenCode("001");
    sdcEntity.setOtherCourses(null);
    sdcEntity.setSupportBlocks(null);
    sdcEntity.setEnrolledGradeCode("01");
    sdcEntity.setEnrolledProgramCodes("0000000000000005");
    sdcEntity.setCareerProgramCode("AA");
    sdcEntity.setNumberOfCourses(null);
    sdcEntity.setBandCode("0542");
    sdcEntity.setPostalCode("V0V0V0");
    sdcEntity.setSdcSchoolCollectionStudentStatusCode("LOADED");
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());
    return sdcEntity;
  }

  @SneakyThrows
  protected SdcSagaEntity creatMockSaga(final SdcSchoolCollectionStudent student) {
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
    school.setDisplayName("Marco's school");
    school.setMincode("03636018");
    school.setOpenedDate("1964-09-01T00:00:00");
    school.setSchoolCategoryCode("PUBLIC");
    school.setSchoolReportingRequirementCode("CSF");
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

}
