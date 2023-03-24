package ca.bc.gov.educ.studentdatacollection.api.collection.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.controller.v1.CollectionController;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CollectionControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  CollectionController controller;
  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  CollectionTypeCodeRepository collectionCodeRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @BeforeEach
  public void before() {
    this.collectionCodeRepository.save(this.createCollectionCodeData());
    this.collectionRepository.save(this.createCollectionData());
  }

  @Test
  void testGetAllCollections_WithWrongScope_ShouldReturnStatusForbidden() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "WRONG_SCOPE";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION).with(mockAuthority)).andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  void testGetAllCollections_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION).with(mockAuthority)).andDo(print())
        .andExpect(status().isOk()).andExpect(
            MockMvcResultMatchers.jsonPath("$.[0].collectionCode").value("TEST"));
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
        this.createCollectionData());

    this.mockMvc.perform(
            get(URL.BASE_URL_COLLECTION + "/" + newCollection.getCollectionID()).with(mockAuthority))
        .andDo(print()).andExpect(status().isOk()).andExpect(
            MockMvcResultMatchers.jsonPath("$.collectionID",
                equalTo(newCollection.getCollectionID().toString())));
  }

  @Test
  void testCreateCollection_GivenValidPayload_ShouldReturnStatusCreated() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity collection = this.createCollectionData();
    collection.setCreateDate(null);
    collection.setUpdateDate(null);

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(collection)).with(mockAuthority))
        .andDo(print()).andExpect(status().isCreated());
  }

  @Test
  void testCreateCollection_WithWrongScope_ShouldReturnStatusForbidden() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "WRONG_SCOPE";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity collection = this.createCollectionData();
    collection.setCreateDate(null);
    collection.setUpdateDate(null);

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(collection)).with(mockAuthority))
        .andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  void testCreateCollection_WithInvalidData_ShouldReturnStatusBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity collection = this.createCollectionData();

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION).contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(asJsonString(collection)).with(mockAuthority))
        .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testDeleteCollection_ShouldReturnStatusNoContent() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_DELETE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity newCollection = this.collectionRepository.save(
        this.createCollectionData());

    this.mockMvc.perform(
            delete(URL.BASE_URL_COLLECTION + "/" + newCollection.getCollectionID()).with(mockAuthority))
        .andDo(print()).andExpect(status().isNoContent());
  }

  @Test
  void testDeleteCollection_WithWrongID_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_DELETE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(
            delete(URL.BASE_URL_COLLECTION + "/" + UUID.randomUUID()).with(mockAuthority))
        .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testDeleteCollection_WithWrongScope_ShouldReturnStatusForbidden() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "WRONG_SCOPE";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final CollectionEntity collection = this.createCollectionData();
    collection.setCreateDate(null);
    collection.setUpdateDate(null);

    this.mockMvc.perform(delete(URL.BASE_URL_COLLECTION + "/" + UUID.randomUUID()).with(mockAuthority))
        .andDo(print()).andExpect(status().isForbidden());
  }

  private CollectionTypeCodeEntity createCollectionCodeData() {
    return CollectionTypeCodeEntity.builder().collectionTypeCode("TEST").label("Test")
        .description("Test code").displayOrder(0).effectiveDate(
            LocalDateTime.now()).expiryDate(LocalDateTime.MAX).openDate(LocalDateTime.now())
        .closeDate(LocalDateTime.MAX).createUser("TEST").createDate(LocalDateTime.now())
        .updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }

  private CollectionEntity createCollectionData() {
    return CollectionEntity.builder().collectionTypeCode("TEST")
        .openDate(LocalDateTime.now()).closeDate(LocalDateTime.MAX).createUser("TEST")
        .createDate(LocalDateTime.now()).updateUser("TEST").updateDate(LocalDateTime.now()).build();
  }

  static String asJsonString(final Object obj) {
    try {
      ObjectMapper om = new ObjectMapper();
      om.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      return om.writeValueAsString(obj);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

}
