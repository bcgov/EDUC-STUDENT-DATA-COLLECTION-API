package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramDuplicateTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.CollectionSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ca.bc.gov.educ.studentdatacollection.api.struct.v1.Condition.AND;
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
  SdcDuplicateRepository sdcDuplicateRepository;
  @Autowired
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  @Autowired
  SdcSchoolCollectionStudentValidationIssueRepository sdcSchoolCollectionStudentValidationIssueRepository;
  @Autowired
  RestUtils restUtils;
  @Autowired
  CodeTableService codeTableService;
  @Autowired
  ValidationRulesService validationRulesService;
  @Autowired
  CollectionTypeCodeRepository collectionTypeCodeRepository;
  @Autowired
  SagaService sagaService;
  @Autowired
  SagaRepository sagaRepository;
  @Autowired
  SagaEventRepository sagaEventRepository;
  @Autowired
  SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  protected static final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void after() {
    this.sagaEventRepository.deleteAll();
    this.sdcDuplicateRepository.deleteAll();
    this.collectionRepository.deleteAll();
    this.sdcDuplicateRepository.deleteAll();
    this.sagaRepository.deleteAll();
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
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].unresolvedDuplicates").value("0"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].sdcDistrictCollectionId").value(mockDistrictCollectionEntity2.getSdcDistrictCollectionID().toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].districtTitle").value("012 - District2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].sdcDistrictCollectionStatusCode").value("NEW"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].numSubmittedSchools").value("0/2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].unresolvedDuplicates").value("0"));
  }

  @Test
  void testPostProvincialDuplicates_WithValidPayload_ReturnsCorrectResponse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
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

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/in-province-duplicates").with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk());
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

    this.collectionRepository.save(
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
    closedCollection.setCollectionStatusCode(CollectionStatus.COMPLETED.getCode());
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

    SchoolTombstone school1 = this.createMockSchool();
    SdcSchoolCollectionEntity schoolCollection1 = this.createMockSdcSchoolCollectionEntity(currentCollection, UUID.fromString(school1.getSchoolId()));
    SdcSchoolCollectionStudentEntity student1 = this.createMockSchoolStudentEntity(schoolCollection1);
    UUID student1AssignedPen = UUID.randomUUID();
    student1.setAssignedStudentId(student1AssignedPen);
    this.sdcSchoolCollectionRepository.save(schoolCollection1);
    this.sdcSchoolCollectionStudentRepository.save(student1);

    SchoolTombstone school2 = this.createMockSchool();
    school2.setDistrictId(String.valueOf(UUID.randomUUID()));
    SdcSchoolCollectionEntity schoolCollection2 = this.createMockSdcSchoolCollectionEntity(currentCollection, UUID.fromString(school2.getSchoolId()));
    SdcSchoolCollectionStudentEntity student2 = this.createMockSchoolStudentEntity(schoolCollection2);
    student2.setAssignedStudentId(student1AssignedPen);
    SdcSchoolCollectionStudentEntity student3 = this.createMockSchoolStudentEntity(schoolCollection2);
    this.sdcSchoolCollectionRepository.save(schoolCollection2);
    this.sdcSchoolCollectionStudentRepository.saveAll(Arrays.asList(student2, student3));

    List<String> sdcSchoolRoleCodes = Arrays.asList("SCHOOL_SDC");
    EdxUser school1EdxUser1 = createMockEdxUser(sdcSchoolRoleCodes, null, UUID.fromString(school1.getSchoolId()), null);
    List<EdxUser> school1Users = Arrays.asList(school1EdxUser1);

    List<String> sdcDistrictRoleCodes = Arrays.asList("DISTRICT_SDC");
    EdxUser school2EdxUser = createMockEdxUser(null, sdcDistrictRoleCodes, null, UUID.fromString(school2.getDistrictId()));
    List<EdxUser> school2Users = Arrays.asList(school2EdxUser);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
    when(this.restUtils.getEdxUsersForSchool(UUID.fromString(school1.getSchoolId()))).thenReturn(school1Users);
    when(this.restUtils.getEdxUsersForDistrict(UUID.fromString(school2.getDistrictId()))).thenReturn(school2Users);

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION + "/" + currentCollection.getCollectionID() + "/in-province-duplicates").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    var dupeStudents = this.sdcDuplicateRepository.findAll();
    assertThat(dupeStudents).hasSize(1);
    assertThat(dupeStudents.get(0).getDuplicateLevelCode()).isEqualTo("PROVINCIAL");

    var schoolCollections = this.sdcSchoolCollectionRepository.findAll();
    schoolCollections.forEach(schoolCollection -> {
      assertThat(schoolCollection.getSdcSchoolCollectionStatusCode()).isEqualTo("P_DUP_POST");
    });

    var districtCollections = this.sdcDistrictCollectionRepository.findAll();
    districtCollections.forEach(districtCollection -> {
      assertThat(districtCollection.getSdcDistrictCollectionStatusCode()).isEqualTo("P_DUP_POST");
    });

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

  @Test
  void testFindDuplicatesInCollection_WithWrongScope_ShouldReturnForbidden() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "WRONG_SCOPE";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + UUID.randomUUID() +"/duplicates").with(mockAuthority)
            .param("matchedAssignedIDs", ""))
            .andDo(print())
            .andExpect(status().isForbidden());
  }

  @Test
  void testFindDuplicatesInCollection_WithWrongCollectionID_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + null +"/duplicates").with(mockAuthority)
            .param("matchedAssignedIDs", ""))
            .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testFindDuplicatesInCollection_shouldReturnDuplicateAssignedIds() throws Exception {
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

    var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    firstSchool.setUploadDate(null);
    firstSchool.setUploadFileName(null);
    firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    secondSchool.setUploadDate(null);
    secondSchool.setUploadFileName(null);
    secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    secondSchool.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
    sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchool, secondSchool));

    final File file = new File(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
    );
    final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
    var students = IntStream.range(0, models.size())
            .mapToObj(i -> {
              var student = models.get(i);
              var studentId = UUID.randomUUID();

              student.setAssignedStudentId(studentId);
              //Even students go to the previous year; odd students to the current year.
              if (i % 2 == 0) {
                student.setSdcSchoolCollection(secondSchool);
              } else {
                student.setSdcSchoolCollection(firstSchool);
              }
              if (i == 1) {
                student.setEnrolledProgramCodes("9876543210");
                student.setEllNonEligReasonCode(ProgramEligibilityIssueCode.NOT_ENROLLED_ELL.getCode());
              }
              if (i == 3) {
                student.setEnrolledProgramCodes("9876543217");
                student.setEllNonEligReasonCode(ProgramEligibilityIssueCode.HOMESCHOOL.getCode());
              }
              if (i == 5) {
                student.setEnrolledProgramCodes("9876543217");
              }
              if (i == 7) {
                student.setEnrolledProgramCodes("9876543217");
              }
              return student;
            })
            .toList();

    var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);
    var assignedIds = savedStudents.stream().map(s -> s.getAssignedStudentId().toString()).collect(Collectors.joining(","));

    this.mockMvc
            .perform(get(URL.BASE_URL_COLLECTION + "/"+ collection.getCollectionID() +"/duplicates")
                    .with(mockAuthority)
                    .param("matchedAssignedIDs", assignedIds)
                    .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(6)));
  }

  @Test
  void testFindDuplicatesInCollection_shouldReturnNoDuplicate() throws Exception {
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

    var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    firstSchool.setUploadDate(null);
    firstSchool.setUploadFileName(null);
    firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    secondSchool.setUploadDate(null);
    secondSchool.setUploadFileName(null);
    secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
    secondSchool.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
    sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchool, secondSchool));

    final File file = new File(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
    );
    final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
    var students = IntStream.range(0, models.size())
            .mapToObj(i -> {
              var student = models.get(i);
              var studentId = UUID.randomUUID();

              student.setAssignedStudentId(studentId);
              //Even students go to the previous year; odd students to the current year.
              if (i % 2 == 0) {
                student.setSdcSchoolCollection(secondSchool);
              } else {
                student.setSdcSchoolCollection(firstSchool);
              }
              if (i == 1) {
                student.setEnrolledProgramCodes("9876543210");
                student.setEllNonEligReasonCode(ProgramEligibilityIssueCode.NOT_ENROLLED_ELL.getCode());
              }
              if (i == 3) {
                student.setEnrolledProgramCodes("9876543217");
                student.setEllNonEligReasonCode(ProgramEligibilityIssueCode.HOMESCHOOL.getCode());
              }
              if (i == 5) {
                student.setEnrolledProgramCodes("9876543217");
              }
              if (i == 7) {
                student.setEnrolledProgramCodes("9876543217");
              }
              return student;
            })
            .toList();

    sdcSchoolCollectionStudentRepository.saveAll(students);
    var assignedIds = UUID.randomUUID().toString();

    this.mockMvc
            .perform(get(URL.BASE_URL_COLLECTION + "/"+ collection.getCollectionID() +"/duplicates")
                    .with(mockAuthority)
                    .param("matchedAssignedIDs", assignedIds)
                    .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(0)));
  }
  @Test
  void testGetDistrictCollectionProvincialDuplicates() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID2 = sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict3 = createMockSdcDistrictCollectionEntity(collection, null);
    var sdcDistrictCollectionID3 = sdcDistrictCollectionRepository.save(sdcMockDistrict3).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID2);

    SchoolTombstone school3 = createMockSchool();
    school3.setDistrictId(sdcMockDistrict3.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity3 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school3.getSchoolId()));
    sdcSchoolCollectionEntity3.setSdcDistrictCollectionID(sdcDistrictCollectionID3);


    SchoolTombstone school4 = createMockSchool(); //Same district as school1
    school4.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity4 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school4.getSchoolId()));
    sdcSchoolCollectionEntity4.setSdcDistrictCollectionID(sdcDistrictCollectionID);

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2, sdcSchoolCollectionEntity3, sdcSchoolCollectionEntity4));

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
    when(this.restUtils.getSchoolBySchoolID(school3.getSchoolId())).thenReturn(Optional.of(school3));
    when(this.restUtils.getSchoolBySchoolID(school4.getSchoolId())).thenReturn(Optional.of(school4));

    var assignedStudentID = UUID.randomUUID();
    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    sdcSchoolCollectionStudent1.setAssignedStudentId(assignedStudentID);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudent2.setAssignedStudentId(assignedStudentID);
    var sdcSchoolCollectionStudent3 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity3);
    sdcSchoolCollectionStudent3.setAssignedStudentId(assignedStudentID);
    var sdcSchoolCollectionStudent4 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity4);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2, sdcSchoolCollectionStudent3, sdcSchoolCollectionStudent4));

    //Same district, won't be provincial dupe
    var inDistDupe = createMockSdcDuplicateEntity(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent4, collection.getCollectionID());
    sdcDuplicateRepository.save(inDistDupe);

    this.mockMvc.perform(get(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/in-province-duplicates")
                    .with(mockAuthority)
                    .header("correlationID", UUID.randomUUID().toString())
                    .contentType(APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(3)));
  }

  @Test
  void testAutoResolveProvincialDuplicates() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection.setCollectionStatusCode("PROVDUPES");
    collection = collectionRepository.save(collection);

    //Create Districts
    District mockDistrict = createMockDistrict();
    SdcDistrictCollectionEntity sdcMockDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(mockDistrict.getDistrictId()));
    var sdcDistrictCollectionID = sdcDistrictCollectionRepository.save(sdcMockDistrictCollection).getSdcDistrictCollectionID();

    District mockDistrict2 = createMockDistrict();
    SdcDistrictCollectionEntity sdcMockDistrictCollection2 = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(mockDistrict2.getDistrictId()));
    var sdcDistrictCollectionID2 = sdcDistrictCollectionRepository.save(sdcMockDistrictCollection2).getSdcDistrictCollectionID();

    District mockDistrict3 = createMockDistrict();
    SdcDistrictCollectionEntity sdcMockDistrictCollection3 = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(mockDistrict3.getDistrictId()));
    var sdcDistrictCollectionID3 = sdcDistrictCollectionRepository.save(sdcMockDistrictCollection3).getSdcDistrictCollectionID();

    // Create schools
    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrictCollection.getDistrictID().toString());
    school1.setFacilityTypeCode(FacilityTypeCodes.ALT_PROGS.getCode());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
    sdcSchoolCollectionEntity1.setSdcDistrictCollectionID(sdcDistrictCollectionID);

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrictCollection2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcSchoolCollectionEntity2.setSdcDistrictCollectionID(sdcDistrictCollectionID2);

    SchoolTombstone school3 = createMockSchool();
    school3.setDistrictId(sdcMockDistrictCollection3.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity3 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school3.getSchoolId()));
    sdcSchoolCollectionEntity3.setSdcDistrictCollectionID(sdcDistrictCollectionID3);


    SchoolTombstone school4 = createMockSchool(); //Same district as school1
    school4.setDistrictId(sdcMockDistrictCollection.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity4 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school4.getSchoolId()));
    sdcSchoolCollectionEntity4.setSdcDistrictCollectionID(sdcDistrictCollectionID);

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2, sdcSchoolCollectionEntity3, sdcSchoolCollectionEntity4));

    // Create students
    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudent1.setFte(BigDecimal.ONE);

    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity3);
    sdcSchoolCollectionStudent2.setFte(BigDecimal.valueOf(.50));

    var sdcSchoolCollectionStudent3 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity3);
    sdcSchoolCollectionStudent3.setSchoolFundingCode(null);
    sdcSchoolCollectionStudent3.setSpecialEducationCategoryCode(null);
    sdcSchoolCollectionStudent3.setBandCode(null);
    sdcSchoolCollectionStudent3.setFte(BigDecimal.valueOf(.50));
    sdcSchoolCollectionStudent3.setEnrolledProgramCodes("2942");
    sdcSchoolCollectionStudent3.setNumberOfCoursesDec(BigDecimal.valueOf(10.00));

    var sdcSchoolCollectionStudent4 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity4);
    sdcSchoolCollectionStudent4.setEnrolledGradeCode(SchoolGradeCodes.GRADE04.getCode());
    sdcSchoolCollectionStudent4.setSchoolFundingCode(null);
    sdcSchoolCollectionStudent4.setSpecialEducationCategoryCode(null);
    sdcSchoolCollectionStudent4.setBandCode(null);
    sdcSchoolCollectionStudent4.setNumberOfCoursesDec(BigDecimal.valueOf(12.50));
    sdcSchoolCollectionStudent4.setEnrolledProgramCodes("29");

    var sdcSchoolCollectionStudent5 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);

    var sdcSchoolCollectionStudent6 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity4);

    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2, sdcSchoolCollectionStudent3, sdcSchoolCollectionStudent4, sdcSchoolCollectionStudent5, sdcSchoolCollectionStudent6));

    // Create dupes
    var provincialDuplicate = createMockSdcDuplicateEntity(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2, collection.getCollectionID());
    provincialDuplicate.setDuplicateLevelCode(DuplicateLevelCode.PROVINCIAL.getCode());
    sdcDuplicateRepository.save(provincialDuplicate);

    var outOfDistrictDuplicate = createMockSdcDuplicateEntity(sdcSchoolCollectionStudent5, sdcSchoolCollectionStudent6, collection.getCollectionID());
    outOfDistrictDuplicate.setDuplicateLevelCode(DuplicateLevelCode.PROVINCIAL.getCode());
    sdcDuplicateRepository.save(outOfDistrictDuplicate);

    var programDuplicate = createMockSdcDuplicateEntity(sdcSchoolCollectionStudent3, sdcSchoolCollectionStudent4, collection.getCollectionID());
    programDuplicate.setDuplicateLevelCode(DuplicateLevelCode.PROVINCIAL.getCode());
    programDuplicate.setDuplicateTypeCode(DuplicateTypeCode.PROGRAM.getCode());
    programDuplicate.setProgramDuplicateTypeCode(ProgramDuplicateTypeCode.INDIGENOUS.getCode());
    sdcDuplicateRepository.save(programDuplicate);

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
    when(this.restUtils.getSchoolBySchoolID(school3.getSchoolId())).thenReturn(Optional.of(school3));
    when(this.restUtils.getSchoolBySchoolID(school4.getSchoolId())).thenReturn(Optional.of(school4));
    when(this.restUtils.getDistrictByDistrictID(String.valueOf(mockDistrict.getDistrictId()))).thenReturn(Optional.of(mockDistrict));
    when(this.restUtils.getDistrictByDistrictID(String.valueOf(mockDistrict2.getDistrictId()))).thenReturn(Optional.of(mockDistrict2));
    when(this.restUtils.getDistrictByDistrictID(String.valueOf(mockDistrict3.getDistrictId()))).thenReturn(Optional.of(mockDistrict3));

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/resolve-duplicates")
            .with(mockAuthority)
            .header("correlationID", UUID.randomUUID().toString())
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    var currentCollection = this.collectionRepository.findAll();
    assertThat(currentCollection.get(0).getCollectionStatusCode()).isEqualTo("DUPES_RES");
  }

  @Test
  void testCloseCollection_GivenValidInput_ShouldReturnStatusAcceptedRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.collectionTypeCodeRepository.save(this.createMockCollectionCodeEntityForFeb());

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection.setCollectionStatusCode("INPROGRESS");
    val savedCollection = collectionRepository.save(collection);

    CollectionSagaData sagaData = new CollectionSagaData();
    sagaData.setExistingCollectionID(savedCollection.getCollectionID().toString());
    sagaData.setNewCollectionSignOffDueDate(String.valueOf(LocalDateTime.now()));
    sagaData.setNewCollectionDuplicationResolutionDueDate(String.valueOf(LocalDateTime.now()));
    sagaData.setNewCollectionSnapshotDate(String.valueOf(LocalDateTime.now()));
    sagaData.setNewCollectionSubmissionDueDate(String.valueOf(LocalDateTime.now()));

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION + "/close-collection")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(sagaData))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(mockAuthority))
            .andDo(print()).andExpect(status().isAccepted());
  }

  @Test
  void testCloseCollection_GivenInvalidInput_ShouldReturnStatusBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.collectionTypeCodeRepository.save(this.createMockCollectionCodeEntityForFeb());

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection.setCollectionStatusCode("INPROGRESS");
    val savedCollection = collectionRepository.save(collection);

    CollectionSagaData sagaData = new CollectionSagaData();
    sagaData.setExistingCollectionID(savedCollection.getCollectionID().toString());
    sagaData.setNewCollectionSnapshotDate(String.valueOf(LocalDateTime.now()));
    sagaData.setNewCollectionSubmissionDueDate(String.valueOf(LocalDateTime.now()));

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION + "/close-collection")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(sagaData))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(mockAuthority))
            .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testCloseCollection_MultipleSAGAs_ShouldReturnStatusConflict() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.collectionTypeCodeRepository.save(this.createMockCollectionCodeEntityForFeb());

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection.setCollectionStatusCode("INPROGRESS");
    val savedCollection = collectionRepository.save(collection);

    CollectionSagaData sagaData = new CollectionSagaData();
    sagaData.setExistingCollectionID(savedCollection.getCollectionID().toString());
    sagaData.setNewCollectionSignOffDueDate(String.valueOf(LocalDateTime.now()));
    sagaData.setNewCollectionDuplicationResolutionDueDate(String.valueOf(LocalDateTime.now()));
    sagaData.setNewCollectionSnapshotDate(String.valueOf(LocalDateTime.now()));
    sagaData.setNewCollectionSubmissionDueDate(String.valueOf(LocalDateTime.now()));

    this.sagaService.createSagaRecordInDB(SagaEnum.CLOSE_COLLECTION_SAGA.toString(), ApplicationProperties.STUDENT_DATA_COLLECTION_API, JsonUtil.getJsonStringFromObject(sagaData), null, null, UUID.fromString(sagaData.getExistingCollectionID()));

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION + "/close-collection")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(sagaData))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(mockAuthority))
            .andDo(print()).andExpect(status().isConflict());
  }

  @Test
  void testReadCollectionPaginated_withValidPayload_ShouldReturnStatusOk() throws Exception {
    var collectionEntity = createMockCollectionEntity();
    collectionEntity.setSubmissionDueDate(LocalDate.now());
    var collection = collectionRepository.save(collectionEntity);


    var yesterday = LocalDate.now().plusDays(-1);
    var tomorrow = LocalDate.now().plusDays(1);

    final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("collectionTypeCode").operation(FilterOperation.EQUAL).value(collection.getCollectionTypeCode()).valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().condition(AND).key("submissionDueDate").operation(FilterOperation.BETWEEN).value(yesterday + "," + tomorrow).valueType(ValueType.DATE).build();

    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());

    final var objMapper = new ObjectMapper();
    final String criteriaJSON = objMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
            .perform(get(URL.BASE_URL_COLLECTION+URL.PAGINATED)
                    .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_COLLECTION")))
                    .param("searchCriteriaList", criteriaJSON)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  void testGetAllSchoolCollectionsInCollection_shouldReturnSchoolCollections() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity sdcMockDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcMockDistrictCollection);

    SchoolTombstone school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrictCollection.getSdcDistrictCollectionID());
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    SchoolTombstone school2 = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setSdcDistrictCollectionID(sdcMockDistrictCollection.getSdcDistrictCollectionID());
    sdcSchoolCollectionRepository.save(sdcMockSchool2);

    this.mockMvc.perform(
                    get(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/sdcSchoolCollections").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)));
  }

  @Test
  void testGetAllDistrictCollectionsInCollection_shouldReturnDistrictCollections() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity sdcMockDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcMockDistrictCollection);

    SchoolTombstone school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrictCollection.getSdcDistrictCollectionID());
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    SchoolTombstone school2 = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setSdcDistrictCollectionID(sdcMockDistrictCollection.getSdcDistrictCollectionID());
    sdcSchoolCollectionRepository.save(sdcMockSchool2);

    this.mockMvc.perform(
                    get(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/sdcDistrictCollections").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(1)));
  }

  @Test
  void testGetEnrolmentCountInCollectionByGrade_WithValidPayload_ReturnsCorrectResponse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
    var districtID1 = UUID.randomUUID();
    var district1 = createMockDistrict();
    district1.setDisplayName("District1");
    district1.setDistrictNumber("011");
    district1.setDistrictId(districtID1.toString());
    var mockDistrictCollectionEntity1 = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID1));

    SchoolTombstone school = this.createMockSchool();
    school.setDistrictId(districtID1.toString());
    var sdcSchoolCollection1a = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
    sdcSchoolCollection1a.setSdcDistrictCollectionID(mockDistrictCollectionEntity1.getSdcDistrictCollectionID());
    sdcSchoolCollection1a.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());

    var sdcSchoolCollectionEntity  = sdcSchoolCollectionRepository.save(sdcSchoolCollection1a);

    var student1 = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    student1.setEnrolledGradeCode("12");

    var student2 = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    student2.setEnrolledGradeCode("12");

    var student3 = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
    student3.setEnrolledGradeCode("10");
    this.sdcSchoolCollectionStudentRepository.saveAll(List.of(student1, student2, student3));
    when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));
    String gradeToQuery = "12";

    Map<String, List<UUID>> requestBody = new HashMap<>();
    requestBody.put("schoolIDs", List.of(UUID.fromString(school.getSchoolId())));

    String requestBodyJson = objectMapper.writeValueAsString(requestBody);

    this.mockMvc.perform(post(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/counts/"+gradeToQuery )
                    .with(mockAuthority)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBodyJson))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].schoolID").value(school.getSchoolId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].gradeEnrolmentCount").value("2"));
  }

  @Test
  void testGetCollectionClosureSagaStatus_WhenSagaExists_ShouldReturnTrue() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());

    CollectionSagaData sagaData = new CollectionSagaData();
    sagaData.setExistingCollectionID(collection.getCollectionID().toString());
    sagaData.setNewCollectionSignOffDueDate(String.valueOf(LocalDateTime.now()));
    sagaData.setNewCollectionDuplicationResolutionDueDate(String.valueOf(LocalDateTime.now()));
    sagaData.setNewCollectionSnapshotDate(String.valueOf(LocalDateTime.now()));
    sagaData.setNewCollectionSubmissionDueDate(String.valueOf(LocalDateTime.now()));

    var saga = createMockCloseCollectionSaga(sagaData);
    saga.setCollectionID(collection.getCollectionID());
    sagaRepository.save(saga);

    this.mockMvc.perform(
                    get(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/closure-saga-status").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("true"));
  }

  @Test
  void testGetCollectionClosureSagaStatus_WhenSagaDoesNotExist_ShouldReturnFalse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());

    this.mockMvc.perform(
                    get(URL.BASE_URL_COLLECTION + "/" + collection.getCollectionID() + "/closure-saga-status").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("false"));
  }

}
