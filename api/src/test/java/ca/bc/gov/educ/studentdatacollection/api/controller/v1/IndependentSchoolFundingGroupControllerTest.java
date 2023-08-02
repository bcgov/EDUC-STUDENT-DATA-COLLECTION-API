package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.IndependentSchoolFundingGroupRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.IndependentSchoolFundingGroupSnapshotRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {StudentDataCollectionApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class IndependentSchoolFundingGroupControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private RestUtils restUtils;
  @Autowired
  IndependentSchoolFundingGroupController independentSchoolFundingGroupController;
  @Autowired
  IndependentSchoolFundingGroupRepository independentSchoolFundingGroupRepository;
  @Autowired
  IndependentSchoolFundingGroupSnapshotRepository independentSchoolFundingGroupSnapshotRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  protected final static ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @Test
  void testGetSchoolFundingGroup_WithWrongScope_ShouldReturnForbidden() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "WRONG_SCOPE";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc.perform(get(URL.BASE_URL_SCHOOL_FUNDING + "/" + UUID.randomUUID()).with(mockAuthority))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  void testGetSchoolFundingGroup_WithWrongID_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SCHOOL_FUNDING_GROUP";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc.perform(get(URL.BASE_URL_SCHOOL_FUNDING + "/" + UUID.randomUUID()).with(mockAuthority))
        .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testGetSchoolFundingGroupByID_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SCHOOL_FUNDING_GROUP";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final var independentSchoolFundingGroupEntity = this.independentSchoolFundingGroupRepository.save(this.createMockIndependentSchoolFundingGroupEntity(UUID.randomUUID()));

    this.mockMvc.perform(
            get(URL.BASE_URL_SCHOOL_FUNDING + "/" + independentSchoolFundingGroupEntity.getSchoolFundingGroupID()).with(mockAuthority))
        .andDo(print()).andExpect(status().isOk()).andExpect(
            MockMvcResultMatchers.jsonPath("$.schoolFundingGroupID",
                equalTo(independentSchoolFundingGroupEntity.getSchoolFundingGroupID().toString())));
  }

  @Test
  void testGetSchoolFundingGroupByCollectionAndSchoolIDs_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SCHOOL_FUNDING_GROUP";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final var independentSchoolFundingGroupEntity = this.independentSchoolFundingGroupSnapshotRepository.save(this.createMockIndependentSchoolFundingGroupSnapshotEntity(UUID.randomUUID(), UUID.randomUUID()));

    var resultActions = this.mockMvc.perform(
                    get(URL.BASE_URL_SCHOOL_FUNDING + "/snapshot/" + independentSchoolFundingGroupEntity.getSchoolID() + "/" + independentSchoolFundingGroupEntity.getCollectionID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List>() {
    });

    assertThat(summary).hasSize(1);

    var independentSchoolFundingGroupEntity1 = this.createMockIndependentSchoolFundingGroupSnapshotEntity(independentSchoolFundingGroupEntity.getSchoolID(), independentSchoolFundingGroupEntity.getCollectionID());
    independentSchoolFundingGroupEntity1.setSchoolGradeCode("GRADE02");
    this.independentSchoolFundingGroupSnapshotRepository.save(independentSchoolFundingGroupEntity1);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_SCHOOL_FUNDING + "/snapshot/" + independentSchoolFundingGroupEntity.getSchoolID() + "/" + independentSchoolFundingGroupEntity.getCollectionID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List>() {
    });

    assertThat(summary1).hasSize(2);
  }
  
  @Test
  void testGetSchoolFundingGroupByCreateUser_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SCHOOL_FUNDING_GROUP";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final var independentSchoolFundingGroupEntity = this.independentSchoolFundingGroupRepository.save(this.createMockIndependentSchoolFundingGroupEntity(UUID.randomUUID()));

    var resultActions = this.mockMvc.perform(
                    get(URL.BASE_URL_SCHOOL_FUNDING + "/search/" + independentSchoolFundingGroupEntity.getSchoolID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List>() {
    });

    assertThat(summary).hasSize(1);

    var independentSchoolFundingGroupEntity1 = this.createMockIndependentSchoolFundingGroupEntity(independentSchoolFundingGroupEntity.getSchoolID());
    independentSchoolFundingGroupEntity1.setSchoolGradeCode("GRADE02");
    this.independentSchoolFundingGroupRepository.save(independentSchoolFundingGroupEntity1);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_SCHOOL_FUNDING + "/search/" + independentSchoolFundingGroupEntity.getSchoolID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List>() {
    });

    assertThat(summary1).hasSize(2);
  }

  @Test
  void testCreateCollection_GivenValidPayload_ShouldReturnStatusCreated() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SCHOOL_FUNDING_GROUP";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(createMockSchool()));
    final var independentSchoolFundingGroupEntity = this.createMockIndependentSchoolFundingGroupEntity(UUID.randomUUID());
    independentSchoolFundingGroupEntity.setCreateDate(null);
    independentSchoolFundingGroupEntity.setUpdateDate(null);

    this.mockMvc.perform(post(URL.BASE_URL_SCHOOL_FUNDING).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(independentSchoolFundingGroupEntity)).with(mockAuthority))
        .andDo(print()).andExpect(status().isCreated());
  }

  @Test
  void testCreateCollection_GivenInValidGrade_ShouldReturnStatusBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SCHOOL_FUNDING_GROUP";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(createMockSchool()));
    final var independentSchoolFundingGroupEntity = this.createMockIndependentSchoolFundingGroupEntity(UUID.randomUUID());
    independentSchoolFundingGroupEntity.setCreateDate(null);
    independentSchoolFundingGroupEntity.setUpdateDate(null);
    independentSchoolFundingGroupEntity.setSchoolGradeCode("ABC");


    this.mockMvc.perform(post(URL.BASE_URL_SCHOOL_FUNDING).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(independentSchoolFundingGroupEntity)).with(mockAuthority))
        .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testCreateCollection_GivenInValidFundingCode_ShouldReturnStatusBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SCHOOL_FUNDING_GROUP";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(createMockSchool()));
    final var independentSchoolFundingGroupEntity = this.createMockIndependentSchoolFundingGroupEntity(UUID.randomUUID());
    independentSchoolFundingGroupEntity.setCreateDate(null);
    independentSchoolFundingGroupEntity.setUpdateDate(null);
    independentSchoolFundingGroupEntity.setSchoolFundingGroupCode("ABC");

    this.mockMvc.perform(post(URL.BASE_URL_SCHOOL_FUNDING).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(independentSchoolFundingGroupEntity)).with(mockAuthority))
        .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testCreateCollection_GivenInValidSchoolID_ShouldReturnStatusBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SCHOOL_FUNDING_GROUP";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.empty());
    final var independentSchoolFundingGroupEntity = this.createMockIndependentSchoolFundingGroupEntity(UUID.randomUUID());
    independentSchoolFundingGroupEntity.setCreateDate(null);
    independentSchoolFundingGroupEntity.setUpdateDate(null);

    this.mockMvc.perform(post(URL.BASE_URL_SCHOOL_FUNDING).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(independentSchoolFundingGroupEntity)).with(mockAuthority))
        .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testCreateCollection_GivenInvalidPayload_ShouldReturnBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SCHOOL_FUNDING_GROUP";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity collection = this.createMockCollectionEntity();
    collection.setCollectionID(UUID.randomUUID());
    collection.setCreateDate(null);
    collection.setUpdateDate(null);

    this.mockMvc.perform(post(URL.BASE_URL_SCHOOL_FUNDING).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(collection)).with(mockAuthority))
        .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testDeleteCollection_GivenValidPayload_ShouldReturnNoContent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_DELETE_SCHOOL_FUNDING_GROUP";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final var independentSchoolFundingGroupEntity = this.independentSchoolFundingGroupRepository.save(this.createMockIndependentSchoolFundingGroupEntity(UUID.randomUUID()));

    this.mockMvc.perform(
            delete(URL.BASE_URL_SCHOOL_FUNDING + "/" + independentSchoolFundingGroupEntity.getSchoolFundingGroupID().toString()).contentType(
                    MediaType.APPLICATION_JSON)
                .with(mockAuthority))
        .andDo(print()).andExpect(status().isNoContent());

    var independentSchoolFundingGroupRepositoryAll =  this.independentSchoolFundingGroupRepository.findAll();
    assertThat(independentSchoolFundingGroupRepositoryAll).isEmpty();
  }

}
