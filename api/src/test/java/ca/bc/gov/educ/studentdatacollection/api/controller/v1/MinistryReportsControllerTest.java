package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolAddress;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SimpleHeadcountResultsTable;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.MinistryReportTypeCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {StudentDataCollectionApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MinistryReportsControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  SdcSchoolCollectionController sdcSchoolCollectionController;

  @Autowired
  CollectionRepository collectionRepository;

  @Autowired
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Autowired
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  @Autowired
  private RestUtils restUtils;
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  protected final static ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @Test
  void testGetMinistryReport_WithWrongType_ShouldReturnBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc.perform(get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + UUID.randomUUID() + "/testing").with(mockAuthority))
        .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testGetMinistryReport_ValidType_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

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

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/school-enrollment-headcounts").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SimpleHeadcountResultsTable>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getRows()).hasSize(2);
  }

  @Test
  void testGetMinistryReportCSV_ValidType_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

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

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/school-enrollment-headcounts/download").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getReportType()).isEqualTo(SCHOOL_ENROLLMENT_HEADCOUNTS.getCode());
  }

  @Test
  void testGetMinistryReport_TypeSCHOOL_ADDRESS_REPORT_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    var address = new SchoolAddress();
    address.setAddressLine1("1");
    address.setCity("xyz");
    address.setProvinceCode("BC");
    address.setPostal("v9b1a1");
    address.setAddressTypeCode("PHYSICAL");
    var addressList = new ArrayList<SchoolAddress>();
    addressList.add(address);
    school.setAddresses(addressList);
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

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

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/school-address-report").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SimpleHeadcountResultsTable>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getRows()).hasSize(2);
  }

  @Test
  void testGetMinistryReportCSV_TypeSCHOOL_ADDRESS_REPORT_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    var address = new SchoolAddress();
    address.setAddressLine1("1");
    address.setCity("xyz");
    address.setProvinceCode("BC");
    address.setPostal("v9b1a1");
    address.setAddressTypeCode("PHYSICAL");
    var addressList = new ArrayList<SchoolAddress>();
    addressList.add(address);
    school.setAddresses(addressList);
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

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

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/school-address-report/download").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getReportType()).isEqualTo(SCHOOL_ADDRESS_REPORT.getCode());
  }

  @Test
  void testGetMinistryReportCSV_TypeFSA_REGISTRATION_REPORT_SEPT_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

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
    sdcSchoolCollectionStudent1.setEnrolledGradeCode("04");
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudent1.setEnrolledGradeCode("07");
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/fsa-registration-report/download").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getReportType()).isEqualTo(FSA_REGISTRATION_REPORT.getCode());
  }

  @Test
  void testGetMinistryReportCSV_TypeFSA_REGISTRATION_REPORT_FEB_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
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
    sdcSchoolCollectionStudent1.setEnrolledGradeCode("03");
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/fsa-registration-report/download").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getReportType()).isEqualTo(FSA_REGISTRATION_REPORT.getCode());
  }

  @Test
  void testGetMinistryReportCSV_TypeFSA_REGISTRATION_REPORT_JULY_ShouldThrowError() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
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

    this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/fsa-registration-report/download").with(mockAuthority))
            .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testGetMinistryReport_ValidIndySchoolType_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());

    var fundingGroups = IndependentSchoolFundingGroup.builder().schoolFundingGroupCode("14").schoolGradeCode("GRADE01").build();
    school.setSchoolFundingGroups(Arrays.asList(fundingGroups));
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2));

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/indy-school-enrollment-headcounts").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SimpleHeadcountResultsTable>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getRows()).hasSize(2);
  }

  @Test
  void testGetMinistryReportSpecialEd_ValidIndySchoolType_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());

    var fundingGroups = IndependentSchoolFundingGroup.builder().schoolFundingGroupCode("14").schoolGradeCode("GRADE01").build();
    school.setSchoolFundingGroups(Arrays.asList(fundingGroups));
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2));

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    sdcSchoolCollectionStudent1.setSpecialEducationCategoryCode("P");
    sdcSchoolCollectionStudent1.setIsAdult(true);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/indy-inclusive-ed-enrollment-headcounts").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SimpleHeadcountResultsTable>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getRows()).hasSize(2);
  }

  @Test
  void testGetMinistryReportCSV_ValidIndySpecialEducationType_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    var fundingGroups = IndependentSchoolFundingGroup.builder().schoolFundingGroupCode("14").schoolGradeCode("GRADE01").build();
    school.setSchoolFundingGroups(Arrays.asList(fundingGroups));
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2));

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/indy-inclusive-ed-enrollment-headcounts/download").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getReportType()).isEqualTo(INDY_INCLUSIVE_ED_ENROLLMENT_HEADCOUNTS.getCode());
  }

  @Test
  void testGetMinistryReport_ValidIndySchoolType_ShouldReturnNoReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

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

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/indy-school-enrollment-headcounts").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SimpleHeadcountResultsTable>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getRows()).isEmpty();
  }

  @Test
  void testGetMinistryReportCSV_ValidIndySchoolsType_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
    var fundingGroups = IndependentSchoolFundingGroup.builder().schoolFundingGroupCode("14").schoolGradeCode("GRADE01").build();
    school.setSchoolFundingGroups(Arrays.asList(fundingGroups));
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict).getSdcDistrictCollectionID();

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict2).getSdcDistrictCollectionID();

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2));

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/indy-school-enrollment-headcounts/download").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getReportType()).isEqualTo(INDY_SCHOOL_ENROLLMENT_HEADCOUNTS.getCode());
  }

  @Test
  void testGetMinistryReport_TypeOFFSHORE_ENROLLMENT_HEADCOUNTS_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    school.setSchoolCategoryCode(SchoolCategoryCodes.OFFSHORE.getCode());
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict2);

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2));

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/offshore-enrollment-headcounts").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SimpleHeadcountResultsTable>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getRows()).hasSize(2);
  }

  @Test
  void testGetMinistryReport_TypeOFFSHORE_ENROLLMENT_HEADCOUNTS_ShouldReturnReportZeroData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict2);

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2));

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/offshore-enrollment-headcounts").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SimpleHeadcountResultsTable>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getRows()).isEmpty();
  }

  @Test
  void testGetMinistryReportCSV_TypeOFFSHORE_ENROLLMENT_HEADCOUNTS_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchoolDetail();
    school.setSchoolCategoryCode(SchoolCategoryCodes.OFFSHORE.getCode());
    when(this.restUtils.getAllSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict2);

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2));

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/offshore-enrollment-headcounts/download").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getReportType()).isEqualTo(OFFSHORE_ENROLLMENT_HEADCOUNTS.getCode());
  }

  @Test
  void testGetMinistryReportCSV_TypeOFFSHORE_SPOKEN_LANGUAGE_HEADCOUNTS_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school = this.createMockSchool();
    school.setSchoolCategoryCode(SchoolCategoryCodes.OFFSHORE.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict2);

    SchoolTombstone school1 = createMockSchool();
    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));

    SchoolTombstone school2 = createMockSchool();
    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2));

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    sdcSchoolCollectionStudent1.setHomeLanguageSpokenCode("001");
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudent2.setHomeLanguageSpokenCode("001");
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/offshore-languages-headcounts/download").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getReportType()).isEqualTo(OFFSHORE_SPOKEN_LANGUAGE_HEADCOUNTS.getCode());
  }

  @Test
  void testGetMinistryReport_TypeOFFSHORE_SPOKEN_LANGUAGE_HEADCOUNTS_ShouldReturnReportData() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_MINISTRY_REPORTS";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var school1 = this.createMockSchool();
    school1.setDisplayName("School1");
    school1.setSchoolCategoryCode(SchoolCategoryCodes.OFFSHORE.getCode());

    var school2 = this.createMockSchool();
    school2.setDisplayName("School2");
    school2.setSchoolCategoryCode(SchoolCategoryCodes.OFFSHORE.getCode());

    when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
    when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collection = collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    SdcDistrictCollectionEntity sdcMockDistrict2 = createMockSdcDistrictCollectionEntity(collection, null);
    sdcDistrictCollectionRepository.save(sdcMockDistrict2);

    school1.setDistrictId(sdcMockDistrict.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));

    school2.setDistrictId(sdcMockDistrict2.getDistrictID().toString());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));

    sdcSchoolCollectionRepository.saveAll(List.of(sdcSchoolCollectionEntity1, sdcSchoolCollectionEntity2));

    var sdcSchoolCollectionStudent1 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity1);
    sdcSchoolCollectionStudent1.setHomeLanguageSpokenCode("001");
    var sdcSchoolCollectionStudent2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity2);
    sdcSchoolCollectionStudent2.setHomeLanguageSpokenCode("001");
    sdcSchoolCollectionStudentRepository.saveAll(List.of(sdcSchoolCollectionStudent1, sdcSchoolCollectionStudent2));

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_MINISTRY_HEADCOUNTS + "/" + collection.getCollectionID() + "/offshore-languages-headcounts").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<SimpleHeadcountResultsTable>() {
    });

    assertThat(summary1).isNotNull();
    assertThat(summary1.getRows()).hasSize(2);
  }

}
