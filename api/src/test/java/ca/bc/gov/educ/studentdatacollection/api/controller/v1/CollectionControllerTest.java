package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDuplicateRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentValidationIssueRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
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
import static org.hamcrest.Matchers.hasSize;
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
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  @Autowired
  SdcSchoolCollectionStudentValidationIssueRepository sdcSchoolCollectionStudentValidationIssueRepository;
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

  void testGetMonitorSdcDistrictCollectionResponse_WithInValidId_ReturnsNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + UUID.randomUUID() + "/monitorSdcDistrictCollections").with(mockAuthority)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testGetMonitorSdcDistrictCollectionResponse_WithValidPayload_ReturnsCorrectResponse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    var districtID1 = UUID.randomUUID();
    var district1 = createMockDistrict();
    district1.setDisplayName("District1");
    district1.setDistrictNumber("011");
    district1.setDistrictId(districtID1.toString());
    var mockDistrictCollectionEntity1 = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID1));

    var districtID2 = UUID.randomUUID();
    var district2 = createMockDistrict();
    district2.setDisplayName("District2");
    district2.setDistrictNumber("012");
    district2.setDistrictId(districtID2.toString());
    var mockDistrictCollectionEntity2 = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID2));

    var sdcSchoolCollection1a = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
    sdcSchoolCollection1a.setSdcDistrictCollectionID(mockDistrictCollectionEntity1.getSdcDistrictCollectionID());
    sdcSchoolCollection1a.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());
    var sdcSchoolCollection1b = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
    sdcSchoolCollection1b.setSdcDistrictCollectionID(mockDistrictCollectionEntity1.getSdcDistrictCollectionID());
    sdcSchoolCollection1b.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SCH_C_VRFD.getCode());

    var sdcSchoolCollection2a = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
    sdcSchoolCollection2a.setSdcDistrictCollectionID(mockDistrictCollectionEntity2.getSdcDistrictCollectionID());
    sdcSchoolCollection2a.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SCH_C_VRFD.getCode());
    var sdcSchoolCollection2b = createMockSdcSchoolCollectionEntity(collection, UUID.randomUUID());
    sdcSchoolCollection2b.setSdcDistrictCollectionID(mockDistrictCollectionEntity2.getSdcDistrictCollectionID());
    sdcSchoolCollection2b.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SCH_C_VRFD.getCode());
    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollection1a, sdcSchoolCollection1b, sdcSchoolCollection2a, sdcSchoolCollection2b));

    when(this.restUtils.getDistrictByDistrictID(district1.getDistrictId())).thenReturn(Optional.of(district1));
    when(this.restUtils.getDistrictByDistrictID(district2.getDistrictId())).thenReturn(Optional.of(district2));

    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/monitorSdcDistrictCollections").with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].sdcDistrictCollectionId").value(mockDistrictCollectionEntity1.getSdcDistrictCollectionID().toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].districtTitle").value("011 - District1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].sdcDistrictCollectionStatusCode").value("NEW"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].numSubmittedSchools").value("1/2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].unresolvedProgramDuplicates").value("0"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].unresolvedEnrollmentDuplicates").value("0"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].sdcDistrictCollectionId").value(mockDistrictCollectionEntity2.getSdcDistrictCollectionID().toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].districtTitle").value("012 - District2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].sdcDistrictCollectionStatusCode").value("NEW"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].numSubmittedSchools").value("0/2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].unresolvedProgramDuplicates").value("0"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].unresolvedEnrollmentDuplicates").value("0"));
  }
  @Test
  void testGetMonitorIndySdcSchoolCollectionResponse_WithInValidId_ReturnsNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + UUID.randomUUID() + "/monitorIndySdcSchoolCollections").with(mockAuthority)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testGetMonitorIndySdcSchoolCollectionResponse_WithValidPayload_ReturnsCorrectResponse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
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
    var school3 = createMockSchool();
    school3.setDisplayName("School3");
    school3.setMincode("0000003");
    school3.setDistrictId(districtID.toString());

    var sdcSchoolCollection1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollection1.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    sdcSchoolCollection1.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());
    sdcSchoolCollection1.setSdcDistrictCollectionID(null);
    var sdcSchoolCollection2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollection2.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    sdcSchoolCollection2.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SCH_C_VRFD.getCode());
    sdcSchoolCollection2.setSdcDistrictCollectionID(null);
    var sdcSchoolCollection3 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school3.getSchoolId()));
    sdcSchoolCollection3.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    sdcSchoolCollection3.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SCH_C_VRFD.getCode());
    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollection1, sdcSchoolCollection2, sdcSchoolCollection3));

    var  student = createMockSchoolStudentEntity(sdcSchoolCollection2);
    var issue1 = createMockSdcSchoolCollectionStudentValidationIssueEntity(student, StudentValidationIssueSeverityCode.ERROR);
    var issue2 = createMockSdcSchoolCollectionStudentValidationIssueEntity(student, StudentValidationIssueSeverityCode.ERROR);
    var issue3 = createMockSdcSchoolCollectionStudentValidationIssueEntity(student, StudentValidationIssueSeverityCode.ERROR);
    issue2.setValidationIssueFieldCode("SOMEOTHERFIELD"); //same code different field, should NOT register as a unique error
    issue3.setValidationIssueCode("DIFFERENTCODE"); //different code, should register as a unique error

    sdcSchoolCollectionStudentRepository.save(student);
    sdcSchoolCollectionStudentValidationIssueRepository.saveAll(List.of(issue1, issue2, issue3));

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
    when(this.restUtils.getSchoolBySchoolID(school3.getSchoolId())).thenReturn(Optional.of(school3));

    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/monitorIndySdcSchoolCollections").with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].sdcSchoolCollectionId").value(sdcSchoolCollection1.getSdcSchoolCollectionID().toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].schoolTitle").value("0000001 - School1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].errors").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].fundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].infoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].schoolStatus").value("SUBMITTED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].sdcSchoolCollectionId").value(sdcSchoolCollection2.getSdcSchoolCollectionID().toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].schoolTitle").value("0000002 - School2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].errors").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].fundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].infoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].schoolStatus").value("SCH_C_VRFD"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsWithData").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalErrors").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalFundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalInfoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsSubmitted").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalSchools").value(2));
  }

  @Test
  void testGetMonitorIndySdcSchoolCollectionResponse_GivenSchoolOnlyHasDeletedOrNoStudents_ReturnsCorrectResponse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
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
    sdcSchoolCollection1.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.VERIFIED.getCode());
    sdcSchoolCollection1.setSdcDistrictCollectionID(null);
    var sdcSchoolCollection2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollection2.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    sdcSchoolCollection2.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SCH_C_VRFD.getCode());
    sdcSchoolCollection2.setSdcDistrictCollectionID(null);
    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollection1, sdcSchoolCollection2));

    var  student = createMockSchoolStudentEntity(sdcSchoolCollection2);
    student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.getCode());
    var issue1 = createMockSdcSchoolCollectionStudentValidationIssueEntity(student, StudentValidationIssueSeverityCode.ERROR);
    var issue2 = createMockSdcSchoolCollectionStudentValidationIssueEntity(student, StudentValidationIssueSeverityCode.ERROR);
    issue2.setValidationIssueCode("DIFFERENTCODE"); //different code, should register as a unique error

    sdcSchoolCollectionStudentRepository.save(student);
    sdcSchoolCollectionStudentValidationIssueRepository.saveAll(List.of(issue1, issue2));

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/monitorIndySdcSchoolCollections").with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].sdcSchoolCollectionId").value(sdcSchoolCollection1.getSdcSchoolCollectionID().toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].schoolTitle").value("0000001 - School1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].errors").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].fundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].infoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[0].schoolStatus").value("VERIFIED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].sdcSchoolCollectionId").value(sdcSchoolCollection2.getSdcSchoolCollectionID().toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].schoolTitle").value("0000002 - School2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].errors").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].fundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].infoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections[1].schoolStatus").value("SCH_C_VRFD"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsWithData").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalErrors").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalFundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalInfoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsSubmitted").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalSchools").value(2));
  }

  @Test
  void testGetMonitorIndySdcSchoolCollectionResponse_WithValidDistrictCollectionIdNoSchoolCollections_ReturnsCorrectResponse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, UUID.randomUUID()));


    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/monitorIndySdcSchoolCollections").with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.monitorSdcSchoolCollections", hasSize(0)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsWithData").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalErrors").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalFundingWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalInfoWarnings").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolsSubmitted").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalSchools").value(0));
  }

}
