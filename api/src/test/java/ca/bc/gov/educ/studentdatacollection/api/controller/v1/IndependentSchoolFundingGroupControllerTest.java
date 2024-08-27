package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.IndependentSchoolFundingGroupSnapshotRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {StudentDataCollectionApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class IndependentSchoolFundingGroupControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private RestUtils restUtils;
  @Autowired
  IndependentSchoolFundingGroupSnapshotController independentSchoolFundingGroupController;
  @Autowired
  IndependentSchoolFundingGroupSnapshotRepository independentSchoolFundingGroupSnapshotRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  protected final static ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @Test
  void testGetSchoolFundingGroup_WithWrongID_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SCHOOL_FUNDING_GROUP_SNAPSHOT";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc.perform(get(URL.BASE_URL_SCHOOL_FUNDING + "/" + UUID.randomUUID()).with(mockAuthority))
        .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  void testGetSchoolFundingGroupByCollectionAndSchoolIDs_ShouldReturnCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SCHOOL_FUNDING_GROUP_SNAPSHOT";
    final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    final var independentSchoolFundingGroupEntity = this.independentSchoolFundingGroupSnapshotRepository.save(this.createMockIndependentSchoolFundingGroupSnapshotEntity(UUID.randomUUID(), UUID.randomUUID()));

    var resultActions = this.mockMvc.perform(
                    get(URL.BASE_URL_SCHOOL_FUNDING + "/" + independentSchoolFundingGroupEntity.getSchoolID() + "/" + independentSchoolFundingGroupEntity.getCollectionID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List>() {
    });

    assertThat(summary).hasSize(1);

    var independentSchoolFundingGroupEntity1 = this.createMockIndependentSchoolFundingGroupSnapshotEntity(independentSchoolFundingGroupEntity.getSchoolID(), independentSchoolFundingGroupEntity.getCollectionID());
    independentSchoolFundingGroupEntity1.setSchoolGradeCode("GRADE02");
    this.independentSchoolFundingGroupSnapshotRepository.save(independentSchoolFundingGroupEntity1);

    var resultActions1 = this.mockMvc.perform(
                    get(URL.BASE_URL_SCHOOL_FUNDING + "/" + independentSchoolFundingGroupEntity.getSchoolID() + "/" + independentSchoolFundingGroupEntity.getCollectionID()).with(mockAuthority))
            .andDo(print()).andExpect(status().isOk());

    val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List>() {
    });

    assertThat(summary1).hasSize(2);
  }

}
