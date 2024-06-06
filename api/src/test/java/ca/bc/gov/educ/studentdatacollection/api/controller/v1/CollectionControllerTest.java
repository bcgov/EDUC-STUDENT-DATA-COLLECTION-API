package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDuplicateRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {StudentDataCollectionApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CollectionControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  CollectionController controller;
  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  CollectionTypeCodeRepository CollectionTypeCodeRepository;
  @Autowired
  SdcDuplicateRepository sdcDuplicateRepository;
  @Autowired
  RestUtils restUtils;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  protected final static ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @BeforeEach
  public void before() {
    this.CollectionTypeCodeRepository.save(this.createMockCollectionCodeEntity());
  }

  @AfterEach
  public void afterEach() {
    this.sdcDuplicateRepository.deleteAll();
    this.collectionRepository.deleteAll();
    this.sdcDuplicateRepository.deleteAll();
  }


  @Test
  void testGetCollection_WithWrongScope_ShouldReturnForbidden() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "WRONG_SCOPE";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + UUID.randomUUID()).with(mockAuthority))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  void testGetCollection_WithWrongID_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + UUID.randomUUID()).with(mockAuthority))
        .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testGetCollectionByID_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity newCollection = this.collectionRepository.save(
        this.createMockCollectionEntity());

    this.mockMvc.perform(
            get(URL.BASE_URL_COLLECTION + "/" + newCollection.getCollectionID()).with(mockAuthority))
        .andDo(print()).andExpect(status().isOk()).andExpect(
            MockMvcResultMatchers.jsonPath("$.collectionID",
                equalTo(newCollection.getCollectionID().toString())));
  }

  @Test
  void testGetCollectionByCreateUser_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity newCollection = this.collectionRepository.save(
            this.createMockCollectionEntity());

    var resultActions = this.mockMvc.perform(
                    get(URL.BASE_URL_COLLECTION + "/search/" + newCollection.getCreateUser()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List>() {
    });

    assertThat(summary).hasSize(1);

    final CollectionEntity newCollection1 = this.collectionRepository.save(
            this.createMockCollectionEntity());

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_COLLECTION + "/search/" + newCollection.getCreateUser()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List>() {
    });

    assertThat(summary1).hasSize(2);
  }

  @Test
  void testCreateCollection_GivenValidPayload_ShouldReturnStatusCreated() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity collection = this.createMockCollectionEntity();
    collection.setCreateDate(null);
    collection.setUpdateDate(null);

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(collection)).with(mockAuthority))
        .andDo(print()).andExpect(status().isCreated());
  }

  @Test
  void testCreateCollection_GivenInvalidPayload_ShouldReturnBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity collection = this.createMockCollectionEntity();
    collection.setCollectionID(UUID.randomUUID());
    collection.setCreateDate(null);
    collection.setUpdateDate(null);

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(collection)).with(mockAuthority))
        .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testDeleteCollection_GivenValidPayload_ShouldReturnNoContent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_DELETE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity newCollection = this.collectionRepository.save(
        this.createMockCollectionEntity());

    this.mockMvc.perform(
            delete(URL.BASE_URL_COLLECTION + "/" + newCollection.getCollectionID()).contentType(
                    MediaType.APPLICATION_JSON)
                .with(mockAuthority))
        .andDo(print()).andExpect(status().isNoContent());

    List<CollectionEntity> collectionEntityList =  this.collectionRepository.findAll();
    assertThat(collectionEntityList).isEmpty();
  }

  @Test
  void testGetActiveCollection_ShouldReturnStatusOkWithData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity currentCollection = this.createMockCollectionEntity();
    currentCollection.setOpenDate(LocalDateTime.now().minusDays(9));

    final CollectionEntity closedCollection = this.createMockCollectionEntity();

    closedCollection.setOpenDate(LocalDateTime.now().minusMonths(6));
    closedCollection.setCloseDate(LocalDateTime.now().minusDays(10));

    this.collectionRepository.save(currentCollection);
    this.collectionRepository.save(closedCollection);

    this.mockMvc.perform(
                    get(URL.BASE_URL_COLLECTION + "/active").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk()).andExpect(
                    MockMvcResultMatchers.jsonPath("$.collectionID",
                            equalTo(currentCollection.getCollectionID().toString())));
  }

  @Test
  void testPostProvinceDuplicates_ShouldReturnStatusCreated() throws Exception{
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity currentCollection = this.createMockCollectionEntity();
    currentCollection.setOpenDate(LocalDateTime.now().minusDays(9));
    this.collectionRepository.save(currentCollection);

    School school1 = this.createMockSchool();
    SdcSchoolCollectionEntity schoolCollection1 = this.createMockSdcSchoolCollectionEntity(currentCollection, UUID.fromString(school1.getSchoolId()));
    SdcSchoolCollectionStudentEntity student1 = this.createMockSchoolStudentEntity(schoolCollection1);
    UUID student1AssignedPen = UUID.randomUUID();
    student1.setAssignedStudentId(student1AssignedPen);
    this.sdcSchoolCollectionRepository.save(schoolCollection1);
    this.sdcSchoolCollectionStudentRepository.save(student1);

    School school2 = this.createMockSchool();
    school2.setDistrictId(String.valueOf(UUID.randomUUID()));
    SdcSchoolCollectionEntity schoolCollection2 = this.createMockSdcSchoolCollectionEntity(currentCollection, UUID.fromString(school2.getSchoolId()));
    SdcSchoolCollectionStudentEntity student2 = this.createMockSchoolStudentEntity(schoolCollection2);
    student2.setAssignedStudentId(student1AssignedPen);
    SdcSchoolCollectionStudentEntity student3 = this.createMockSchoolStudentEntity(schoolCollection2);
    this.sdcSchoolCollectionRepository.save(schoolCollection2);
    this.sdcSchoolCollectionStudentRepository.saveAll(Arrays.asList(student2, student3));

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION + "/" + currentCollection.getCollectionID() + "/in-province-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    var dupeStudents = this.sdcDuplicateRepository.findAll();
    assertThat(dupeStudents).hasSize(1);
  }

  @Test
  void testPostProvinceDuplicatesWithIncorrectCollectionID_ShouldRespondWithBadRequest() throws Exception{
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity currentCollection = this.createMockCollectionEntity();
    currentCollection.setOpenDate(LocalDateTime.now().minusDays(9));
    this.collectionRepository.save(currentCollection);

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION + "/" + UUID.randomUUID() + "/in-province-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isBadRequest());
  }

}
