package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentValidationIssueEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
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

public class SdcFileControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  CollectionRepository sdcRepository;

  @Autowired
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Autowired
  SdcSchoolCollectionStudentRepository schoolStudentRepository;

  @Autowired
  SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;

  @Autowired
  SdcSchoolCollectionStudentValidationIssueRepository sdcSchoolCollectionStudentValidationIssueRepository;

  @Autowired
  RestUtils restUtils;

  @Autowired
  private MockMvc mockMvc;

  protected final static ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  public static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void afterEach() {
    this.schoolStudentRepository.deleteAll();
    this.sdcSchoolCollectionRepository.deleteAll();
    this.sdcRepository.deleteAll();
  }

  @Test
  void testProcessSdcFile_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    final var students = this.schoolStudentRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();
  }

  @Test
  void testProcessSdcFile_givenVerFiletype_ShouldReturnStatusOk() throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    School school = this.createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
      collection,
      UUID.fromString(school.getSchoolId())
    );
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
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
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(verFile))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
  }

  @Test
  void testProcessSdcFile_givenStdFiletype_ShouldReturnStatusOk() throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    School school = this.createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
      collection,
      UUID.fromString(school.getSchoolId())
    );
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository
      .save(sdcMockSchool);

    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    SdcFileUpload stdFile = SdcFileUpload.builder()
      .fileContents(fileContents)
      .createUser("ABC")
      .fileName("SampleUpload.file.std")
      .build();

    String cid = sdcSchoolCollection.getSdcSchoolCollectionID().toString();
    this.mockMvc.perform(post( BASE_URL + "/" + cid + "/file")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(stdFile))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
  }

  @Test
  void testProcessSdcFile_givenInvalidFiletype_ShouldReturnStatusBadRequest() throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    School school = this.createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
      collection,
      UUID.fromString(school.getSchoolId())
    );
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository
      .save(sdcMockSchool);

    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    SdcFileUpload stdFile = SdcFileUpload.builder()
      .fileContents(fileContents)
      .createUser("ABC")
      .fileName("SampleUpload.nope")
      .build();

    String cid = sdcSchoolCollection.getSdcSchoolCollectionID().toString();
    this.mockMvc.perform(post( BASE_URL + "/" + cid + "/file")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(stdFile))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  void testProcessSdcFile_givenMatchingMincode_ShouldReturnStatusOk() throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    School school = this.createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
      collection,
      UUID.fromString(school.getSchoolId())
    );
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository
      .save(sdcMockSchool);

    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    SdcFileUpload stdFile = SdcFileUpload.builder()
      .fileContents(fileContents)
      .createUser("ABC")
      .fileName("SampleUpload.std")
      .build();

    String cid = sdcSchoolCollection.getSdcSchoolCollectionID().toString();
    this.mockMvc.perform(post( BASE_URL + "/" + cid + "/file")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(stdFile))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
  }

  @Test
  void testProcessSdcFile_givenMincodeMismatch_ShouldReturnStatusBadRequest() throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    School school = this.createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
      collection,
      UUID.fromString(school.getSchoolId())
    );
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
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
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(stdFile))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  void testProcessSdcFNCharsFile_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-2-student-fnchars.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    final var students = this.schoolStudentRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();
  }

  @Test
  void testProcessSdcFileReUpload_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    var students = this.schoolStudentRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();

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
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(body))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    assertThat(result).hasSize(1);
    entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    students = this.schoolStudentRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();

    var issues = sdcSchoolCollectionStudentValidationIssueRepository.findAll();
    assertThat(issues).isEmpty();

    var historyStuds2 = this.sdcSchoolCollectionStudentHistoryRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(historyStuds2).isNotNull().hasSize(1);
  }

  @Test
  void testProcessSdcFileCheckProgress_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.std").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");
    final var students = this.schoolStudentRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();

    var resultActions = this.mockMvc.perform(get(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SdcFileSummary>() {
    });
    assertThat(summary.getFileName()).isEqualTo(body.getFileName());
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
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @CsvSource({
    "src/test/resources/sample-malformed-short-header.txt,Header record is missing characters.",
    "src/test/resources/sample-malformed-long-header.txt,Header record has extraneous characters.",
    "src/test/resources/sample-malformed-short-trailer.txt,Trailer record is missing characters.",
    "src/test/resources/sample-malformed-long-trailer.txt,Trailer record has extraneous characters."
  })
  void testProcessSdcFile_givenMalformedRow_ShouldReturnStatusBadRequest(
    final String sample,
    final String errorMessage
  ) throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    School school = this.createMockSchool();

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository
      .save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId())));

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
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
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
  void testProcessSdcFile_givenDetailTooShort_ShouldReturnStatusBadRequest(
    final String sample,
    final String errorExpression
  ) throws Exception {
    CollectionEntity collection = sdcRepository.save(createMockCollectionEntity());
    School school = this.createMockSchool();

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionRepository
      .save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId())));

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
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
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
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
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
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

}
