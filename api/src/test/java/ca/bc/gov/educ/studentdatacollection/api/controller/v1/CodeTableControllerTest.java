package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class CodeTableControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;

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
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].bandCode").value("0500"));
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
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].enrolledGradeCode").value("01"));
  }

}