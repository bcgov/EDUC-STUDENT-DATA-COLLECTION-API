package ca.bc.gov.educ.studentdatacollection.api.collection.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.controller.v1.SdcSchoolCollectionController;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SdcSchoolCollectionControllerTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    SdcSchoolCollectionController sdcSchoolCollectionController;
    @Autowired
    CollectionRepository collectionRepository;
    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    @BeforeEach
    public void before() {
    }

    @Test
    void testGetCollectionByID_ShouldReturnCollection() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCloseDate(LocalDateTime.now().plusDays(2));
        collectionRepository.save(collection);

        School school = createMockSchool();
        SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchool);

        this.mockMvc.perform(
                        get(URL.BASE_URL_SCHOOL_COLLECTION + "/" + sdcMockSchool.getSdcSchoolCollectionID()).with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());
    }

  @Test
  void testUpdateCollection_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    School school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    var mockSchool = SdcSchoolCollectionMapper.mapper.toSdcSchoolBatch(sdcMockSchool);
    mockSchool.setCreateDate(null);
    mockSchool.setUpdateDate(null);
    mockSchool.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());

    this.mockMvc.perform(put(URL.BASE_URL_SCHOOL_COLLECTION + "/" + sdcMockSchool.getSdcSchoolCollectionID().toString())
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(mockSchool))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    var updatedSchool = sdcSchoolCollectionRepository.findById(sdcMockSchool.getSdcSchoolCollectionID());
    assertThat(updatedSchool).isPresent();
    assertThat(updatedSchool.get().getSdcSchoolCollectionStatusCode()).isEqualTo(SdcSchoolCollectionStatus.NEW.getCode());
  }

  @Test
  void testUpdateCollection_GivenBadStatus_ShouldReturnBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    School school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    var mockSchool = SdcSchoolCollectionMapper.mapper.toSdcSchoolBatch(sdcMockSchool);
    mockSchool.setCreateDate(null);
    mockSchool.setUpdateDate(null);
    mockSchool.setSdcSchoolCollectionStatusCode("ABC");

    this.mockMvc.perform(put(URL.BASE_URL_SCHOOL_COLLECTION + "/" + sdcMockSchool.getSdcSchoolCollectionID().toString())
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRITE_SDC_COLLECTION")))
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

        School school = createMockSchool();
        SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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

        School school = createMockSchool();
        SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchool);

        this.mockMvc.perform(
                        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + school.getSchoolId()).with(mockAuthority))
                .andDo(print()).andExpect(status().isOk()).andExpect(
                        MockMvcResultMatchers.jsonPath("$.sdcSchoolCollectionID",
                                equalTo(sdcMockSchool.getSdcSchoolCollectionID().toString())));
    }

    @Test
    void testGetCollectionBySchoolID_ShouldReturnStatusNotFound() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        CollectionEntity collection = createMockCollectionEntity();
        collection.setOpenDate(LocalDateTime.now().minusDays(5));
        collection.setCloseDate(LocalDateTime.now().minusDays(2));
        collectionRepository.save(collection);

        School school = createMockSchool();
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchool);

        this.mockMvc.perform(
                        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + school.getSchoolId()).with(mockAuthority))
                .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    void testGetCollectionBySchoolID_ShouldReturnSchoolCollectionStatus() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCloseDate(LocalDateTime.now().plusDays(2));
        collectionRepository.save(collection);

        School school = createMockSchool();
        SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchool);

        this.mockMvc.perform(
                        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + school.getSchoolId()).with(mockAuthority))
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

        School school = createMockSchool();
        SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchool);

        this.mockMvc.perform(
                        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + school.getSchoolId()).with(mockAuthority))
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

        School school = createMockSchool();
        SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchool);

        this.mockMvc.perform(
                        get(URL.BASE_URL_SCHOOL_COLLECTION + "/search/" + school.getSchoolId()).with(mockAuthority))
                .andDo(print()).andExpect(status().isForbidden());
    }

}
