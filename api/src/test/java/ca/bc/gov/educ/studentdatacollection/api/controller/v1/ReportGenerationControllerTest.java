package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.dto.institute.PaginatedResponse;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Search;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValueType;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.summary.StudentDifference;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.struct.v1.Condition.AND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportGenerationControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  SdcSchoolCollectionController sdcSchoolCollectionController;

  @Autowired
  SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;

  @Autowired
  CollectionRepository collectionRepository;

  @Autowired
  SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Autowired
  SdcDistrictCollectionRepository sdcDistricCollectionRepository;

  @Autowired
  SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  @Autowired
  RestUtils restUtils;

  @Autowired
  SdcSchoolCollectionStudentEnrolledProgramRepository sdcSchoolCollectionStudentEnrolledProgramRepository;

  @Autowired
  private SdcSchoolCollectionStudentEnrolledProgramRepository enrolledProgramRepository;

  @AfterEach
  public void after() {
    this.sdcDistricCollectionRepository.deleteAll();
    this.collectionRepository.deleteAll();
    this.sdcSchoolCollectionStudentRepository.deleteAll();
    this.sdcSchoolCollectionStudentEnrolledProgramRepository.deleteAll();
    this.sdcSchoolCollectionRepository.deleteAll();
  }

  @ParameterizedTest
  @CsvSource({
          "GRADE_ENROLLMENT_HEADCOUNT",
          "CAREER_HEADCOUNT",
          "FRENCH_HEADCOUNT",
          "INDIGENOUS_HEADCOUNT",
          "BAND_RESIDENCE_HEADCOUNT",
          "ELL_HEADCOUNT",
          "SPECIAL_EDUCATION_HEADCOUNT"
  })
  void testGetGradeEnrollmentHeadcountReport_ShouldReturnCollection(String reportTypeCode) throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var district = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student2.setBandCode("0600");
    student2.setIsSchoolAged(false);
    student2.setIsAdult(true);
    student2.setFte(new BigDecimal(2.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    this.mockMvc.perform(
        get(URL.BASE_URL_REPORT_GENERATION + "/sdcSchoolCollection/" + sdcMockSchool.getSdcSchoolCollectionID() + "/" + reportTypeCode).with(mockAuthority))
      .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testGetFrenchProgramHeadcountReportCSF_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var district = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));
    var schoolMock = this.createMockSchool();
    schoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(false);
    student1.setIsAdult(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcSchoolCollection/" + sdcMockSchool.getSdcSchoolCollectionID() + "/FRENCH_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testGetGradeEnrollmentHeadcountReport_ShouldReturnBadRequest() throws Exception {
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
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcSchoolCollection/" + sdcMockSchool.getSdcSchoolCollectionID() + "/ABC").with(mockAuthority))
            .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testGetGradeEnrollmentHeadcountReport_MissingDistrict_ShouldReturn500Exception() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.empty());

    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockSchool.getSdcSchoolCollectionID() + "/GRADE_ENROLLMENT_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testGetGradeEnrollmentHeadcountReport_MissingSchool_ShouldReturn500Exception() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    var district = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.empty());

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SchoolTombstone schoolTombstone = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolTombstone.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockSchool.getSdcSchoolCollectionID() + "/GRADE_ENROLLMENT_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testAllStudentLightFromStudentCollectionIdGenerateCsvService_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var district = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(false);
    student1.setIsAdult(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcSchoolCollection/" + sdcMockSchool.getSdcSchoolCollectionID() + "/" + "ALL_STUDENT_SCHOOL_CSV").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testAllStudentLightFromStudentWithWarnsCollectionIdGenerateCsvService_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var district = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(false);
    student1.setIsAdult(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcSchoolCollection/" + sdcMockSchool.getSdcSchoolCollectionID() + "/" + "ALL_STUDENT_ERRORS_WARNS_SCHOOL_CSV").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testAllStudentLightFromDistrictCollectionIdGenerateCsvService_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(false);
    student1.setIsAdult(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "ALL_STUDENT_DIS_CSV").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testAllStudentLightFromDistrictWithErrorsWarnsCollectionIdGenerateCsvService_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setIsSchoolAged(false);
    student1.setIsAdult(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "ALL_STUDENT_ERRORS_WARNS_DIS_CSV").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testAllStudentLightFromStudentCollectionIdGenerateCsvService_misformattedUUID_ShouldReturn4xx() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + "0000" + "/" + "ALL_STUDENT_SCHOOL_CSV").with(mockAuthority))
            .andDo(print()).andExpect(status().is4xxClientError());
  }

  @Test
  void testAllStudentLightFromDistrictCollectionIdGenerateCsvService_misformattedUUID_ShouldReturn4xx() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + "0000" + "/" + "ALL_STUDENT_DIS_CSV").with(mockAuthority))
            .andDo(print()).andExpect(status().is4xxClientError());
  }

  @Test
  void testEligibleFrenchProgramHeadcountDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var csfSchoolMock = this.createMockSchool();
    csfSchoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(csfSchoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcCsfMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(csfSchoolMock.getSchoolId()));
    sdcCsfMockSchool.setUploadDate(null);
    sdcCsfMockSchool.setUploadFileName(null);
    sdcCsfMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcCsfMockSchool = sdcSchoolCollectionRepository.save(sdcCsfMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setIsSchoolAged(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADUATED_ADULT.getCode());
    student1.setIsSchoolAged(false);
    student1.setIsAdult(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE07.getCode());
    student3.setIsSchoolAged(true);
    student3.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcCsfMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student3.setIsSchoolAged(true);
    student3.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student4);

    setEnrolledProgramCode(student1, "08");
    setEnrolledProgramCode(student2, "11");
    setEnrolledProgramCode(student3, "14");
    setEnrolledProgramCode(student4, "05");

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_FRENCH_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEligibleFrenchProgramHeadcountDistrictPerSchool_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var csfSchoolMock = this.createMockSchool();
    csfSchoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(csfSchoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcCsfMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(csfSchoolMock.getSchoolId()));
    sdcCsfMockSchool.setUploadDate(null);
    sdcCsfMockSchool.setUploadFileName(null);
    sdcCsfMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcCsfMockSchool = sdcSchoolCollectionRepository.save(sdcCsfMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setIsSchoolAged(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADUATED_ADULT.getCode());
    student1.setIsSchoolAged(false);
    student1.setIsAdult(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE07.getCode());
    student3.setIsSchoolAged(true);
    student3.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcCsfMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student3.setIsSchoolAged(true);
    student3.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student4);

    setEnrolledProgramCode(student1, "08");
    setEnrolledProgramCode(student2, "11");
    setEnrolledProgramCode(student3, "14");
    setEnrolledProgramCode(student4, "05");

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_FRENCH_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEligibleFrenchProgramHeadcountDistrictPerSchool_emptyDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_FRENCH_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEnrollmentHeadcountDistrictReport_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setIsSchoolAged(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADUATED_ADULT.getCode());
    student1.setIsSchoolAged(false);
    student1.setIsAdult(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE07.getCode());
    student3.setIsSchoolAged(true);
    student3.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student3.setIsSchoolAged(true);
    student3.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student4);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_GRADE_ENROLLMENT_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEnrollmentHeadcountDistrictReportPerSchool_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var school2Mock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school2Mock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2Mock.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool2 = sdcSchoolCollectionRepository.save(sdcMockSchool2);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setIsSchoolAged(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADUATED_ADULT.getCode());
    student1.setIsSchoolAged(false);
    student1.setIsAdult(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool2);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE07.getCode());
    student3.setIsSchoolAged(true);
    student3.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcMockSchool2);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student3.setIsSchoolAged(true);
    student3.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student4);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_GRADE_ENROLLMENT_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEnrollmentHeadcountDistrictReportPerSchool_emptyDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_GRADE_ENROLLMENT_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testCareerHeadcountDistrictReport_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var csfSchoolMock = this.createMockSchool();
    csfSchoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(csfSchoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcCsfMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(csfSchoolMock.getSchoolId()));
    sdcCsfMockSchool.setUploadDate(null);
    sdcCsfMockSchool.setUploadFileName(null);
    sdcCsfMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcCsfMockSchool = sdcSchoolCollectionRepository.save(sdcCsfMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcCsfMockSchool);
    sdcSchoolCollectionStudentRepository.save(student4);

    setEnrolledProgramCode(student1, "40");
    setEnrolledProgramCode(student2, "41");
    setEnrolledProgramCode(student3, "40");
    setEnrolledProgramCode(student4, "41");

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_CAREER_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEligibleCareerProgramHeadcountDistrictPerSchool_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var schoolMock2 = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock2));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock2.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool2 = sdcSchoolCollectionRepository.save(sdcMockSchool2);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcMockSchool2);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    sdcSchoolCollectionStudentRepository.save(student4);

    setEnrolledProgramCode(student1, "40");
    setEnrolledProgramCode(student2, "41");
    setEnrolledProgramCode(student3, "40");
    setEnrolledProgramCode(student4, "41");

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_CAREER_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEligibleCareerProgramHeadcountDistrictPerSchool_emptyDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_CAREER_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testSpedHeadcountDistrictReport_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var csfSchoolMock = this.createMockSchool();
    csfSchoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(csfSchoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcCsfMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(csfSchoolMock.getSchoolId()));
    sdcCsfMockSchool.setUploadDate(null);
    sdcCsfMockSchool.setUploadFileName(null);
    sdcCsfMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcCsfMockSchool = sdcSchoolCollectionRepository.save(sdcCsfMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcCsfMockSchool);
    sdcSchoolCollectionStudentRepository.save(student4);

    student1.setSpecialEducationCategoryCode("A");
    student2.setSpecialEducationCategoryCode("B");
    student3.setSpecialEducationCategoryCode("C");
    student4.setSpecialEducationCategoryCode("D");

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_SPECIAL_EDUCATION_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testInclEdCategoryHeadcountDistrictReport_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var csfSchoolMock = this.createMockSchool();
    csfSchoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(csfSchoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcCsfMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(csfSchoolMock.getSchoolId()));
    sdcCsfMockSchool.setUploadDate(null);
    sdcCsfMockSchool.setUploadFileName(null);
    sdcCsfMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcCsfMockSchool = sdcSchoolCollectionRepository.save(sdcCsfMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setIsAdult(true);
    student1.setSpecialEducationCategoryCode("A");
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student2.setSpecialEducationCategoryCode("B");
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student3.setSpecialEducationCategoryCode("C");
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcCsfMockSchool);
    student4.setSpecialEducationCategoryCode("D");
    sdcSchoolCollectionStudentRepository.save(student4);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_SPECIAL_EDUCATION_HEADCOUNT_CATEGORY_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEligibleSpedHeadcountDistrictPerSchool_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var schoolMock2 = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock2));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock2.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool2 = sdcSchoolCollectionRepository.save(sdcMockSchool2);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setSpecialEducationCategoryCode("A");
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setSpecialEducationCategoryCode("B");
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student3.setSpecialEducationCategoryCode("C");
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcMockSchool2);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student4.setSpecialEducationCategoryCode("D");
    sdcSchoolCollectionStudentRepository.save(student4);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_SPECIAL_EDUCATION_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEligibleSpedHeadcountDistrictPerSchool_emptyDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_SPECIAL_EDUCATION_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testIndHeadcountDistrictReport_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var csfSchoolMock = this.createMockSchool();
    csfSchoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(csfSchoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcCsfMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(csfSchoolMock.getSchoolId()));
    sdcCsfMockSchool.setUploadDate(null);
    sdcCsfMockSchool.setUploadFileName(null);
    sdcCsfMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcCsfMockSchool = sdcSchoolCollectionRepository.save(sdcCsfMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setNativeAncestryInd("Y");
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student2.setNativeAncestryInd("Y");
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student3.setNativeAncestryInd("Y");
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcCsfMockSchool);
    student4.setNativeAncestryInd("Y");
    sdcSchoolCollectionStudentRepository.save(student4);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_INDIGENOUS_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEligibleIndHeadcountDistrictPerSchool_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var schoolMock2 = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock2));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock2.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool2 = sdcSchoolCollectionRepository.save(sdcMockSchool2);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setNativeAncestryInd("Y");
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setNativeAncestryInd("Y");
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student3.setNativeAncestryInd("Y");
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcMockSchool2);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student4.setNativeAncestryInd("Y");
    sdcSchoolCollectionStudentRepository.save(student4);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_INDIGENOUS_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testBandDistrictReport_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var csfSchoolMock = this.createMockSchool();
    csfSchoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(csfSchoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcCsfMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(csfSchoolMock.getSchoolId()));
    sdcCsfMockSchool.setUploadDate(null);
    sdcCsfMockSchool.setUploadFileName(null);
    sdcCsfMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcCsfMockSchool = sdcSchoolCollectionRepository.save(sdcCsfMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setNativeAncestryInd("Y");
    student1.setBandCode("0500");
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student2.setNativeAncestryInd("Y");
    student2.setBandCode("0500");
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student3.setNativeAncestryInd("Y");
    student3.setBandCode("0600");
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcCsfMockSchool);
    student4.setNativeAncestryInd("Y");
    student4.setBandCode("0600");
    sdcSchoolCollectionStudentRepository.save(student4);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_BAND_RESIDENCE_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testBandHeadcountDistrict_emptyDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_BAND_RESIDENCE_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testBandDistrictReportPerSchool_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var csfSchoolMock = this.createMockSchool();
    csfSchoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(csfSchoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcCsfMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(csfSchoolMock.getSchoolId()));
    sdcCsfMockSchool.setUploadDate(null);
    sdcCsfMockSchool.setUploadFileName(null);
    sdcCsfMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcCsfMockSchool = sdcSchoolCollectionRepository.save(sdcCsfMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setNativeAncestryInd("Y");
    student1.setBandCode("0500");
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student2.setNativeAncestryInd("Y");
    student2.setBandCode("0500");
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student3.setNativeAncestryInd("Y");
    student3.setBandCode("0600");
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcCsfMockSchool);
    student4.setNativeAncestryInd("Y");
    student4.setBandCode("0600");
    sdcSchoolCollectionStudentRepository.save(student4);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_BAND_RESIDENCE_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testBandHeadcountDistrictPerSchool_emptyDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_BAND_RESIDENCE_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEligibleIndHeadcountDistrictPerSchool_emptyDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_INDIGENOUS_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEllHeadcountDistrictReport_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var csfSchoolMock = this.createMockSchool();
    csfSchoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(csfSchoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcCsfMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(csfSchoolMock.getSchoolId()));
    sdcCsfMockSchool.setUploadDate(null);
    sdcCsfMockSchool.setUploadFileName(null);
    sdcCsfMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcCsfMockSchool = sdcSchoolCollectionRepository.save(sdcCsfMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcCsfMockSchool);
    sdcSchoolCollectionStudentRepository.save(student4);

    student1.setEnrolledGradeCode("17");
    student2.setEnrolledGradeCode("17");
    student3.setEnrolledGradeCode("17");
    student4.setEnrolledGradeCode("17");

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_ELL_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEligibleEllHeadcountDistrictPerSchool_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var schoolMock2 = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock2));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock2.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool2 = sdcSchoolCollectionRepository.save(sdcMockSchool2);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcMockSchool2);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    sdcSchoolCollectionStudentRepository.save(student4);

    student1.setEnrolledGradeCode("17");
    student2.setEnrolledGradeCode("17");
    student3.setEnrolledGradeCode("17");
    student4.setEnrolledGradeCode("17");

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_ELL_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testEligibleEllHeadcountDistrictPerSchool_emptyDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_ELL_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }


  @Test
  void testRefugeePerSchool_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var schoolMock2 = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock2));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock2.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool2 = sdcSchoolCollectionRepository.save(sdcMockSchool2);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setIsSchoolAged(true);
    student1.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
    student1.setFte(new BigDecimal(1.0000));
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setIsSchoolAged(true);
    student2.setIsSchoolAged(true);
    student2.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
    student2.setFte(new BigDecimal(2.0000));
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student3.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student3.setIsSchoolAged(true);
    student3.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
    student3.setFte(new BigDecimal(3.3333));
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcMockSchool2);
    student4.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student4.setIsSchoolAged(true);
    student4.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
    student4.setFte(new BigDecimal(4.2525));
    sdcSchoolCollectionStudentRepository.save(student4);

    setEnrolledProgramCode(student3, EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode());
    setEnrolledProgramCode(student4, EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode());

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_REFUGEE_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testRefugeePerSchool_emptyDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_REFUGEE_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testStudentDifferences_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var schoolMock2 = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock2));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcMockSchool2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock2.getSchoolId()));
    sdcMockSchool2.setUploadDate(null);
    sdcMockSchool2.setUploadFileName(null);
    sdcMockSchool2.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool2 = sdcSchoolCollectionRepository.save(sdcMockSchool2);

    var studentID = UUID.randomUUID();

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setIsSchoolAged(true);
    student1.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
    student1.setFte(new BigDecimal(1.0000));
    student1.setAssignedStudentId(studentID);
    student1.setAssignedPen("123456789");
    student1.setOriginalDemogHash("123");
    student1.setCurrentDemogHash("123");
    student1 = sdcSchoolCollectionStudentRepository.save(student1);
    sdcSchoolCollectionStudentHistoryRepository.save(createSDCSchoolStudentHistory(student1, "ABC"));

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student2.setEnrolledGradeCode(SchoolGradeCodes.GRADE11.getCode());
    student2.setIsSchoolAged(true);
    student2.setIsSchoolAged(true);
    student2.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
    student2.setFte(new BigDecimal(2.0000));
    student2.setOriginalDemogHash("123");
    student2.setCurrentDemogHash("123");
    sdcSchoolCollectionStudentRepository.save(student2);
    sdcSchoolCollectionStudentHistoryRepository.save(createSDCSchoolStudentHistory(student2, "ABC"));

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student3.setEnrolledGradeCode(SchoolGradeCodes.GRADE10.getCode());
    student3.setIsSchoolAged(true);
    student3.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
    student3.setFte(new BigDecimal(3.3333));
    student3.setOriginalDemogHash("123");
    student3.setCurrentDemogHash("123");
    sdcSchoolCollectionStudentRepository.save(student3);
    sdcSchoolCollectionStudentHistoryRepository.save(createSDCSchoolStudentHistory(student3, "ABC"));

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcMockSchool2);
    student4.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student4.setIsSchoolAged(true);
    student4.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
    student4.setFte(new BigDecimal(4.2525));
    student4.setOriginalDemogHash("123");
    student4.setCurrentDemogHash("321");
    sdcSchoolCollectionStudentRepository.save(student4);
    student4.setLegalFirstName("JIMBO");
    sdcSchoolCollectionStudentHistoryRepository.save(createSDCSchoolStudentHistory(student4, "ABC"));

    setEnrolledProgramCode(student3, EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode());
    setEnrolledProgramCode(student4, EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode());

    final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("originalDemogHash,currentDemogHash").operation(FilterOperation.NOT_EQUAL_OTHER_COLUMN).value("NT").valueType(ValueType.STRING).build();

    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());

    final var objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/differences?sdcSchoolCollectionID=" + sdcMockSchool.getSdcSchoolCollectionID()).
                            with(mockAuthority).param("searchCriteriaList", criteriaJSON))
            .andDo(print()).andExpect(status().isOk());


    PaginatedResponse<StudentDifference> paginatedResponse = objectMapper.readValue(
            resultActions1.andReturn().getResponse().getContentAsByteArray(),
            new TypeReference<PaginatedResponse<StudentDifference>>() {}
    );
    val studentDifferences = paginatedResponse.getContent();

    assertEquals(1, studentDifferences.size());
    assertEquals("JIM", studentDifferences.get(0).getCurrentStudent().getLegalFirstName());
    assertEquals("JIMBO", studentDifferences.get(0).getOriginalStudent().getLegalFirstName());
  }

  @Test
  void testZeroFTEHeadcountDistrictPerSchool_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    var csfSchoolMock = this.createMockSchool();
    csfSchoolMock.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(csfSchoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(schoolMock.getSchoolId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcMockSchool = sdcSchoolCollectionRepository.save(sdcMockSchool);

    SdcSchoolCollectionEntity sdcCsfMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(csfSchoolMock.getSchoolId()));
    sdcCsfMockSchool.setUploadDate(null);
    sdcCsfMockSchool.setUploadFileName(null);
    sdcCsfMockSchool.setSdcDistrictCollectionID(sdcMockDistrict.getSdcDistrictCollectionID());
    sdcCsfMockSchool = sdcSchoolCollectionRepository.save(sdcCsfMockSchool);

    SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE09.getCode());
    student1.setIsSchoolAged(true);
    student1.setFte(BigDecimal.ZERO);
    student1.setFteZeroReasonCode(ZeroFteReasonCodes.OFFSHORE.getCode());
    sdcSchoolCollectionStudentRepository.save(student1);

    SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADUATED_ADULT.getCode());
    student1.setIsSchoolAged(false);
    student1.setIsAdult(true);
    student1.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student2);

    SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(sdcMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE07.getCode());
    student3.setIsSchoolAged(true);
    student3.setFte(BigDecimal.ZERO);
    student3.setFteZeroReasonCode(ZeroFteReasonCodes.INACTIVE.getCode());
    sdcSchoolCollectionStudentRepository.save(student3);

    SdcSchoolCollectionStudentEntity student4 = createMockSchoolStudentEntity(sdcCsfMockSchool);
    student1.setEnrolledGradeCode(SchoolGradeCodes.GRADE08.getCode());
    student3.setIsSchoolAged(true);
    student3.setFte(new BigDecimal(1.0));
    sdcSchoolCollectionStudentRepository.save(student4);

    setEnrolledProgramCode(student1, "08");
    setEnrolledProgramCode(student2, "11");
    setEnrolledProgramCode(student3, "14");
    setEnrolledProgramCode(student4, "05");

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_ZERO_FTE_SUMMARY").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testZeroFTEHeadcountDistrictPerSchool_emptyDistrict_ShouldReturnOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    var districtMock = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(districtMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(districtMock.getDistrictId()));
    sdcMockDistrict = sdcDistricCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/sdcDistrictCollection/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_ZERO_FTE_SUMMARY").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  private void setEnrolledProgramCode(SdcSchoolCollectionStudentEntity studentEntity, String enrolledProgram) {
    var enrolledProgramEntity = new SdcSchoolCollectionStudentEnrolledProgramEntity();
    enrolledProgramEntity.setSdcSchoolCollectionStudentEntity(studentEntity);
    enrolledProgramEntity.setEnrolledProgramCode(enrolledProgram);
    enrolledProgramEntity.setCreateUser("ABC");
    enrolledProgramEntity.setUpdateUser("ABC");
    enrolledProgramEntity.setCreateDate(LocalDateTime.now());
    enrolledProgramEntity.setUpdateDate(LocalDateTime.now());
    enrolledProgramRepository.save(enrolledProgramEntity);
  }

  public SdcSchoolCollectionStudentHistoryEntity createSDCSchoolStudentHistory(SdcSchoolCollectionStudentEntity curSdcSchoolStudentEntity, String updateUser) {
    final SdcSchoolCollectionStudentHistoryEntity sdcSchoolCollectionStudentHistoryEntity = new SdcSchoolCollectionStudentHistoryEntity();
    BeanUtils.copyProperties(curSdcSchoolStudentEntity, sdcSchoolCollectionStudentHistoryEntity);
    sdcSchoolCollectionStudentHistoryEntity.setSdcSchoolCollectionStudentID(curSdcSchoolStudentEntity.getSdcSchoolCollectionStudentID());
    sdcSchoolCollectionStudentHistoryEntity.setSdcSchoolCollectionID(curSdcSchoolStudentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID());
    sdcSchoolCollectionStudentHistoryEntity.setCreateUser(updateUser);
    sdcSchoolCollectionStudentHistoryEntity.setCreateDate(LocalDateTime.now());
    sdcSchoolCollectionStudentHistoryEntity.setUpdateUser(updateUser);
    sdcSchoolCollectionStudentHistoryEntity.setUpdateDate(LocalDateTime.now());
    return sdcSchoolCollectionStudentHistoryEntity;
  }
}
