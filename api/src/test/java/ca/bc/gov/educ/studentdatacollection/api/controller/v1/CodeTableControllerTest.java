package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.BandCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.BandCode;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class CodeTableControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  BandCodeRepository bandCodeRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  protected final static ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @Test
  void testGetAllEnrolledPrograms_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.ENROLLED_PROGRAM_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].enrolledProgramCode").value("05"));
  }

  @Test
  void testGetAllCareerPrograms_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.CAREER_PROGRAM_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].careerProgramCode").value("XA"));
  }

  @Test
  void testGetAllHomeLanguageSpoken_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.HOME_LANGUAGE_SPOKEN_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].homeLanguageSpokenCode").value("001"));
  }

  @Test
  void testGetAllBandCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.BAND_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].bandCode").value("0500"));
  }

  @Test
  void testGetAllFundingCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.FUNDING_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].schoolFundingCode").value("14"));
  }
  @Test
  void testGetAllEnrolledGradeCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.GRADE_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].enrolledGradeCode").value("KH"));
  }

  @Test
  void testGetAllGenderCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.GENDER_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].genderCode").value("M"));
  }

  @Test
  void testGetAllValidationIssueCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.VALIDATION_ISSUE_TYPE_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].validationIssueTypeCode").value("GENDERINVALID"))
        .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
  }

  @Test
  void testGetAllFundingGroups_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.FUNDING_GROUP_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].schoolFundingGroupCode").value("GROUP1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
  }

  @Test
  void testGetZeroFteReasonCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.ZERO_FTE_REASON_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].fteZeroReasonCode").value("TOOYOUNG"))
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
  }

  @Test
  void testGetCollectionTypeCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.COLLECTION_TYPE_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].collectionTypeCode").value("SEPTEMBER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
  }

  @Test
  void testGetProgramEligibilityIssueCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.PROGRAM_ELIGIBILITY_ISSUE_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].programEligibilityIssueTypeCode").value("HOMESCHOOL"))
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
  }

  @Test
  void testGetDuplicateResolutionCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.DUPLICATE_RESOLUTION_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].duplicateResolutionCode").value("RELEASED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
  }

  @Test
  void testGetProgramDuplicateTypeCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.PROGRAM_DUPLICATE_TYPE_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].programDuplicateTypeCode").value("SPECIAL_ED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
  }

  @Test
  void testGetSdcSchoolCollectionStatusCodes_ShouldReturnCodes() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_COLLECTION_CODES";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    this.mockMvc.perform(get(URL.BASE_URL + URL.SDC_SCHOOL_COLLECTION_STATUS_CODES).with(mockAuthority)).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].sdcSchoolCollectionStatusCode").value("NEW"))
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
  }

  @Test
  void testUpdateBandCode_ShouldReturnBandCode() throws Exception {
    BandCode bandCode = new BandCode();
    bandCode.setBandCode("0600");
    bandCode.setLabel("NEW BAND");
    bandCode.setDescription("NEW BAND DESC");
    bandCode.setDisplayOrder(1);
    bandCode.setEffectiveDate(LocalDateTime.now().toString());
    bandCode.setExpiryDate(LocalDateTime.now().toString());
    bandCode.setUpdateUser("TESTACCNT");

    this.mockMvc.perform(put(URL.BASE_URL + URL.BAND_CODES)
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_COLLECTION_CODES")))
            .content(JsonUtil.getJsonStringFromObject(bandCode))
            .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    var updatedBand = bandCodeRepository.findById(bandCode.getBandCode());
    assertThat(updatedBand).isPresent();
    assertThat(updatedBand.get().getLabel()).isEqualTo("NEW BAND");
  }

  @Test
  void testUpdateBandCode_InvalidBand_ShouldReturnBadRequest() throws Exception {
    BandCode bandCode = new BandCode();
    bandCode.setBandCode("8888");
    bandCode.setLabel("NEW BAND");
    bandCode.setDescription("NEW BAND DESC");
    bandCode.setDisplayOrder(1);
    bandCode.setEffectiveDate(LocalDateTime.now().toString());
    bandCode.setExpiryDate(LocalDateTime.now().toString());
    bandCode.setUpdateUser("TESTACCNT");

    this.mockMvc.perform(put(URL.BASE_URL + URL.BAND_CODES)
            .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_COLLECTION_CODES")))
            .content(JsonUtil.getJsonStringFromObject(bandCode))
            .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }
}
