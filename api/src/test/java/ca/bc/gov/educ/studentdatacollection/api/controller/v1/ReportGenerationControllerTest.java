package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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

    School school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
        get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockSchool.getSdcSchoolCollectionID() + "/" + reportTypeCode).with(mockAuthority))
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

    School school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockSchool.getSdcSchoolCollectionID() + "/FRENCH_HEADCOUNT").with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testGetGradeEnrollmentHeadcountReport_ShouldReturnBadRequest() throws Exception {
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
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockSchool.getSdcSchoolCollectionID() + "/ABC").with(mockAuthority))
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

    School school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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

    School school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockSchool.getSdcSchoolCollectionID() + "/" + "ALL_STUDENT_SCHOOL_CSV").with(mockAuthority))
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
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "ALL_STUDENT_DIS_CSV").with(mockAuthority))
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
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_FRENCH_HEADCOUNT").with(mockAuthority))
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
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_FRENCH_HEADCOUNT_PER_SCHOOL").with(mockAuthority))
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
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockDistrict.getSdcDistrictCollectionID() + "/" + "DIS_GRADE_ENROLLMENT_HEADCOUNT").with(mockAuthority))
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
}
