package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDistrictCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicate;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SpecialEducationCategoryCode;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SdcDistrictCollectionControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  SdcDistrictCollectionController sdcDistrictCollectionController;

  @Autowired
  CollectionRepository collectionRepository;

  @Autowired
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  @Autowired
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Autowired
  SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  protected final static ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @Autowired
  RestUtils restUtils;

  @BeforeEach
  public void before() {
  }

  @AfterEach
  public void after() {
    this.collectionRepository.deleteAll();
  }

  @Test
  void testGetActiveDistrictCollectionByDistrictId_GivenNoSdcDistrictCollectionForDistrict_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collectionRepository.save(collection);

    District district = createMockDistrict();
    var mockCompletedSdcDistrictInSameDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    mockCompletedSdcDistrictInSameDistrict.setSdcDistrictCollectionStatusCode("COMPLETED");
    sdcDistrictCollectionRepository.save(mockCompletedSdcDistrictInSameDistrict);

    District district2 = createMockDistrict();
    var mockNewSdcDistrictInDifferentDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district2.getDistrictId()));
    sdcDistrictCollectionRepository.save(mockNewSdcDistrictInDifferentDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/search/" + district.getDistrictId()).with(mockAuthority))
            .andDo(print()).andExpect(status().isNotFound());
  }
  @Test
  void testGetActiveDistrictCollectionByDistrictId_withSameDistrictInPastCollection_ShouldReturnOneDistrictCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity pastCollection = createMockCollectionEntity();
    pastCollection.setOpenDate(LocalDateTime.now().minusDays(5));
    pastCollection.setCloseDate(LocalDateTime.now().minusDays(2));
    collectionRepository.save(pastCollection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity sdcPastMockDistrict = createMockSdcDistrictCollectionEntity(pastCollection, UUID.fromString(district.getDistrictId()));
    sdcPastMockDistrict.setSdcDistrictCollectionStatusCode("COMPLETED");
    sdcDistrictCollectionRepository.save(sdcPastMockDistrict);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/search/" + district.getDistrictId()).with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.sdcDistrictCollectionID", equalTo(sdcMockDistrict.getSdcDistrictCollectionID().toString())));
  }

  @Test
  void testGetActiveDistrictCollectionByDistrictId_WithWrongScope_ShouldReturnStatusForbidden() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/search/" + district.getDistrictId()).with(mockAuthority))
            .andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_K9Check_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(1);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.NON_ALLOWABLE.getCode());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_AdultCheckSpedCareer_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setIsAdult(true);
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(2);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.ALLOWABLE.getCode());

    assertThat(sdcDuplicates.get(1).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(1).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.PROGRAM.getCode());
    assertThat(sdcDuplicates.get(1).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.NON_ALLOWABLE.getCode());
    assertThat(sdcDuplicates.get(1).getProgramDuplicateTypeCode()).isEqualTo(ProgramDuplicateTypeCode.SPECIAL_ED.getCode());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_K710SUCheck_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE05.getCode());
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(1);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.NON_ALLOWABLE.getCode());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_Indy10SUCheck_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school1.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    school1.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student1.setSpecialEducationCategoryCode(null);
    student1.setCareerProgramCode("YX");
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(1);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.ALLOWABLE.getCode());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_Indy10SUCheckIsIndy_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school1.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school2.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student1.setSpecialEducationCategoryCode(null);
    student1.setCareerProgramCode("YX");
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(1);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.ALLOWABLE.getCode());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_Indy10SUCheckIsNotIndy_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school1.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student1.setSpecialEducationCategoryCode(null);
    student1.setCareerProgramCode("YX");
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(1);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.NON_ALLOWABLE.getCode());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_Indy10SUCheckSameDistAltProgs_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school1.setFacilityTypeCode(FacilityTypeCodes.ALT_PROGS.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student1.setSpecialEducationCategoryCode(null);
    student1.setCareerProgramCode("YX");
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(1);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.NON_ALLOWABLE.getCode());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_Indy10SUCheckSameDistNoAltProgs_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student1.setSpecialEducationCategoryCode(null);
    student1.setCareerProgramCode("YX");
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(1);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.ALLOWABLE.getCode());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_89Check_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student1.setSpecialEducationCategoryCode(null);
    student1.setCareerProgramCode("YX");
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(1);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.NON_ALLOWABLE.getCode());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_89CheckDistLearn_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school1.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school2.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student1.setSpecialEducationCategoryCode(null);
    student1.setCareerProgramCode("YX");
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(1);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.ALLOWABLE.getCode());
  }

  @Test
  void testGetAllInDistrictDuplicatesBySdcDistrictCollectionID_89StandardCheck_ShouldReturnDups() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    School school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school1.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity1);

    School school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    school2.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity2);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    var studentID = UUID.randomUUID();
    var student1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student1.setSpecialEducationCategoryCode(null);
    student1.setCareerProgramCode("YX");
    student1.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student1);
    var student2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setAssignedStudentId(studentID);
    sdcSchoolCollectionStudentRepository.save(student2);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcDistrictCollectionID + "/in-district-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val sdcDuplicates = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<SdcDuplicate>>() {
    });

    assertThat(sdcDuplicates).hasSize(1);
    assertThat(sdcDuplicates.get(0).getDuplicateLevelCode()).isEqualTo(DuplicateLevelCode.IN_DIST.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateTypeCode()).isEqualTo(DuplicateTypeCode.ENROLLMENT.getCode());
    assertThat(sdcDuplicates.get(0).getDuplicateSeverityCode()).isEqualTo(DuplicateSeverityCode.ALLOWABLE.getCode());
  }

  @Test
  void testGetSdcDistrictCollection_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + sdcMockDistrict.getSdcDistrictCollectionID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.sdcDistrictCollectionID", equalTo(sdcMockDistrict.getSdcDistrictCollectionID().toString())));
  }
  @Test
  void testGetSdcDistrictCollection_GivenDistrictDoesNotExist_ShouldEntityNotFoundException() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + UUID.randomUUID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testCreateSdcDistrictCollection_WithValidPayloadCollectionID_ShouldReturnStatusOkWithData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(newCollectionEntity, UUID.randomUUID());
    sdcMockDistrict.setCreateDate(null);
    sdcMockDistrict.setUpdateDate(null);

    this.mockMvc.perform(
      post(URL.BASE_URL_DISTRICT_COLLECTION + "/" + newCollectionEntity.getCollectionID()).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(SdcDistrictCollectionMapper.mapper.toStructure(sdcMockDistrict)))
        .with(mockAuthority))
        .andDo(print()).andExpect(status().isCreated()).andExpect(
          MockMvcResultMatchers.jsonPath("$.collectionID").value(newCollectionEntity.getCollectionID().toString()));
  }

  @Test
  void testCreateSdcDistrictCollection_WithInvalidCollectionID_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(newCollectionEntity, UUID.randomUUID());
    sdcMockDistrict.setCreateDate(null);
    sdcMockDistrict.setUpdateDate(null);

    this.mockMvc.perform(
      post(URL.BASE_URL_DISTRICT_COLLECTION + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(SdcDistrictCollectionMapper.mapper.toStructure(sdcMockDistrict)))
        .with(mockAuthority))
        .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testCreateSdcDistrictCollection_WithInvalidPayload_ShouldReturnStatusBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(newCollectionEntity, UUID.randomUUID());
    sdcMockDistrict.setSdcDistrictCollectionID(UUID.randomUUID());
    sdcMockDistrict.setCreateDate(null);
    sdcMockDistrict.setUpdateDate(null);

    this.mockMvc.perform(
      post(URL.BASE_URL_DISTRICT_COLLECTION + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(asJsonString(SdcDistrictCollectionMapper.mapper.toStructure(sdcMockDistrict)))
        .with(mockAuthority))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].message").value("sdcDistrictCollectionID should be null for post operation."));
  }

  @Test
  void testCreateSdcDistrictCollection_WithInvalidStatusCode_ShouldReturnStatusBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(newCollectionEntity, UUID.randomUUID());
    sdcMockDistrict.setSdcDistrictCollectionStatusCode("INVALID");
    sdcMockDistrict.setCreateDate(null);
    sdcMockDistrict.setUpdateDate(null);

    this.mockMvc.perform(
                    post(URL.BASE_URL_DISTRICT_COLLECTION + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(asJsonString(SdcDistrictCollectionMapper.mapper.toStructure(sdcMockDistrict)))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].message").value("Invalid SDC district collection status code."));
  }

  @Test
  void testCreateSdcDistrictCollection_WithBadId_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_DELETE_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(newCollectionEntity, UUID.randomUUID());
    var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
      delete(URL.BASE_URL_DISTRICT_COLLECTION + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .content(asJsonString(SdcDistrictCollectionMapper.mapper.toStructure(sdcMockDistrict)))
      .with(mockAuthority))
      .andDo(print())
      .andExpect(status().isNotFound());

    var deletedEntity = sdcDistrictCollectionRepository.findById(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    assertThat(deletedEntity).isNotEmpty();
  }

  @Test
  void testCreateSdcDistrictCollection_WithValidId_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_DELETE_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity newCollectionEntity = collectionRepository.save(createMockCollectionEntity());

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(newCollectionEntity, UUID.randomUUID());
    var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    delete(URL.BASE_URL_DISTRICT_COLLECTION + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID()).contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(asJsonString(SdcDistrictCollectionMapper.mapper.toStructure(sdcMockDistrict)))
                            .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isNoContent());

    var deletedEntity = sdcDistrictCollectionRepository.findById(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    assertThat(deletedEntity).isEmpty();
  }

  @Test
  void testGetMonitorSdcSchoolCollectionResponse_WithInValidId_ReturnsNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + UUID.randomUUID() + "/monitorSdcSchoolCollections").with(mockAuthority)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testGetMonitorSdcSchoolCollectionResponse_WithValidPayload_ReturnsCorrectResponse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    var districtID = UUID.randomUUID();
    var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

    var school1 = createMockSchool();
    school1.setDisplayName("School1");
    school1.setMincode("0000001");
    school1.setDistrictId(districtID.toString());
    var school2 = createMockSchool();
    school2.setDisplayName("School2");
    school2.setMincode("0000002");
    school2.setDistrictId(districtID.toString());

    var sdcSchoolCollection1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollection1.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    sdcSchoolCollection1.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());
    var sdcSchoolCollection2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollection2.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    sdcSchoolCollection2.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SCH_C_VRFD.getCode());
    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollection1, sdcSchoolCollection2));

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    this.mockMvc.perform(get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID() + "/monitorSdcSchoolCollections").with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].sdcSchoolCollectionId").value(sdcSchoolCollection1.getSdcSchoolCollectionID().toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].schoolTitle").value("0000001 - School1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].errors").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].fundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].infoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].detailsConfirmed").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].contactsConfirmed").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].submittedToDistrict").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].sdcSchoolCollectionId").value(sdcSchoolCollection2.getSdcSchoolCollectionID().toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].schoolTitle").value("0000002 - School2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].errors").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].fundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].infoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].detailsConfirmed").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].contactsConfirmed").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].submittedToDistrict").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsWithData").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalErrors").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalFundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalInfoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsDetailsConfirmed").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsContactsConfirmed").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsSubmitted").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalSchools").value(2));
  }

  @Test
  void testGetMonitorSdcSchoolCollectionResponse_WithValidDistrictCollectionIdNoSchoolCollections_ReturnsCorrectResponse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_DISTRICT_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, UUID.randomUUID()));


    this.mockMvc.perform(get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID() + "/monitorSdcSchoolCollections").with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections", hasSize(0)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsWithData").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalErrors").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalFundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalInfoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsDetailsConfirmed").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsContactsConfirmed").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsSubmitted").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalSchools").value(0));
  }

  @Test
  void testUpdateDistrictCollection_ShouldReturnCollection() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity mockSdcDistrictCollectionEntity = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(mockSdcDistrictCollectionEntity);

    var mockSdcDistrictCollection = SdcDistrictCollectionMapper.mapper.toStructure(mockSdcDistrictCollectionEntity);
    mockSdcDistrictCollection.setCreateDate(null);
    mockSdcDistrictCollection.setUpdateDate(null);
    mockSdcDistrictCollection.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.REVIEWED.getCode());

    this.mockMvc.perform(put(URL.BASE_URL_DISTRICT_COLLECTION + "/" + mockSdcDistrictCollectionEntity.getSdcDistrictCollectionID().toString())
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_DISTRICT_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(mockSdcDistrictCollection))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    var updatedDistrict = sdcDistrictCollectionRepository.findById(mockSdcDistrictCollectionEntity.getSdcDistrictCollectionID());
    assertThat(updatedDistrict).isPresent();
    assertThat(updatedDistrict.get().getSdcDistrictCollectionStatusCode()).isEqualTo(SdcDistrictCollectionStatus.REVIEWED.getCode());
  }

  @Test
  void testUpdateDistrictCollection_GivenBadStatus_ShouldReturnBadRequest() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity mockSdcDistrictCollectionEntity = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(mockSdcDistrictCollectionEntity);

    var mockSdcDistrictCollection = SdcDistrictCollectionMapper.mapper.toStructure(mockSdcDistrictCollectionEntity);
    mockSdcDistrictCollection.setCreateDate(null);
    mockSdcDistrictCollection.setUpdateDate(null);
    mockSdcDistrictCollection.setSdcDistrictCollectionStatusCode("BAD_STATUS");

    this.mockMvc.perform(put(URL.BASE_URL_DISTRICT_COLLECTION + "/" + mockSdcDistrictCollectionEntity.getSdcDistrictCollectionID().toString())
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_DISTRICT_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString())
            .content(JsonUtil.getJsonStringFromObject(mockSdcDistrictCollection))
            .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());

    var originalDistrict = sdcDistrictCollectionRepository.findById(mockSdcDistrictCollectionEntity.getSdcDistrictCollectionID());
    assertThat(originalDistrict).isPresent();
    assertThat(originalDistrict.get().getSdcDistrictCollectionStatusCode()).isEqualTo(mockSdcDistrictCollectionEntity.getSdcDistrictCollectionStatusCode());
  }

  @Test
  void testGetAllSchoolCollectionsForDistrict_shouldReturnSetOfFileSummaries() throws Exception {
    CollectionEntity collection = createMockCollectionEntity();
    collectionRepository.save(collection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity mockSdcDistrictCollectionEntity = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(mockSdcDistrictCollectionEntity);

    School school1 = createMockSchool();
    school1.setDistrictId(district.getDistrictId());
    School school2 = createMockSchool();
    school2.setDistrictId(district.getDistrictId());

    LocalDateTime uploadDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

    SdcSchoolCollectionEntity schoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    schoolCollectionEntity1.setSdcDistrictCollectionID(mockSdcDistrictCollectionEntity.getSdcDistrictCollectionID());
    schoolCollectionEntity1.setSdcSchoolCollectionStatusCode("NEW");
    schoolCollectionEntity1.setUploadDate(uploadDate);
    sdcSchoolCollectionRepository.save(schoolCollectionEntity1);

    SdcSchoolCollectionEntity schoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    schoolCollectionEntity2.setSdcDistrictCollectionID(mockSdcDistrictCollectionEntity.getSdcDistrictCollectionID());
    schoolCollectionEntity2.setSdcSchoolCollectionStatusCode("LOADED");
    sdcSchoolCollectionRepository.save(schoolCollectionEntity2);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(schoolCollectionEntity1);
    student1.setSdcSchoolCollectionStudentStatusCode("LOADED");
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(schoolCollectionEntity1);
    student2.setSdcSchoolCollectionStudentStatusCode("LOADED");
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(schoolCollectionEntity1);
    student3.setSdcSchoolCollectionStudentStatusCode("VERIFIED");
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(schoolCollectionEntity2);
    student4.setSdcSchoolCollectionStudentStatusCode("VERIFIED");
    sdcSchoolCollectionStudentRepository.save(student4);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    this.mockMvc.perform(get(URL.BASE_URL_DISTRICT_COLLECTION + "/" + mockSdcDistrictCollectionEntity.getSdcDistrictCollectionID().toString() + "/fileProgress")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_DISTRICT_COLLECTION")))
            .header("correlationID", UUID.randomUUID().toString()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].fileName").value(schoolCollectionEntity1.getUploadFileName()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].percentageStudentsProcessed").value("33"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].schoolDisplayName").value(school1.getMincode() + " - " + school1.getDisplayName()))
            .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)));
  }

}
