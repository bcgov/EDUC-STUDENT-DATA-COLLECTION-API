package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateErrorDescriptionCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateLevelCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileSummary;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL.BASE_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SdcFileControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  CollectionRepository sdcRepository;

  @Autowired
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Autowired
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  @Autowired
  SdcSchoolCollectionStudentRepository schoolStudentRepository;

  @Autowired
  SdcDuplicateRepository sdcDuplicateRepository;

  @Autowired
  SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;

  @Autowired
  SdcSchoolCollectionStudentValidationIssueRepository sdcSchoolCollectionStudentValidationIssueRepository;

  @Autowired
  RestUtils restUtils;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

  protected final static ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void afterEach() {
    this.sdcDuplicateRepository.deleteAll();
    this.schoolStudentRepository.deleteAll();
    this.sdcSchoolCollectionRepository.deleteAll();
    this.sdcRepository.deleteAll();
    this.sdcDistrictCollectionRepository.deleteAll();
  }

  @Test
  void testProcessSdcFile_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    final var students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();
  }

  @Test
  void testProcessSdcFile_givenVerFiletype_ShouldReturnStatusOk() throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    SchoolTombstone schoolTombstone = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
      collection,
      UUID.fromString(schoolTombstone.getSchoolId())
    );
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository
      .save(sdcMockSchool);


    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    SdcFileUpload verFile = SdcFileUpload.builder()
      .fileContents(fileContents)
      .createUser("ABC")
      .fileName("SampleUpload.ver")
      .build();

    String cid = sdcSchoolCollection.getSdcSchoolCollectionID().toString();
    this.mockMvc.perform(post( BASE_URL + "/" + cid + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(verFile))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
  }

  @ParameterizedTest
  @CsvSource({
          "SampleUpload.file.std",
          "SampleUpload.STD",
          "SampleUpload.std"
  })
  void testProcessSdcFile_givenStdFiletype_ShouldReturnStatusOk(String fileName) throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    SchoolTombstone schoolTombstone = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
      collection,
      UUID.fromString(schoolTombstone.getSchoolId())
    );
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository
      .save(sdcMockSchool);

    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    SdcFileUpload stdFile = SdcFileUpload.builder()
      .fileContents(fileContents)
      .createUser("ABC")
      .fileName(fileName)
      .build();

    String cid = sdcSchoolCollection.getSdcSchoolCollectionID().toString();
    this.mockMvc.perform(post( BASE_URL + "/" + cid + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(stdFile))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
  }

  @ParameterizedTest
  @CsvSource({
          "SampleUpload.nope",
          "SampleUpload.ABC",
          "SampleUpload."
  })
  void testProcessSdcFile_givenInvalidFiletype_ShouldReturnStatusBadRequest(String filename) throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    SchoolTombstone schoolTombstone = this.createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
      collection,
      UUID.fromString(schoolTombstone.getSchoolId())
    );
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository
      .save(sdcMockSchool);

    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    SdcFileUpload stdFile = SdcFileUpload.builder()
      .fileContents(fileContents)
      .createUser("ABC")
      .fileName(filename)
      .build();

    String cid = sdcSchoolCollection.getSdcSchoolCollectionID().toString();
    this.mockMvc.perform(post( BASE_URL + "/" + cid + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(stdFile))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  void testProcessSdcFile_givenMincodeMismatch_ShouldReturnStatusBadRequest() throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    SchoolTombstone schoolTombstone = this.createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
      collection,
      UUID.fromString(schoolTombstone.getSchoolId())
    );
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository
      .save(sdcMockSchool);

    final FileInputStream fis = new FileInputStream("src/test/resources/mincode-mismatch.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    SdcFileUpload stdFile = SdcFileUpload.builder()
      .fileContents(fileContents)
      .createUser("ABC")
      .fileName("SampleUpload.std")
      .build();

    String cid = sdcSchoolCollection.getSdcSchoolCollectionID().toString();
    this.mockMvc.perform(post( BASE_URL + "/" + cid + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(stdFile))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  void testProcessSdcFNCharsFile_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    school.setMincode("00603007");
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-2-student-fnchars.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    final var students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();
  }

  @Test
  void testProcessSdcFileReUpload_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    var students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();
    students.forEach(sdcSchoolCollectionStudentEntity -> {
      sdcSchoolCollectionStudentEntity.setSdcSchoolCollectionStudentStatusCode("ERROR");
      this.schoolStudentRepository.save(sdcSchoolCollectionStudentEntity);
    });

    var historyStuds = this.sdcSchoolCollectionStudentHistoryRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(historyStuds).isNotNull().hasSize(1);

    SdcSchoolCollectionStudentValidationIssueEntity vIssue = new SdcSchoolCollectionStudentValidationIssueEntity();
    vIssue.setCreateUser("ABC");
    vIssue.setValidationIssueCode("ABC");
    vIssue.setValidationIssueFieldCode("ABC");
    vIssue.setValidationIssueSeverityCode("ABC");
    vIssue.setSdcSchoolCollectionStudentEntity(students.get(0));
    sdcSchoolCollectionStudentValidationIssueRepository.save(vIssue);

    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    assertThat(result).hasSize(1);
    entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();

    var issues = sdcSchoolCollectionStudentValidationIssueRepository.findAll();
    assertThat(issues).isNotNull().hasSize(1);

    var historyStuds2 = this.sdcSchoolCollectionStudentHistoryRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(historyStuds2).isNotNull().hasSize(1);
  }

  @Test
  void testProcessSdcFileReUploadCompare_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-2-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    var students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();
    students.forEach(sdcSchoolCollectionStudentEntity -> {
      sdcSchoolCollectionStudentEntity.setSdcSchoolCollectionStudentStatusCode("ERROR");
      this.schoolStudentRepository.save(sdcSchoolCollectionStudentEntity);
    });

    var historyStuds = this.sdcSchoolCollectionStudentHistoryRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(historyStuds).isNotNull().hasSize(2);

    final FileInputStream fis2 = new FileInputStream("src/test/resources/sample-2-student-diff.txt");
    final String fileContents2 = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis2));
    assertThat(fileContents2).isNotEmpty();
    val body2 = SdcFileUpload.builder().fileContents(fileContents2).createUser("ABC").fileName("SampleUpload.std").build();

    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body2))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    assertThat(result).hasSize(1);
    entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull().hasSize(2);

    var historyStuds2 = this.sdcSchoolCollectionStudentHistoryRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(historyStuds2).isNotNull().hasSize(2);
  }

  @Test
  void testProcessSdcFileReUploadCompareWithDups_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-2-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    var students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();
    students.forEach(sdcSchoolCollectionStudentEntity -> {
      sdcSchoolCollectionStudentEntity.setSdcSchoolCollectionStudentStatusCode("ERROR");
      this.schoolStudentRepository.save(sdcSchoolCollectionStudentEntity);
    });

    var historyStuds = this.sdcSchoolCollectionStudentHistoryRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(historyStuds).isNotNull().hasSize(2);

    final FileInputStream fis2 = new FileInputStream("src/test/resources/sample-2-student-diff.txt");
    final String fileContents2 = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis2));
    assertThat(fileContents2).isNotEmpty();
    val body2 = SdcFileUpload.builder().fileContents(fileContents2).createUser("ABC").fileName("SampleUpload.std").build();

    SdcDuplicateEntity dup = new SdcDuplicateEntity();
    dup.setDuplicateErrorDescriptionCode(DuplicateErrorDescriptionCode.K_TO_7_DUP.getCode());
    dup.setDuplicateLevelCode(DuplicateLevelCode.IN_DIST.getCode());
    dup.setDuplicateTypeCode(DuplicateTypeCode.ENROLLMENT.getCode());
    dup.setDuplicateSeverityCode(DuplicateSeverityCode.ALLOWABLE.getCode());

    SdcDuplicateStudentEntity stud1 = new SdcDuplicateStudentEntity();
    stud1.setSdcSchoolCollectionID(UUID.randomUUID());
    stud1.setSdcDuplicateEntity(dup);
    stud1.setSdcSchoolCollectionStudentEntity(students.get(0));
    stud1.setCreateUser("ABC");
    stud1.setCreateDate(LocalDateTime.now());
    stud1.setUpdateUser("ABC");
    stud1.setUpdateDate(LocalDateTime.now());

    SdcDuplicateStudentEntity stud2 = new SdcDuplicateStudentEntity();
    stud2.setSdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    stud2.setSdcDuplicateEntity(dup);
    stud2.setSdcSchoolCollectionStudentEntity(students.get(1));
    stud2.setCreateUser("ABC");
    stud2.setCreateDate(LocalDateTime.now());
    stud2.setUpdateUser("ABC");
    stud2.setUpdateDate(LocalDateTime.now());

    dup.getSdcDuplicateStudentEntities().add(stud1);
    dup.getSdcDuplicateStudentEntities().add(stud2);

    sdcDuplicateRepository.save(dup);

    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body2))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    assertThat(result).hasSize(1);
    entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull().hasSize(2);

    var historyStuds2 = this.sdcSchoolCollectionStudentHistoryRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(historyStuds2).isNotNull().hasSize(2);

    var dups = sdcDuplicateRepository.findAll();
    assertThat(dups).isNotNull().isEmpty();
  }

  @Test
  void testProcessSdcFileReUploadCompare_WithDeletedStudents_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-2-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");

    var students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull().hasSize(2);
    students.forEach(sdcSchoolCollectionStudentEntity -> {
      sdcSchoolCollectionStudentEntity.setSdcSchoolCollectionStudentStatusCode("ERROR");
      this.schoolStudentRepository.save(sdcSchoolCollectionStudentEntity);
    });

    sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudent(students.get(0).getSdcSchoolCollectionStudentID());

    var studentsAfterDelete = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    var deletedStudent = studentsAfterDelete.stream().filter(student -> student.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase("DELETED"));
    assertThat(deletedStudent).isNotNull().hasSize(1);

    final FileInputStream fis2 = new FileInputStream("src/test/resources/sample-2-student.txt");
    final String fileContents2 = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis2));
    assertThat(fileContents2).isNotEmpty();
    val body2 = SdcFileUpload.builder().fileContents(fileContents2).createUser("ABC").fileName("SampleUpload.std").build();

    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body2))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    assertThat(result).hasSize(1);
    entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    var reUploadedStudents = students.stream().filter(student -> student.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase("DELETED"));
    assertThat(reUploadedStudents).isNotNull().isEmpty();

  }

  @Test
  void testProcessSdcFileCheckProgress_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    final var students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();

    var resultActions = this.mockMvc.perform(get(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SdcFileSummary>() {
    });
    assertThat(summary.getFileName()).isEqualTo(body.getFileName());
    assertThat(summary.getUploadReportDate()).isNotNull();
    assertThat(summary.getPositionInQueue()).isEqualTo("1");
  }

  @Test
  void testProcessSdcFile_givenInvalidPayload_ShouldReturnStatusBadRequest() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId())));
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @CsvSource({
    "src/test/resources/sample-malformed-short-header.txt,Header record is missing characters.",
    "src/test/resources/sample-malformed-long-header.txt,Header record has extraneous characters."
  })
  void testProcessSdcFile_givenMalformedHeaderOrTrailer_ShouldReturnStatusBadRequest(
    final String sample,
    final String errorMessage
  ) throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    SchoolTombstone schoolTombstone = this.createMockSchool();

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));

    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository
      .save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId())));

    final FileInputStream fis = new FileInputStream(sample);

    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();

    SdcFileUpload body = SdcFileUpload
      .builder()
      .fileContents(fileContents)
      .createUser("ABC")
      .fileName("SampleUpload.std")
      .build();

    String cid = sdcSchoolCollection.getSdcSchoolCollectionID().toString();
    MvcResult apiResponse = this.mockMvc.perform(post(BASE_URL + "/" + cid + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)
    ).andExpect(status().isBadRequest()).andReturn();

    assertThat(apiResponse
      .getResponse()
      .getContentAsString()
    ).contains(errorMessage);
  }

  @ParameterizedTest
  @CsvSource({
    "src/test/resources/sample-malformed-short-content.txt,"
    + ".*Detail record \\d+ is missing characters.*",
    "src/test/resources/sample-malformed-long-content.txt,"
    + ".*Detail record \\d+ has extraneous characters.*"
  })
  void testProcessSdcFile_givenMalformedDetailRow_ShouldReturnStatusBadRequest(final String sample, final String errorExpression) throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    SchoolTombstone schoolTombstone = this.createMockSchool();

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolTombstone));

    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId())));

    final FileInputStream fis = new FileInputStream(sample);

    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();

    SdcFileUpload body = SdcFileUpload
      .builder()
      .createUser("ABC")
      .fileContents(fileContents)
      .fileName("SampleUpload.std")
      .build();

    String cid = sdcSchoolCollection.getSdcSchoolCollectionID().toString();
    MvcResult apiResponse = this.mockMvc.perform(post(BASE_URL + "/" + cid + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)
    ).andExpect(status().isBadRequest()).andReturn();

    assertThat(apiResponse
      .getResponse()
      .getContentAsString()
    ).matches(errorExpression);
  }

  @Test
  void testProcessSdcFile_givenInvalidPayloadFile_ShouldReturnStatusBadRequest() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student-bad.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    var resultActions = this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());

    val error = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<>() {
    });
    assertThat(error).isNotNull();
  }

  @Test
  void testProcessSdcFile_givenInvalidPayloadForDates_ShouldReturnStatusBadRequest() throws Exception {
    var mockColl = createMockCollectionEntity();
    mockColl.setOpenDate(LocalDateTime.now().plusDays(5));
    var collection = sdcRepository.save(mockColl);
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().createUser("ABC").fileContents(fileContents).fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @CsvSource({
          "src/test/resources/sample-0-student.txt",
          "src/test/resources/sample-1-student-with-bad-student-count.txt",
          "src/test/resources/sample-1-student-with-malformed-student-count.txt"
  })
  void testProcessSdcFile_givenInvalidPayloadForStudentCounts_ShouldReturnStatusBadRequest(String resourceFile) throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setUploadReportDate(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream(resourceFile);
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  void testProcessDistrictSdcFile_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    var districtID = UUID.randomUUID();
    var districtCollection = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));
    school.setDistrictId(districtID.toString());
    sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId())));
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();

    this.mockMvc.perform(post(BASE_URL + "/district/" + districtCollection.getSdcDistrictCollectionID() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getUploadFileName()).isEqualTo("SampleUpload.std");
    assertThat(entity.getUploadReportDate()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("DIS_UPLOAD");
    final var students = this.schoolStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();
  }

  @Test
  void testProcessSdcFileCheckProgress_givenOtherFilesProcessing_ShouldReturnStatusOk() throws Exception {
    // Create first sdcSchoolCollection
    var collection1 = sdcRepository.save(createMockCollectionEntity());
    var school1 = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school1));
    var sdcMockSchool1 = createMockSdcSchoolCollectionEntity(collection1, UUID.fromString(school1.getSchoolId()));
    sdcMockSchool1.setUploadDate(null);
    sdcMockSchool1.setUploadFileName(null);
    sdcMockSchool1.setUploadReportDate(null);
    var sdcSchoolCollection1 = sdcSchoolCollectionRepository.save(sdcMockSchool1);

    // Upload file for first sdcSchoolCollection
    final FileInputStream fis1 = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents1 = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis1));
    assertThat(fileContents1).isNotEmpty();
    val body1 = SdcFileUpload.builder().fileContents(fileContents1).createUser("ABC").fileName("SampleUpload1.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection1.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body1))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    // Verify results for first sdcSchoolCollection
    var result1 = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result1).hasSize(1);
    var entity1 = result1.get(0);
    assertThat(entity1.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity1.getUploadFileName()).isEqualTo("SampleUpload1.std");
    assertThat(entity1.getUploadReportDate()).isNotNull();
    assertThat(entity1.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");

    // Get file summary for first sdcSchoolCollection
    var resultActions1 = this.mockMvc.perform(get(BASE_URL + "/" + sdcSchoolCollection1.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body1))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SdcFileSummary>() {});
    assertThat(summary1.getFileName()).isEqualTo(body1.getFileName());
    assertThat(summary1.getUploadReportDate()).isNotNull();

    // Create second sdcSchoolCollection
    var collection2 = sdcRepository.save(createMockCollectionEntity());
    var school2 = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school2));
    var sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection2, UUID.fromString(school2.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setUploadReportDate(null);
    var sdcSchoolCollection2 = sdcSchoolCollectionRepository.save(sdcMockSchool2);

    // Upload file for second sdcSchoolCollection
    final FileInputStream fis2 = new FileInputStream("src/test/resources/sample-2-student.txt");
    final String fileContents2 = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis2));
    assertThat(fileContents2).isNotEmpty();
    val body2 = SdcFileUpload.builder().fileContents(fileContents2).createUser("DEF").fileName("SampleUpload2.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection2.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body2))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    // Verify results for second sdcSchoolCollection
    var result2 = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result2).hasSize(2);
    var entity2 = result2.get(1); // Assuming the second entity is at index 1
    assertThat(entity2.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity2.getUploadFileName()).isEqualTo("SampleUpload2.std");
    assertThat(entity2.getUploadReportDate()).isNotNull();
    assertThat(entity2.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");

    // Get file summary for second sdcSchoolCollection
    var resultActions2 = this.mockMvc.perform(get(BASE_URL + "/" + sdcSchoolCollection2.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body2))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    val summary2 = objectMapper.readValue(resultActions2.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SdcFileSummary>() {});
    assertThat(summary2.getFileName()).isEqualTo(body2.getFileName());
    assertThat(summary2.getUploadReportDate()).isNotNull();
    assertThat(summary2.getPositionInQueue()).isEqualTo("2");
  }
}
