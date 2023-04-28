package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class CodeTableControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  EnrolledProgramCodeRepository enrolledProgramCodeRepository;
  @Autowired
  CareerProgramCodeRepository careerProgramCodeRepository;
  @Autowired
  HomeLanguageSpokenCodeRepository homeLanguageSpokenCodeRepository;
  @Autowired
  BandCodeRepository bandCodeRepository;
  @Autowired
  FundingCodeRepository fundingCodeRepository;
  @Autowired
  EnrolledGradeCodeRepository enrolledGradeCodeRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  protected final static ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @BeforeEach
  public void before() {
    enrolledProgramCodeRepository.save(this.createEnrolledProgramCodeData());
    careerProgramCodeRepository.save(this.createCareerProgramCodeData());
    homeLanguageSpokenCodeRepository.save(this.homeLanguageSpokenCodeData());
    bandCodeRepository.save(this.bandCodeData());
    fundingCodeRepository.save(this.fundingCodeData());
    enrolledGradeCodeRepository.save(this.enrolledGradeCodeData());
  }
  @AfterEach
  public void after() {
    enrolledProgramCodeRepository.deleteAll();
    careerProgramCodeRepository.deleteAll();
    homeLanguageSpokenCodeRepository.deleteAll();
    bandCodeRepository.deleteAll();
    fundingCodeRepository.deleteAll();
    enrolledGradeCodeRepository.deleteAll();
  }

  private EnrolledProgramCodeEntity createEnrolledProgramCodeData() {
    return EnrolledProgramCodeEntity.builder().enrolledProgramCode("05").description("Programme Francophone")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Francophone").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }
  private CareerProgramCodeEntity createCareerProgramCodeData() {
    return CareerProgramCodeEntity.builder().careerProgramCode("XA").description("Business")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Business").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }
  private HomeLanguageSpokenCodeEntity homeLanguageSpokenCodeData() {
    return HomeLanguageSpokenCodeEntity.builder().homeLanguageSpokenCode("001").description("Portuguese")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Portuguese").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }
  private BandCodeEntity bandCodeData() {
    return BandCodeEntity.builder().bandCode("0500").description("KWANLIN DUN")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("KWANLIN DUN").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }
  private SchoolFundingCodeEntity fundingCodeData() {
    return SchoolFundingCodeEntity.builder().schoolFundingCode("14").description("OUT-OF-PROVINCE/INTERNATIONAL")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("OUT-OF-PROVINCE/INTERNATIONAL").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }
  private EnrolledGradeCodeEntity enrolledGradeCodeData() {
    return EnrolledGradeCodeEntity.builder().enrolledGradeCode("01").description("Grade 1")
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("Grade 1").createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }


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