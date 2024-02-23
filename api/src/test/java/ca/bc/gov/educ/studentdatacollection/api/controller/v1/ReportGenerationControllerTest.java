package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentEnrolledProgramRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  @Autowired
  RestUtils restUtils;

  @Autowired
  SdcSchoolCollectionStudentEnrolledProgramRepository sdcSchoolCollectionStudentEnrolledProgramRepository;

  @BeforeEach
  public void before() {
  }

  @AfterEach
  public void after() {
    this.collectionRepository.deleteAll();
  }

  @Test
  void testGetCollectionByID_ShouldReturnCollection() throws Exception {
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
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
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
        get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockSchool.getSdcSchoolCollectionID() + "/GRADE_ENROLLMENT_FTE").with(mockAuthority))
      .andDo(print()).andExpect(status().isOk());
  }

  @Test
  void testGetCollectionByID_ShouldReturnBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    School school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockSchool.getSdcSchoolCollectionID() + "/ABC").with(mockAuthority))
            .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  void testGetCollectionByID_MissingDistrict_ShouldReturn500Exception() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.empty());

    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    School school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockSchool.getSdcSchoolCollectionID() + "/GRADE_ENROLLMENT_FTE").with(mockAuthority))
            .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testGetCollectionByID_MissingSchool_ShouldReturn500Exception() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    var district = this.createMockDistrict();
    when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));

    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.empty());

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    School school = createMockSchool();
    SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
    sdcMockSchool.setUploadDate(null);
    sdcMockSchool.setUploadFileName(null);
    sdcSchoolCollectionRepository.save(sdcMockSchool);

    this.mockMvc.perform(
                    get(URL.BASE_URL_REPORT_GENERATION + "/" + sdcMockSchool.getSdcSchoolCollectionID() + "/GRADE_ENROLLMENT_FTE").with(mockAuthority))
            .andDo(print()).andExpect(status().isNotFound());
  }

}
