package ca.bc.gov.educ.studentdatacollection.api.controller;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcBatchStatusCodes;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileSummary;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

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
  RestUtils restUtils;

  @Autowired
  private MockMvc mockMvc;

  protected final static ObjectMapper objectMapper = new ObjectMapper();


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
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.txt").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo(SdcBatchStatusCodes.LOADED.getCode());
    final var students = this.schoolStudentRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();
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
    val body = SdcFileUpload.builder().fileContents(fileContents).createUser("ABC").fileName("SampleUpload.txt").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());
    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);
    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo(SdcBatchStatusCodes.LOADED.getCode());
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
    val body = SdcFileUpload.builder().fileContents(fileContents).fileName("SampleUpload.txt").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
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
    val body = SdcFileUpload.builder().createUser("ABC").fileContents(fileContents).fileName("SampleUpload.txt").build();
    this.mockMvc.perform(post(BASE_URL + "/" + sdcSchoolCollection.getSdcSchoolCollectionID().toString() + "/file")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

}
