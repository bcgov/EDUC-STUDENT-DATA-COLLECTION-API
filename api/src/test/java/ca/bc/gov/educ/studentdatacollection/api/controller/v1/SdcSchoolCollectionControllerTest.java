package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.struct.v1.Condition.AND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SdcSchoolCollectionControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  CollectionRepository collectionRepository;

  @Autowired
  RestUtils restUtils;

  @Autowired
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Autowired
  SdcSchoolCollectionStudentEnrolledProgramRepository sdcSchoolCollectionStudentEnrolledProgramRepository;

  @Autowired
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  @Autowired
  SdcDuplicateRepository sdcDuplicateRepository;
  protected static final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @AfterEach
  public void after() {
    this.sdcDuplicateRepository.deleteAll();
    this.sdcSchoolCollectionStudentRepository.deleteAll();
    this.sdcSchoolCollectionRepository.deleteAll();
    this.sdcDistrictCollectionRepository.deleteAll();
    this.collectionRepository.deleteAll();
  }

  @Test
  void testGetCollectionByID_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
        get(URL.BASE_URL_SCHOOL_COLLECTION + "/" + sdcMockSchool.getSdcSchoolCollectionID()).with(mockAuthority))
      .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testGetDuplicateByID_ShouldReturnDuplicate() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID2 = sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID2);

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2));

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    //Same district, won't be provincial dupe
    var inDistDupe = createMockSdcDuplicateEntity(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2, collection.getCollectionID());
    var savedDupe = sdcDuplicateRepository.save(inDistDupe);

    this.mockMvc.perform(
                    get(URL.BASE_URL_DUPLICATE + "/" + savedDupe.getSdcDuplicateID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk()).andExpect(
                    MockMvcResultMatchers.jsonPath("$.sdcDuplicateID",
                            equalTo(savedDupe.getSdcDuplicateID().toString())));
  }

  @Test
  void testGetDuplicateByID_ShouldReturnNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(
                    get(URL.BASE_URL_SCHOOL_COLLECTION + "/duplicate/" + UUID.randomUUID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testUpdateCollection_ShouldReturnCollection() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    var mockSchoolCollection = SdcSchoolCollectionMapper.mapper.toSdcSchoolWithStudents(sdcMockSchool);
    mockSchoolCollection.setCreateDate(null);
    mockSchoolCollection.setUpdateDate(null);
    mockSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());

    this.mockMvc.perform(put(URL.BASE_URL_SCHOOL_COLLECTION + "/" + sdcMockSchool.getSdcSchoolCollectionID().toString())
        .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
        .header("correlationID", UUID.randomUUID().toString())
        .content(JsonUtil.getJsonStringFromObject(mockSchoolCollection))
        .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    var updatedSchool = sdcSchoolCollectionRepository.findById(sdcMockSchool.getSdcSchoolCollectionID());
    assertThat(updatedSchool).isPresent();
    assertThat(updatedSchool.get().getSdcSchoolCollectionStatusCode()).isEqualTo(SdcSchoolCollectionStatus.NEW.getCode());
  }

  @Test
  void testUpdateCollection_GivenBadStatus_ShouldReturnBadRequest() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    var mockSchool = SdcSchoolCollectionMapper.mapper.toSdcSchoolWithStudents(sdcMockSchool);
    mockSchool.setCreateDate(null);
    mockSchool.setUpdateDate(null);
    mockSchool.setSdcSchoolCollectionStatusCode("ABC");

    this.mockMvc.perform(put(URL.BASE_URL_SCHOOL_COLLECTION + "/" + sdcMockSchool.getSdcSchoolCollectionID().toString())
        .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
        .header("correlationID", UUID.randomUUID().toString())
        .content(JsonUtil.getJsonStringFromObject(mockSchool))
        .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());

    var oldSchool = sdcSchoolCollectionRepository.findById(sdcMockSchool.getSdcSchoolCollectionID());
    assertThat(oldSchool).isPresent();
    assertThat(oldSchool.get().getSdcSchoolCollectionStatusCode()).isEqualTo(sdcMockSchool.getSdcSchoolCollectionStatusCode());
  }

  @Test
  void testGetCollectionByID_WithWrongID_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
        get(URL.BASE_URL_SCHOOL_COLLECTION + "/" + UUID.randomUUID()).with(mockAuthority))
      .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testGetCollectionBySchoolID_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + schoolTombstone.getSchoolId()).with(mockAuthority))
      .andDo(print()).andExpect(status().isOk()).andExpect(
          MockMvcResultMatchers.jsonPath("$.sdcSchoolCollectionID",
            equalTo(sdcMockSchool.getSdcSchoolCollectionID().toString())));
  }

  @Test
  void testGetAllCollectionsBySchoolID_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    CollectionEntity collection2 = createMockCollectionEntity();
    collection2.setCloseDate(LocalDateTime.now().minusDays(5));
    collectionRepository.save(collection2);

    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection2, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool2);

    this.mockMvc.perform(
        get(URL.BASE_URL_SCHOOL_COLLECTION + "/searchAll?schoolID=" + schoolTombstone.getSchoolId()).with(mockAuthority))
      .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testGetAllCollectionsBySdcDistrictCollectionID_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity sdcMockDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcMockDistrictCollection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrictCollection.getSdcDistrictCollectionID());
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    SchoolTombstone schoolTombstone2 = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone2.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setSdcDistrictCollectionID(sdcMockDistrictCollection.getSdcDistrictCollectionID());
    sdcSchoolCollectionRepository.save(sdcMockSchool2);

    this.mockMvc.perform(
                    get(URL.BASE_URL_SCHOOL_COLLECTION + "/searchAll?sdcDistrictCollectionID=" + sdcMockDistrictCollection.getSdcDistrictCollectionID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  void testGetAllCollectionsWithInvalidParam_ShouldThrowException() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(
                    get(URL.BASE_URL_SCHOOL_COLLECTION + "/searchAll?userID=12345").with(mockAuthority))
            .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testGetCollectionBySchoolID_withSameSchoolInPastCollection_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity pastCollection = createMockCollectionEntity();
    pastCollection.setCollectionStatusCode(CollectionStatus.COMPLETED.getCode());
    pastCollection.setOpenDate(LocalDateTime.now().minusDays(5));
    pastCollection.setCloseDate(LocalDateTime.now().minusDays(2));
    collectionRepository.save(pastCollection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcPastMockSchool = createMockSdcSchoolCollectionEntity(pastCollection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcPastMockSchool.setUploadDate(null);
    sdcPastMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcPastMockSchool);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCollectionStatusCode(CollectionStatus.INPROGRESS.getCode());
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + schoolTombstone.getSchoolId()).with(mockAuthority))
      .andDo(print()).andExpect(status().isOk()).andExpect(
          MockMvcResultMatchers.jsonPath("$.sdcSchoolCollectionID",
            equalTo(sdcMockSchool.getSdcSchoolCollectionID().toString())));
  }

  @Test
  void testGetCollectionBySchoolID_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCollectionStatusCode(CollectionStatus.COMPLETED.getCode());
    collection.setOpenDate(LocalDateTime.now().minusDays(5));
    collection.setCloseDate(LocalDateTime.now().minusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + schoolTombstone.getSchoolId()).with(mockAuthority))
      .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testGetCollectionBySchoolID_ShouldReturnSchoolCollectionStatus() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + schoolTombstone.getSchoolId()).with(mockAuthority))
      .andDo(print()).andExpect(status().isOk()).andExpect(
          MockMvcResultMatchers.jsonPath("$.sdcSchoolCollectionStatusCode",
            equalTo("NEW")));
  }

  @Test
  void testGetCollectionBySchoolID_ShouldReturnCollectionTypeCode() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + schoolTombstone.getSchoolId()).with(mockAuthority))
      .andDo(print()).andExpect(status().isOk()).andExpect(
          MockMvcResultMatchers.jsonPath("$.collectionTypeCode",
            equalTo("SEPTEMBER")));
  }

  @Test
  void testGetCollectionBySchoolID_WithWrongScope_ShouldReturnStatusForbidden() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + schoolTombstone.getSchoolId()).with(mockAuthority))
      .andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  void testCreateSdcSchoolCollection_WithValidPayloadCollectionID_ShouldReturnStatusOkWithData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
        grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
        newCollectionEntity, UUID.randomUUID());
    sdcMockSchool.setCreateDate(null);
    sdcMockSchool.setUpdateDate(null);

    this.mockMvc.perform(
        post(URL.BASE_URL_SCHOOL_COLLECTION + "/" + newCollectionEntity.getCollectionID()).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(SdcSchoolCollectionMapper.mapper.toSdcSchoolWithStudents(sdcMockSchool)))
        .with(mockAuthority))
      .andDo(print()).andExpect(status().isCreated()).andExpect(
          MockMvcResultMatchers.jsonPath("$.collectionID").value(newCollectionEntity.getCollectionID().toString()));
  }

  @Test
  void testCreateSdcSchoolCollectionWithStudent_WithValidPayloadCollectionID_ShouldReturnStatusOkWithData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
        grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
        newCollectionEntity, UUID.randomUUID());
    sdcMockSchool.setCreateDate(null);
    sdcMockSchool.setUpdateDate(null);
    sdcMockSchool.getSDCSchoolStudentEntities().add(createMockSchoolStudentEntity(null));

    this.mockMvc.perform(
        post(URL.BASE_URL_SCHOOL_COLLECTION + "/" + newCollectionEntity.getCollectionID()).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(SdcSchoolCollectionMapper.mapper.toSdcSchoolWithStudents(sdcMockSchool)))
        .with(mockAuthority))
      .andDo(print()).andExpect(status().isCreated()).andExpect(
          MockMvcResultMatchers.jsonPath("$.collectionID").value(newCollectionEntity.getCollectionID().toString()));
  }

  @Test
  void testCreateSdcSchoolCollectionWithStudentAndProgramCodes_WithValidPayloadCollectionID_ShouldReturnStatusOkWithData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
        grantedAuthority);
    String programCodes = "0987654321";
    Integer numberOfCodes = TransformUtil.splitIntoChunks(programCodes, 2).size();

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
        newCollectionEntity, UUID.randomUUID());
    sdcMockSchool.setCreateDate(null);
    sdcMockSchool.setUpdateDate(null);
    SdcSchoolCollectionStudentEntity student = createMockSchoolStudentEntity(null);
    student.setEnrolledProgramCodes(programCodes);
    sdcMockSchool.getSDCSchoolStudentEntities().add(student);

    this.mockMvc.perform(
        post(URL.BASE_URL_SCHOOL_COLLECTION + "/" + newCollectionEntity.getCollectionID()).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(SdcSchoolCollectionMapper.mapper.toSdcSchoolWithStudents(sdcMockSchool)))
        .with(mockAuthority))
      .andDo(print()).andExpect(status().isCreated());

    List<SdcSchoolCollectionStudentEnrolledProgramEntity> codes =
      this.sdcSchoolCollectionStudentEnrolledProgramRepository.findAll();
    SdcSchoolCollectionStudentEnrolledProgramEntity firstCode = codes.get(0);

    assertThat(!CollectionUtils.isEmpty(codes)).isTrue();
    assertThat(codes).hasSize(numberOfCodes);
    assertThat(programCodes).contains(firstCode.getEnrolledProgramCode());
  }

  @Test
  void testCreateSdcSchoolCollectionWithStudentAndValidationError_WithValidPayloadCollectionID_ShouldReturnStatusOkWithData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
        grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
        newCollectionEntity, UUID.randomUUID());
    sdcMockSchool.setCreateDate(null);
    sdcMockSchool.setUpdateDate(null);
    var student = createMockSchoolStudentEntity(null);
    student.getSDCStudentValidationIssueEntities()
      .add(createMockSdcSchoolCollectionStudentValidationIssueEntity(null, StudentValidationIssueSeverityCode.ERROR));
    sdcMockSchool.getSDCSchoolStudentEntities().add(student);

    this.mockMvc.perform(
        post(URL.BASE_URL_SCHOOL_COLLECTION + "/" + newCollectionEntity.getCollectionID()).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(SdcSchoolCollectionMapper.mapper.toSdcSchoolWithStudents(sdcMockSchool)))
        .with(mockAuthority))
      .andDo(print()).andExpect(status().isCreated()).andExpect(
          MockMvcResultMatchers.jsonPath("$.collectionID").value(newCollectionEntity.getCollectionID().toString()));
  }

  @Test
  void testCreateSdcSchoolCollection_WithInvalidCollectionIDUrl_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
        grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
        newCollectionEntity, UUID.randomUUID());
    sdcMockSchool.setCreateDate(null);
    sdcMockSchool.setUpdateDate(null);

    this.mockMvc.perform(
        post(URL.BASE_URL_SCHOOL_COLLECTION + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(SdcSchoolCollectionMapper.mapper.toSdcSchoolWithStudents(sdcMockSchool)))
        .with(mockAuthority))
      .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testCreateAndDeleteSdcSchoolCollection_WithValidPayloadCollectionID_ShouldReturnStatusOkWithData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
        grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(
        newCollectionEntity, UUID.randomUUID());
    sdcMockSchool.setCreateDate(null);
    sdcMockSchool.setUpdateDate(null);

    this.mockMvc.perform(
        post(URL.BASE_URL_SCHOOL_COLLECTION + "/" + newCollectionEntity.getCollectionID()).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(SdcSchoolCollectionMapper.mapper.toSdcSchoolWithStudents(sdcMockSchool)))
        .with(mockAuthority))
      .andDo(print()).andExpect(status().isCreated()).andExpect(
          MockMvcResultMatchers.jsonPath("$.collectionID").value(newCollectionEntity.getCollectionID().toString()));

    var sdcSchoolCollection = sdcSchoolCollectionRepository.findActiveCollectionBySchoolId(sdcMockSchool.getSchoolID());

    final GrantedAuthority grantedAuthority1 = () -> "SCOPE_DELETE_SDC_SCHOOL_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority1 = oidcLogin().authorities(
        grantedAuthority1);

    this.mockMvc.perform(
        delete(URL.BASE_URL_SCHOOL_COLLECTION + "/" + sdcSchoolCollection.get().getSdcSchoolCollectionID()).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .with(mockAuthority1))
      .andDo(print()).andExpect(status().isNoContent());

    var sdcSchoolCollectionDelete = sdcSchoolCollectionRepository.findById(sdcSchoolCollection.get().getSdcSchoolCollectionID());
    assertThat(sdcSchoolCollectionDelete).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "WRITE_SDC_COLLECTION",
    "FAKE_SCOPE"
  })
  void testUnsubmitCollection_GivenDoesntHaveBothScopes_ShouldThrowException(String scope) throws Exception {
    var sdcSchoolCollectionID = UUID.randomUUID();

    this.mockMvc.perform(post(URL.BASE_URL_SCHOOL_COLLECTION + "/unsubmit")
            .with(jwt().jwt(jwt -> jwt.claim("scope", scope)))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(SdcSchoolCollection.builder().sdcSchoolCollectionID(String.valueOf(sdcSchoolCollectionID)).build()))
            .contentType(APPLICATION_JSON)).andExpect(status().isForbidden());
  }

  @Test
  void testUnsubmitCollection_GivenDistrictState_ShouldUpdateCorrectly() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();

    SchoolTombstone schoolTombstone = createMockSchool();
    schoolTombstone.setDistrictId(district.getDistrictId());

    SdcDistrictCollectionEntity sdcMockDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcMockDistrictCollection.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.REVIEWED.getCode());
    sdcDistrictCollectionRepository.save(sdcMockDistrictCollection);

    SdcSchoolCollectionEntity sdcMockSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchoolCollection.setSdcDistrictCollectionID(sdcMockDistrictCollection.getSdcDistrictCollectionID());
    sdcMockSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());
    sdcSchoolCollectionRepository.save(sdcMockSchoolCollection);

    var payload = UnsubmitSdcSchoolCollection.builder().sdcSchoolCollectionID(sdcMockSchoolCollection.getSdcSchoolCollectionID()).updateUser("USER").build();

    this.mockMvc.perform(post(URL.BASE_URL_SCHOOL_COLLECTION + "/unsubmit")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_DISTRICT_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(payload))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    var updatedSchoolCollection = sdcSchoolCollectionRepository.findById(sdcMockSchoolCollection.getSdcSchoolCollectionID());
    var updatedDistrictCollection = sdcDistrictCollectionRepository.findById(sdcMockDistrictCollection.getSdcDistrictCollectionID());
    assertThat(updatedSchoolCollection).isPresent();
    assertThat(updatedSchoolCollection.get().getSdcSchoolCollectionStatusCode()).isEqualTo(SdcSchoolCollectionStatus.DUP_VRFD.getCode());
    assertThat(updatedDistrictCollection).isPresent();
    assertThat(updatedDistrictCollection.get().getSdcDistrictCollectionStatusCode()).isEqualTo(SdcDistrictCollectionStatus.LOADED.getCode());
  }

  @Test
  void testStartCollectionFromLastSept_ShouldReturnOk() throws Exception {
    CollectionEntity oldCollection = createMockCollectionEntity();
    oldCollection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
    oldCollection.setCloseDate(LocalDateTime.now().minusDays(2));
    oldCollection.setSnapshotDate(LocalDate.now().minusDays(20));
    collectionRepository.save(oldCollection);

    District district = createMockDistrict();

    SchoolTombstone schoolTombstone = createMockSchool();
    schoolTombstone.setDistrictId(district.getDistrictId());

    SdcDistrictCollectionEntity sdcMockDistrictCollection = createMockSdcDistrictCollectionEntity(oldCollection, UUID.fromString(district.getDistrictId()));
    sdcMockDistrictCollection.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.COMPLETED.getCode());
    sdcDistrictCollectionRepository.save(sdcMockDistrictCollection);

    SdcSchoolCollectionEntity sdcMockSchoolCollection = createMockSdcSchoolCollectionEntity(oldCollection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchoolCollection.setSdcDistrictCollectionID(sdcMockDistrictCollection.getSdcDistrictCollectionID());
    sdcMockSchoolCollection.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.COMPLETED.getCode());
    var savedOldColl = sdcSchoolCollectionRepository.save(sdcMockSchoolCollection);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district2 = createMockDistrict();

    SchoolTombstone schoolTombstone2 = createMockSchool();
    schoolTombstone2.setDistrictId(district2.getDistrictId());

    SdcDistrictCollectionEntity sdcMockDistrictCollection2 = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district2.getDistrictId()));
    sdcMockDistrictCollection2.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.REVIEWED.getCode());
    sdcDistrictCollectionRepository.save(sdcMockDistrictCollection2);

    SdcSchoolCollectionEntity sdcMockSchoolCollection2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchoolCollection2.setSdcDistrictCollectionID(sdcMockDistrictCollection2.getSdcDistrictCollectionID());
    sdcMockSchoolCollection2.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());
    var savedColl = sdcSchoolCollectionRepository.save(sdcMockSchoolCollection2);

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(savedOldColl);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(savedOldColl);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var startFromPrior = new StartFromPriorSdcSchoolCollection();
    startFromPrior.setSdcSchoolCollectionID(savedColl.getSdcSchoolCollectionID());
    startFromPrior.setUpdateUser("ABC");

    this.mockMvc.perform(post(URL.BASE_URL_SCHOOL_COLLECTION + "/priorCollection")
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_SDC_SCHOOL_COLLECTION")))
            .content(JsonUtil.getJsonStringFromObject(startFromPrior))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    var updatedStudents = sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollection_SdcSchoolCollectionID(savedColl.getSdcSchoolCollectionID());

    assertThat(updatedStudents.get(0).getSdcSchoolCollectionStudentStatusCode()).isEqualTo("LOADED");
  }

  @Test
  void testGetSchoolCollectionProvincialDuplicates() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID2 = sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school1.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollection1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollection1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    var sdcSchoolCollectionEntity1 = sdcSchoolCollectionRepository.save(sdcSchoolCollection1);

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID2);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    var assignedStudentID = UUID.randomUUID();
    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    sdcSchoolCollectionStudent1.setAssignedStudentId(assignedStudentID);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudent2.setAssignedStudentId(assignedStudentID);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school1));

    this.mockMvc.perform(get(URL.BASE_URL_DUPLICATE + "/sdcSchoolCollection/" + sdcSchoolCollectionEntity1.getSdcSchoolCollectionID() + "/provincial-duplicates")
                    .with(mockAuthority)
                    .header("correlationID", UUID.randomUUID().toString())
                    .contentType(APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(1)));
  }

  @Test
  void testGetSchoolCollectionProvincialDuplicatesWithHS() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID2 = sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school1.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollection1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollection1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    var sdcSchoolCollectionEntity1 = sdcSchoolCollectionRepository.save(sdcSchoolCollection1);

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID2);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    var assignedStudentID = UUID.randomUUID();
    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    sdcSchoolCollectionStudent1.setAssignedStudentId(assignedStudentID);
    sdcSchoolCollectionStudent1.setEnrolledGradeCode(SchoolGradeCodes.HOMESCHOOL.getCode());
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudent2.setAssignedStudentId(assignedStudentID);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school1));

    var resultActions1 = this.mockMvc.perform(get(URL.BASE_URL_DUPLICATE + "/sdcSchoolCollection/" + sdcSchoolCollectionEntity1.getSdcSchoolCollectionID() + "/provincial-duplicates")
                    .with(mockAuthority)
                    .header("correlationID", UUID.randomUUID().toString())
                    .contentType(APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(1)));

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(summary1).hasSize(1);
    assertThat(summary1.get(0).getDuplicateErrorDescriptionCode()).isEqualTo(DuplicateErrorDescriptionCode.HS_DUP.getMessage());
  }

  @Test
  void testGetSchoolCollectionProvincialDuplicatesWithHSShouldNotProduceDupe() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID2 = sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school1.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollection1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollection1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    var sdcSchoolCollectionEntity1 = sdcSchoolCollectionRepository.save(sdcSchoolCollection1);

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    school2.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID2);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    var assignedStudentID = UUID.randomUUID();
    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    sdcSchoolCollectionStudent1.setAssignedStudentId(assignedStudentID);
    sdcSchoolCollectionStudent1.setEnrolledGradeCode(SchoolGradeCodes.HOMESCHOOL.getCode());
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudent2.setAssignedStudentId(assignedStudentID);
    sdcSchoolCollectionStudent2.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    this.mockMvc.perform(get(URL.BASE_URL_DUPLICATE + "/sdcSchoolCollection/" + sdcSchoolCollectionEntity1.getSdcSchoolCollectionID() + "/provincial-duplicates")
                    .with(mockAuthority)
                    .header("correlationID", UUID.randomUUID().toString())
                    .contentType(APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(0)));
  }

  @Test
  void testReadSdcSchoolCollectionPaginated_withValidPayload_ShouldReturnStatusOk() throws Exception {
    var collection = collectionRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("collectionEntity.collectionTypeCode").operation(FilterOperation.EQUAL).value(collection.getCollectionTypeCode()).valueType(ValueType.STRING).build();

    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());

    final var objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION+URL.PAGINATED)
                    .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_COLLECTION")))
                    .param("searchCriteriaList", criteriaJSON)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }
}
