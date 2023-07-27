package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Search;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValueType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.struct.v1.Condition.AND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SdcSchoolCollectionStudentControllerTest extends BaseStudentDataCollectionAPITest {

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
    SdcSchoolCollectionStudentEnrolledProgramRepository sdcSchoolCollectionStudentEnrolledProgramRepository;
    @Autowired
    SdcSchoolCollectionStudentValidationIssueRepository sdcSchoolCollectionStudentValidationIssueRepository;
    @Autowired
    RestUtils restUtils;

    @AfterEach
    void cleanup(){
        sdcSchoolCollectionStudentValidationIssueRepository.deleteAll();
        sdcSchoolCollectionStudentRepository.deleteAll();
        sdcSchoolCollectionRepository.deleteAll();
        collectionRepository.deleteAll();
    }

    @Test
    void testReadSdcSchoolCollectionStudentPaginated_Always_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));
        sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));

        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED+"?pageSize=2")
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andDo(MvcResult::getAsyncResult)
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentPaginated_ministryOwnershipTeamID_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("legalFirstName").operation(FilterOperation.EQUAL).value("JAM").valueType(ValueType.STRING).build();
        final SearchCriteria criteria2 = SearchCriteria.builder().condition(AND).key("gender").operation(FilterOperation.EQUAL).value("M").valueType(ValueType.STRING).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);
        criteriaList.add(criteria2);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentPaginatedCrit_ministryOwnershipTeamID_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        var yesterday = LocalDateTime.now().plusDays(-1);
        var tomorrow = LocalDateTime.now().plusDays(1);
        final SearchCriteria criteriaDate = SearchCriteria.builder().condition(AND).key("createDate").operation(FilterOperation.BETWEEN).value(yesterday + "," + tomorrow).valueType(ValueType.DATE_TIME).build();
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("legalFirstName").operation(FilterOperation.EQUAL).value("JAM").valueType(ValueType.STRING).build();
        final SearchCriteria criteria2 = SearchCriteria.builder().condition(AND).key("gender").operation(FilterOperation.EQUAL).value("M").valueType(ValueType.STRING).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteriaDate);
        criteriaList.add(criteria);
        criteriaList.add(criteria2);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentNotEqPaginatedCrit_ministryOwnershipTeamID_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        var yesterday = LocalDateTime.now().plusDays(-1);
        var tomorrow = LocalDateTime.now().plusDays(1);
        final SearchCriteria criteriaDate = SearchCriteria.builder().condition(AND).key("createDate").operation(FilterOperation.BETWEEN).value(yesterday + "," + tomorrow).valueType(ValueType.DATE_TIME).build();
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("legalFirstName").operation(FilterOperation.NOT_EQUAL).value("JAM").valueType(ValueType.STRING).build();
        final SearchCriteria criteria2 = SearchCriteria.builder().condition(AND).key("gender").operation(FilterOperation.EQUAL).value("M").valueType(ValueType.STRING).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteriaDate);
        criteriaList.add(criteria);
        criteriaList.add(criteria2);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentByID_WithValidPayload_ShouldReturnStatusOkWithValidationissues() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var savedSdcSchoolCollectionStudent = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));

        sdcSchoolCollectionStudentValidationIssueRepository.save(createMockSdcSchoolCollectionStudentValidationIssueEntity(savedSdcSchoolCollectionStudent));

        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + savedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID())
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .contentType(APPLICATION_JSON))
            .andDo(print()).andExpect(status().isOk()).andExpect(
                MockMvcResultMatchers.jsonPath("$.sdcSchoolCollectionStudentValidationIssues",
                    hasSize(1)));

    }

    @Test
    void testReadSdcSchoolCollectionStudentByIDWithEnrolledProgram_WithValidPayload_ShouldReturnStatusOkWithValidationissues() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var savedSdcSchoolCollectionStudent = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));

        var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg.setSdcSchoolCollectionStudentEntity(savedSdcSchoolCollectionStudent);
        enrolledProg.setEnrolledProgramCode("AB");
        enrolledProg.setCreateUser("ABC");
        enrolledProg.setUpdateUser("ABC");
        enrolledProg.setCreateDate(LocalDateTime.now());
        enrolledProg.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg);
        sdcSchoolCollectionStudentValidationIssueRepository.save(createMockSdcSchoolCollectionStudentValidationIssueEntity(savedSdcSchoolCollectionStudent));

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + savedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID())
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .contentType(APPLICATION_JSON))
                .andDo(print()).andExpect(status().isOk()).andExpect(
                        MockMvcResultMatchers.jsonPath("$.sdcSchoolCollectionStudentValidationIssues",
                                hasSize(1))).andExpect(
                        MockMvcResultMatchers.jsonPath("$.sdcSchoolCollectionStudentEnrolledPrograms",
                                hasSize(1)));

    }

    @Test
    void testReadSdcSchoolCollectionStudentNotEqEnrolledProgramsPaginatedCrit_ministryOwnershipTeamID_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);

        var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg.setSdcSchoolCollectionStudentEntity(stud1);
        enrolledProg.setEnrolledProgramCode("BC");
        enrolledProg.setCreateUser("ABC");
        enrolledProg.setUpdateUser("ABC");
        enrolledProg.setCreateDate(LocalDateTime.now());
        enrolledProg.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg);

        var enrolledProg2 = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg2.setSdcSchoolCollectionStudentEntity(stud2);
        enrolledProg2.setEnrolledProgramCode("AB");
        enrolledProg2.setCreateUser("ABC");
        enrolledProg2.setUpdateUser("ABC");
        enrolledProg2.setCreateDate(LocalDateTime.now());
        enrolledProg2.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg2);
        sdcSchoolCollectionStudentValidationIssueRepository.save(createMockSdcSchoolCollectionStudentValidationIssueEntity(stud2));

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcStudentEnrolledProgramEntities.enrolledProgramCode").operation(FilterOperation.NOT_EQUAL).value("AB").valueType(ValueType.STRING).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentInEnrolledProgramsPaginatedCrit_ministryOwnershipTeamID_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);

        var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg.setSdcSchoolCollectionStudentEntity(stud1);
        enrolledProg.setEnrolledProgramCode("BC");
        enrolledProg.setCreateUser("ABC");
        enrolledProg.setUpdateUser("ABC");
        enrolledProg.setCreateDate(LocalDateTime.now());
        enrolledProg.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg);

        var enrolledProg2 = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg2.setSdcSchoolCollectionStudentEntity(stud2);
        enrolledProg2.setEnrolledProgramCode("AB");
        enrolledProg2.setCreateUser("ABC");
        enrolledProg2.setUpdateUser("ABC");
        enrolledProg2.setCreateDate(LocalDateTime.now());
        enrolledProg2.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg2);
        sdcSchoolCollectionStudentValidationIssueRepository.save(createMockSdcSchoolCollectionStudentValidationIssueEntity(stud2));

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcStudentEnrolledProgramEntities.enrolledProgramCode").operation(FilterOperation.IN).value("AB,BC").valueType(ValueType.STRING).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentNotInEnrolledProgramsPaginatedCrit_ministryOwnershipTeamID_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);

        var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg.setSdcSchoolCollectionStudentEntity(stud1);
        enrolledProg.setEnrolledProgramCode("BC");
        enrolledProg.setCreateUser("ABC");
        enrolledProg.setUpdateUser("ABC");
        enrolledProg.setCreateDate(LocalDateTime.now());
        enrolledProg.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg);

        var enrolledProg2 = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg2.setSdcSchoolCollectionStudentEntity(stud2);
        enrolledProg2.setEnrolledProgramCode("AB");
        enrolledProg2.setCreateUser("ABC");
        enrolledProg2.setUpdateUser("ABC");
        enrolledProg2.setCreateDate(LocalDateTime.now());
        enrolledProg2.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg2);
        sdcSchoolCollectionStudentValidationIssueRepository.save(createMockSdcSchoolCollectionStudentValidationIssueEntity(stud2));

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcStudentEnrolledProgramEntities.enrolledProgramCode").operation(FilterOperation.NOT_IN).value("CD,EF").valueType(ValueType.STRING).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentNotInEnrolledProgramsReturnParentsPaginatedCrit_ministryOwnershipTeamID_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        var stud3 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        sdcSchoolCollectionStudentRepository.save(stud3);

        var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg.setSdcSchoolCollectionStudentEntity(stud1);
        enrolledProg.setEnrolledProgramCode("BC");
        enrolledProg.setCreateUser("ABC");
        enrolledProg.setUpdateUser("ABC");
        enrolledProg.setCreateDate(LocalDateTime.now());
        enrolledProg.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg);

        var enrolledProg2 = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg2.setSdcSchoolCollectionStudentEntity(stud2);
        enrolledProg2.setEnrolledProgramCode("AB");
        enrolledProg2.setCreateUser("ABC");
        enrolledProg2.setUpdateUser("ABC");
        enrolledProg2.setCreateDate(LocalDateTime.now());
        enrolledProg2.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg2);
        sdcSchoolCollectionStudentValidationIssueRepository.save(createMockSdcSchoolCollectionStudentValidationIssueEntity(stud2));

        final SearchCriteria criteriaColl = SearchCriteria.builder().condition(AND).key("sdcSchoolCollectionID").operation(FilterOperation.EQUAL).value(sdcMockSchool.getSdcSchoolCollectionID().toString()).valueType(ValueType.UUID).build();
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcStudentEnrolledProgramEntities.enrolledProgramCode").operation(FilterOperation.NOT_IN).value("CD,EF").valueType(ValueType.STRING).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteriaColl);
        criteriaList.add(criteria);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentByID_WithInvalidID_ShouldReturnStatusNotFound() throws Exception {
        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + UUID.randomUUID())
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .contentType(APPLICATION_JSON))
            .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    void ErrorAndWarningCountBySdcSchoolCollectionID_WithoutErrorsAndWarnings_ShouldReturnData() throws Exception {
        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.ERROR_WARNING_COUNT + "/" + UUID.randomUUID())
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .contentType(APPLICATION_JSON))
            .andDo(print())
            .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.warning").value(0));
    }

    @Test
    void ErrorAndWarningCountBySdcSchoolCollectionID_WithErrorsAndWarnings_ShouldReturnData() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        var student = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(school));
        sdcSchoolCollectionStudentValidationIssueRepository.save(createMockSdcSchoolCollectionStudentValidationIssueEntity(student));
        sdcSchoolCollectionStudentValidationIssueRepository.save(createMockSdcSchoolCollectionStudentValidationIssueEntity(student));
        var warningValidationIssue = createMockSdcSchoolCollectionStudentValidationIssueEntity(student);
        warningValidationIssue.setValidationIssueSeverityCode("WARNING");
        sdcSchoolCollectionStudentValidationIssueRepository.save(warningValidationIssue);

        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.ERROR_WARNING_COUNT + "/" + school.getSdcSchoolCollectionID())
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .contentType(APPLICATION_JSON))
            .andDo(print())
            .andExpect(MockMvcResultMatchers.jsonPath("$.error").value(1)) //should only show 1 error since it is for the same validationIssueCode
            .andExpect(MockMvcResultMatchers.jsonPath("$.warning").value(1));
    }

    @Test
    void testUpdateAndValidateSdcSchoolCollectionStudent_StudentWithInvalidPENInvalidDOB_ShouldReturnStatusBAD_REQUEST() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setEnrolledProgramCodes("1011121314151617");
        entity.setStudentPen("12345678");
        entity.setDob("01022027");
        this.sdcSchoolCollectionStudentRepository.save(entity);

        MvcResult apiResponse = this.mockMvc.perform(
                        put(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + entity.getSdcSchoolCollectionStudentID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("subErrors");
        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("Invalid Student Pen.");
        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("Invalid DOB.");
    }

    @Test
    void testUpdateAndValidateSdcSchoolCollectionStudent_StudentWithInvalidCodes_ShouldReturnStatusBAD_REQUEST() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setEnrolledProgramCodes("1011121314151617");
        entity.setSpecialEducationCategoryCode("A");
        entity.setSchoolFundingCode("12");
        entity.setHomeLanguageSpokenCode("11");
        entity.setEnrolledGradeCode("00");
        entity.setCareerProgramCode("80");
        entity.setBandCode("X");
        this.sdcSchoolCollectionStudentRepository.save(entity);

        MvcResult apiResponse = this.mockMvc.perform(
                        put(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + entity.getSdcSchoolCollectionStudentID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("subErrors");
        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("Invalid School Funding Code.");
        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("Invalid Home Language Spoken Code.");
        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("Invalid Enrolled Grade Code.");
        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("Invalid Career Program Code.");
        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("Invalid Band Code.");
    }

    @Test
    void testUpdateAndValidateSdcSchoolCollectionStudent_StudentWithValidationErr_ShouldUpdateStatusToERRORAndReturnStatusOk() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setEnrolledProgramCodes("1011121314151617");
        this.sdcSchoolCollectionStudentRepository.save(entity);

        this.mockMvc.perform(
                        put(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + entity.getSdcSchoolCollectionStudentID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val curStudentEntity = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(curStudentEntity).isPresent();
        var studentEntity = curStudentEntity.get();
        assertThat(studentEntity.getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.ERROR.toString());
    }

    @Test
    void testUpdateAndValidateSdcSchoolCollectionStudent_StudentWithValidationErr_ShouldUpdateStatusToLOADEDAndReturnStatusOk() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        this.sdcSchoolCollectionStudentRepository.save(entity);

        this.mockMvc.perform(
                        put(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + entity.getSdcSchoolCollectionStudentID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val curStudentEntity = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(curStudentEntity).isPresent();
        var studentEntity = curStudentEntity.get();
        assertThat(studentEntity.getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.LOADED.toString());
    }
}
