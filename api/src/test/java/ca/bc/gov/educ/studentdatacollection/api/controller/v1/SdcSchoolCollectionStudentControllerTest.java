package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportingRequirementCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcStudentEllMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.grad.v1.GradStatusResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Search;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValueType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ca.bc.gov.educ.studentdatacollection.api.struct.v1.Condition.AND;
import static ca.bc.gov.educ.studentdatacollection.api.struct.v1.Condition.OR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

    @Autowired
    SdcStudentEllRepository sdcStudentEllRepository;

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
    void testReadSdcSchoolCollectionStudentPaginated_withSdcStudentEll_ShouldReturnStatusOkWithEll() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(
            collection,
            UUID.fromString(school.getSchoolId()),
            UUID.fromString(school.getDistrictId())
        );
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity otherStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        otherStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity studentWithEll = sdcSchoolCollectionStudentRepository.save(mockStudentEntity);
        sdcSchoolCollectionStudentRepository.save(otherStudentEntity);
        SdcStudentEllEntity ellEntity = this.createMockStudentEllEntity(studentWithEll);
        ellEntity.setYearsInEll(5);

        ellEntity = this.sdcStudentEllRepository.save(ellEntity);

        final MvcResult result = this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED+"?pageSize=2")
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .contentType(APPLICATION_JSON))
            .andDo(print())
            .andDo(MvcResult::getAsyncResult)
            .andReturn();

        String yearsInEll = "$.content[?(@.assignedStudentId=='"
            + studentWithEll.getAssignedStudentId().toString()
            + "')].sdcStudentEll.yearsInEll";
        this.mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath(yearsInEll).value("5"));
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

        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
            savedSdcSchoolCollectionStudent, StudentValidationIssueSeverityCode.ERROR
          ));

        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + savedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID())
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .contentType(APPLICATION_JSON))
            .andDo(print()).andExpect(status().isOk()).andExpect(
                jsonPath("$.sdcSchoolCollectionStudentValidationIssues",
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
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
            savedSdcSchoolCollectionStudent, StudentValidationIssueSeverityCode.ERROR
          ));

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + savedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID())
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .contentType(APPLICATION_JSON))
                .andDo(print()).andExpect(status().isOk()).andExpect(
                        jsonPath("$.sdcSchoolCollectionStudentValidationIssues",
                                hasSize(1))).andExpect(
                        jsonPath("$.sdcSchoolCollectionStudentEnrolledPrograms",
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
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                stud2, StudentValidationIssueSeverityCode.ERROR));

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
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                stud2, StudentValidationIssueSeverityCode.ERROR));

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
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                stud2, StudentValidationIssueSeverityCode.ERROR));

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
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                stud2, StudentValidationIssueSeverityCode.ERROR));

        final SearchCriteria criteriaColl = SearchCriteria.builder().condition(AND).key("sdcSchoolCollection.sdcSchoolCollectionID").operation(FilterOperation.EQUAL).value(sdcMockSchool.getSdcSchoolCollectionID().toString()).valueType(ValueType.UUID).build();
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
            .andExpect(status().isOk());
    }

    @Test
    void ErrorAndWarningCountBySdcSchoolCollectionID_WithErrorsAndWarnings_ShouldReturnData() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        var studentOne = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(school));
        var studentTwo = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(school));
        var studentThree = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(school));

        var deletedStudent = createMockSchoolStudentEntity(school);
        deletedStudent.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.toString());
        sdcSchoolCollectionStudentRepository.save(deletedStudent);

        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                studentOne, StudentValidationIssueSeverityCode.ERROR));
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                studentTwo, StudentValidationIssueSeverityCode.ERROR));
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                studentThree, StudentValidationIssueSeverityCode.ERROR));
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                studentThree, StudentValidationIssueSeverityCode.ERROR));
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                studentTwo, StudentValidationIssueSeverityCode.INFO_WARNING));
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                studentOne, StudentValidationIssueSeverityCode.INFO_WARNING));
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                studentOne, StudentValidationIssueSeverityCode.FUNDING_WARNING));
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                studentThree, StudentValidationIssueSeverityCode.FUNDING_WARNING));

        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                deletedStudent, StudentValidationIssueSeverityCode.ERROR));
        sdcSchoolCollectionStudentValidationIssueRepository
          .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                deletedStudent, StudentValidationIssueSeverityCode.FUNDING_WARNING));

        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.ERROR_WARNING_COUNT + "/"
                        + school.getSdcSchoolCollectionID())
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .contentType(APPLICATION_JSON))
            .andDo(print())
                .andExpect(jsonPath("$.[0]severityCode", equalTo("ERROR")))
                .andExpect(jsonPath("$.[0]total", equalTo(3)))
                .andExpect(jsonPath("$.[1]severityCode", equalTo("FUNDING_WARNING")))
                .andExpect(jsonPath("$.[1]total", equalTo(2)))
                .andExpect(jsonPath("$.[2]severityCode", equalTo("INFO_WARNING")))
                .andExpect(jsonPath("$.[2]total", equalTo(2)));
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
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT)
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
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT)
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
    void testUpdateAndValidateSdcSchoolCollectionStudent_StudentWithValidationErr_ShouldUpdateStatusToERRORAndReturnBadRequest() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setEnrolledProgramCodes("1011121314151617");
        entity.setPostalCode(null);

        this.sdcSchoolCollectionStudentRepository.save(entity);

        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT)
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isBadRequest());
    }

    @Test
    void testCreateStudent_StudentWithCollectionId_ShouldReturnStatusOk() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
        when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.LOADED.toString());
        entity.setCareerProgramCode(null);
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setPostalCode(null);

        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT)
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());
    }

    @Test
    void testCreateStudent_StudentWithNoCollectionId_ShouldReturnBadRequest() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        val entity = this.createMockSchoolStudentEntity(null);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.LOADED.toString());
        entity.setCareerProgramCode(null);
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setPostalCode(null);

        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT)
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateAndValidateSdcSchoolCollectionStudent_StudentWithValidationErr_ShouldUpdateStatusToLOADEDAndReturnStatusOk() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
        when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

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
        entity.setNumberOfCourses("0400");
        this.sdcSchoolCollectionStudentRepository.save(entity);
        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT)
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val curStudentEntity = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(curStudentEntity).isPresent();
        var studentEntity = curStudentEntity.get();
        assertThat(studentEntity.getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.VERIFIED.toString());
    }

    @Test
    void testUpdateAndValidateSdcSchoolCollectionStudent_StudentWithValidationWarning_ShouldUpdateStatusToLOADEDAndReturnStatusOk() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
        when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

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
        entity.setPostalCode(null);
        this.sdcSchoolCollectionStudentRepository.save(entity);

        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT)
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val curStudentEntity = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(curStudentEntity).isPresent();
        var studentEntity = curStudentEntity.get();
        assertThat(studentEntity.getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.INFO_WARNING.getCode());
    }

    @Test
    void testUpdateAndValidateSdcSchoolCollectionStudent_updateDOBToError_ShouldReturnStudentAndNotSaveToDatabaseAndReturnWithValidationIssues() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
            grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
        when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setNumberOfCourses("0400");
        entity.setEnrolledGradeCode("01");
        this.sdcSchoolCollectionStudentRepository.save(entity);

        String dob = "19800101";
        entity.setDob(dob);

        this.mockMvc.perform(
                post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT)
                    .contentType(APPLICATION_JSON)
                    .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                    .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dob", equalTo(dob)))
            .andExpect(jsonPath("$.sdcSchoolCollectionStudentValidationIssues", hasSize(2)))
            .andExpect(jsonPath("$.sdcSchoolCollectionStudentStatusCode", equalTo(SdcSchoolStudentStatus.ERROR.toString())));

        val curStudentEntity = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(curStudentEntity).isPresent();
        var studentEntity = curStudentEntity.get();
        assertThat(studentEntity.getDob()).isNotEqualTo(dob); //verify that the DOB didn't get updated in the DB.
    }

    @Test
    void testUpdateAndValidateSdcSchoolCollectionStudent_withWarning_ShouldReturnStudentAndSaveToDatabaseAndReturnStatusOk() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
            grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
        when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setNumberOfCourses("0400");
        this.sdcSchoolCollectionStudentRepository.save(entity);

        entity.setPostalCode("");

        this.mockMvc.perform(
                post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT )
                    .contentType(APPLICATION_JSON)
                    .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                    .with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sdcSchoolCollectionStudentValidationIssues", hasSize(1)))
            .andExpect(jsonPath("$.sdcSchoolCollectionStudentStatusCode", equalTo(SdcSchoolStudentStatus.INFO_WARNING.toString())));

        val curStudentEntity = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(curStudentEntity).isPresent();
        var studentEntity = curStudentEntity.get();
        assertThat(studentEntity.getPostalCode()).isEmpty();
    }

    @Test
    void testFindAll_multipleOrCriteriaAndAnAndCriteriaCombined_ShouldReturnStatusOk() throws Exception {
        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId())));


        final var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).collect(Collectors.toList());
        models.forEach(entity -> entity.setSdcSchoolCollection(sdcSchoolCollectionEntity));
        this.sdcSchoolCollectionStudentRepository.saveAll(models);
        final SearchCriteria criteria = SearchCriteria.builder().key("enrolledGradeCode").operation(FilterOperation.IN).value("01,02").valueType(ValueType.STRING).build();
        final List<SearchCriteria> criteriaList1 = new ArrayList<>();
        criteriaList1.add(criteria);
        final SearchCriteria criteria2 = SearchCriteria.builder().key("legalFirstName").operation(FilterOperation.CONTAINS).value("OLIVIA").valueType(ValueType.STRING).build();
        final SearchCriteria criteria3 = SearchCriteria.builder().key("usualFirstName").condition(OR).operation(FilterOperation.CONTAINS).value("OLIVIA").valueType(ValueType.STRING).build();
        final SearchCriteria criteria4 = SearchCriteria.builder().key("legalMiddleNames").condition(OR).operation(FilterOperation.CONTAINS).value("OLIVIA").valueType(ValueType.STRING).build();
        final SearchCriteria criteria5 = SearchCriteria.builder().key("usualMiddleNames").condition(OR).operation(FilterOperation.CONTAINS).value("OLIVIA").valueType(ValueType.STRING).build();
        final SearchCriteria criteria6 = SearchCriteria.builder().key("legalLastName").condition(OR).operation(FilterOperation.CONTAINS).value("OLIVIA").valueType(ValueType.STRING).build();
        final SearchCriteria criteria7 = SearchCriteria.builder().key("usualLastName").condition(OR).operation(FilterOperation.CONTAINS).value("OLIVIA").valueType(ValueType.STRING).build();
        final SearchCriteria criteria8 = SearchCriteria.builder().key("studentPen").condition(OR).operation(FilterOperation.CONTAINS).value("OLIVIA").valueType(ValueType.STRING).build();
        final SearchCriteria criteria9 = SearchCriteria.builder().key("localID").condition(OR).operation(FilterOperation.CONTAINS).value("OLIVIA").valueType(ValueType.STRING).build();
        final List<SearchCriteria> criteriaList2 = new ArrayList<>();
        criteriaList2.add(criteria2);
        criteriaList2.add(criteria3);
        criteriaList2.add(criteria4);
        criteriaList2.add(criteria5);
        criteriaList2.add(criteria6);
        criteriaList2.add(criteria7);
        criteriaList2.add(criteria8);
        criteriaList2.add(criteria9);
        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList1).build());
        searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList2).build());
        final ObjectMapper objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.PAGINATED)
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void testDeleteSdcSchoolCollectionStudent_WithValidPayload_ShouldReturnOkay() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_DELETE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
        var savedSdcSchoolCollectionStudent = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));

        this.mockMvc.perform(
                delete(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + savedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk()).andExpect(
                        jsonPath("$.sdcSchoolCollectionStudentStatusCode", equalTo(SdcSchoolStudentStatus.DELETED.toString()))
                );

        var deletedSdcSchoolCollectionStudent = sdcSchoolCollectionStudentRepository.findById(savedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID());
        assertThat(deletedSdcSchoolCollectionStudent).isPresent();
        assertThat(deletedSdcSchoolCollectionStudent.get().getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.DELETED.toString());
    }

    @Test
    void testDeleteMultiSdcSchoolCollectionStudent_WithValidPayload_ShouldReturnOkay() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_DELETE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
        var savedSdcSchoolCollectionStudent = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));
        var savedSdcSchoolCollectionStudent2 = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));

        String payload = asJsonString(Arrays.asList(savedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID(), savedSdcSchoolCollectionStudent2.getSdcSchoolCollectionStudentID()));
        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/soft-delete-students")
                                .contentType(APPLICATION_JSON)
                                .content(payload)
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk());

        var deletedSdcSchoolCollectionStudent = sdcSchoolCollectionStudentRepository.findById(savedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID());
        assertThat(deletedSdcSchoolCollectionStudent).isPresent();
        assertThat(deletedSdcSchoolCollectionStudent.get().getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.DELETED.toString());
        var deletedSdcSchoolCollectionStudent2 = sdcSchoolCollectionStudentRepository.findById(savedSdcSchoolCollectionStudent2.getSdcSchoolCollectionStudentID());
        assertThat(deletedSdcSchoolCollectionStudent2).isPresent();
        assertThat(deletedSdcSchoolCollectionStudent2.get().getSdcSchoolCollectionStudentStatusCode()).isEqualTo(SdcSchoolStudentStatus.DELETED.toString());
    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_enrollmentHeadcounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
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
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                    }
                    return models.get(i);
                })
                .toList();

        sdcSchoolCollectionStudentRepository.saveAll(students);

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "enrollment")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Student Headcount")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['All Students'].currentValue", equalTo("2")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['All Students'].comparisonValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Adult'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['School Aged'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Under School Aged'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Grade Headcount")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.11.currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.11.comparisonValue", equalTo("1")));

    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcountsForINDEPENDSchool_enrollmentHeadcounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        school.setSchoolCategoryCode("INDEPEND");
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
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
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                    }
                    return models.get(i);
                })
                .toList();

        sdcSchoolCollectionStudentRepository.saveAll(students);

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "enrollment")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountResultsTable.headers", not(hasItemInArray("KH"))));

    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_frenchHeadcounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        school.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.REGULAR.getCode());
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
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
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                    }
                    return models.get(i);
                })
                .toList();

        var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        savedStudents.forEach(student -> {
            var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
            enrolledProg.setSdcSchoolCollectionStudentEntity(student);
            enrolledProg.setEnrolledProgramCode("14");
            enrolledProg.setCreateUser("ABC");
            enrolledProg.setUpdateUser("ABC");
            enrolledProg.setCreateDate(LocalDateTime.now());
            enrolledProg.setUpdateDate(LocalDateTime.now());
            enrolledPrograms.add(enrolledProg);
        });


        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "french")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Core French")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Not Reported'].comparisonValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Early French Immersion")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Not Reported'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("0")));
    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcountsForINDP_FNSSchool_frenchHeadcounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        school.setSchoolCategoryCode("INDP_FNS");
        school.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.REGULAR.getCode());
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
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
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                    }
                    return models.get(i);
                })
                .toList();

        var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        savedStudents.forEach(student -> {
            var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
            enrolledProg.setSdcSchoolCollectionStudentEntity(student);
            enrolledProg.setEnrolledProgramCode("14");
            enrolledProg.setCreateUser("ABC");
            enrolledProg.setUpdateUser("ABC");
            enrolledProg.setCreateDate(LocalDateTime.now());
            enrolledProg.setUpdateDate(LocalDateTime.now());
            enrolledPrograms.add(enrolledProg);
        });


        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "french")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountResultsTable.headers", not(hasItemInArray("KH"))));

    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_csfFrenchHeadcounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
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
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                    }
                    return models.get(i);
                })
                .toList();

        var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        savedStudents.forEach(student -> {
            var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
            enrolledProg.setSdcSchoolCollectionStudentEntity(student);
            enrolledProg.setEnrolledProgramCode("14");
            enrolledProg.setCreateUser("ABC");
            enrolledProg.setUpdateUser("ABC");
            enrolledProg.setCreateDate(LocalDateTime.now());
            enrolledProg.setUpdateDate(LocalDateTime.now());
            enrolledPrograms.add(enrolledProg);
        });


        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "french")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Programme Francophone")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Not Reported'].comparisonValue", equalTo("4")));

    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_careerHeadcounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
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
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                    }
                    return models.get(i);
                })
                .toList();

        var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        savedStudents.forEach(student -> {
            var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
            enrolledProg.setSdcSchoolCollectionStudentEntity(student);
            enrolledProg.setEnrolledProgramCode("40");
            enrolledProg.setCreateUser("ABC");
            enrolledProg.setUpdateUser("ABC");
            enrolledProg.setCreateDate(LocalDateTime.now());
            enrolledProg.setUpdateDate(LocalDateTime.now());
            enrolledPrograms.add(enrolledProg);
        });


        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "career")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Career Preparation")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Not Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Co-Operative Education")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Not Reported'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("0")));
    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcountsWithComparisonToPreviousYear_ellHeadCounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        //Current year's collection for the school.
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        //Second school is for the previous year's collections.
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
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
                var ellEntity = new SdcStudentEllEntity();

                student.setAssignedStudentId(studentId);
                //Even students go to the previous year; odd students to the current year.
                if (i % 2 == 0) {
                    student.setSdcSchoolCollection(secondSchool);
                    ellEntity.setYearsInEll(4);
                } else {
                    student.setSdcSchoolCollection(firstSchool);
                }
                if (i == 1) {
                  ellEntity.setYearsInEll(0);
                  student.setEnrolledProgramCodes("9876543210");
                  student.setEllNonEligReasonCode(ProgramEligibilityIssueCode.NOT_ENROLLED_ELL.getCode());
                }
                if (i == 3) {
                  student.setEnrolledProgramCodes("9876543217");
                  student.setEllNonEligReasonCode(ProgramEligibilityIssueCode.HOMESCHOOL.getCode());
                }
                if (i == 5) {
                    student.setEnrolledProgramCodes("9876543217");
                    ellEntity.setYearsInEll(6);
                }
                if (i == 7) {
                    student.setEnrolledProgramCodes("9876543217");
                    ellEntity.setYearsInEll(4);
                }

                ellEntity.setCreateUser("ABC");
                ellEntity.setUpdateUser("ABC");
                ellEntity.setCreateDate(LocalDateTime.now());
                ellEntity.setUpdateDate(LocalDateTime.now());
                ellEntity.setStudentID(student.getAssignedStudentId());
                sdcStudentEllRepository.save(ellEntity);

                return student;
            })
        .toList();

        var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        //All of the previous year students will be reported to an ELL program.
        savedStudents.forEach(student -> {
            if (!StringUtils.equals(
                ProgramEligibilityIssueCode.NOT_ENROLLED_ELL.getCode(),
                student.getEllNonEligReasonCode()
            )) {
                var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
                enrolledProg.setEnrolledProgramCode("17");
                enrolledProg.setSdcSchoolCollectionStudentEntity(student);
                enrolledProg.setCreateUser("ABC");
                enrolledProg.setUpdateUser("ABC");
                enrolledProg.setCreateDate(LocalDateTime.now());
                enrolledProg.setUpdateDate(LocalDateTime.now());
                enrolledPrograms.add(enrolledProg);
            }
        });

        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .param("type", "ell")
                .param("compare", "true")
                .contentType(APPLICATION_JSON))
            .andDo(print())
            .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("English Language Learners")))
            .andExpect(jsonPath("$.headcountHeaders[0].orderedColumnTitles", containsInRelativeOrder("Eligible", "Reported", "Not Reported")))
            .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("2")))
            .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].currentValue", equalTo("3")))
            .andExpect(jsonPath("$.headcountHeaders[0].columns.['Not Reported'].currentValue", equalTo("1")))
            .andExpect(jsonPath("$.headcountHeaders[0].columns.['Not Reported'].comparisonValue", equalTo("0")))
            .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Years in ELL Headcount")))
            .andExpect(jsonPath("$.headcountHeaders[1].orderedColumnTitles", containsInRelativeOrder("1-5 Years", "6+ Years")))
            .andExpect(jsonPath("$.headcountHeaders[1].columns.['1-5 Years'].currentValue", equalTo("1")))
            .andExpect(jsonPath("$.headcountHeaders[1].columns.['6+ Years'].comparisonValue", equalTo("0")));

    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_WithInvalidCollection() throws Exception {
        UUID collectionID = UUID.randomUUID();
        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + collectionID.toString())
                    .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                    .param("type", "enrollment")
                    .param("compare", "true")
                    .contentType(APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof EntityNotFoundException))
            .andExpect(result -> assertTrue(Objects.requireNonNull(result.getResolvedException()).getMessage().contains(collectionID.toString())));
    }
    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_WithInvalidType_ShouldReturnBadRequest() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var schoolEntity = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcSchoolCollectionRepository.save(schoolEntity);

        mockMvc.perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + schoolEntity.getSdcSchoolCollectionID())
                .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .param("type", "invalidType")
                .param("compare", "true")
                .contentType(APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testReadSdcSchoolCollectionStudentPaginated_withNullFundingCode_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        stud1.setSchoolFundingCode(null);
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("schoolFundingCode").operation(FilterOperation.EQUAL).value(null).valueType(ValueType.STRING).build();

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
    void testReadSdcSchoolCollectionStudentPaginated_withNotNullFundingCode_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        stud1.setSchoolFundingCode("14");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("schoolFundingCode").operation(FilterOperation.NOT_EQUAL).value(null).valueType(ValueType.STRING).build();

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
    void testReadSdcSchoolCollectionStudentPaginated_withFteGreaterThan_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        stud1.setSchoolFundingCode("14");
        stud1.setFte(BigDecimal.valueOf(1));
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("fte").operation(FilterOperation.GREATER_THAN).value("0").valueType(ValueType.INTEGER).build();

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
    void testReadSdcSchoolCollectionStudentPaginated_withFteLessThan_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        stud1.setSchoolFundingCode("14");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("fte").operation(FilterOperation.LESS_THAN).value("1").valueType(ValueType.INTEGER).build();

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
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentPaginated_withFteGreaterThanEqual_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        stud1.setSchoolFundingCode("14");
        stud1.setFte(BigDecimal.valueOf(1));
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("fte").operation(FilterOperation.GREATER_THAN_OR_EQUAL_TO).value("1").valueType(ValueType.INTEGER).build();

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
    void testReadSdcSchoolCollectionStudentPaginated_withFteLessThanEqual_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        stud1.setSchoolFundingCode("14");
        stud1.setFte(BigDecimal.valueOf(1));
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("fte").operation(FilterOperation.LESS_THAN_OR_EQUAL_TO).value("1").valueType(ValueType.INTEGER).build();

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
    void testReadSdcSchoolCollectionStudentPaginated_withEllYearsGreaterThan_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity otherStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        otherStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity studentWithEll = sdcSchoolCollectionStudentRepository.save(mockStudentEntity);
        sdcSchoolCollectionStudentRepository.save(otherStudentEntity);
        SdcStudentEllEntity ellEntity = this.createMockStudentEllEntity(studentWithEll);
        ellEntity.setYearsInEll(5);

        this.sdcStudentEllRepository.save(ellEntity);

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcStudentEllEntity.yearsInEll").operation(FilterOperation.GREATER_THAN).value("2").valueType(ValueType.INTEGER).build();

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
    void testReadSdcSchoolCollectionStudentPaginated_withEllYearsLessThanEqual_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity otherStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        otherStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity studentWithEll = sdcSchoolCollectionStudentRepository.save(mockStudentEntity);
        sdcSchoolCollectionStudentRepository.save(otherStudentEntity);
        SdcStudentEllEntity ellEntity = this.createMockStudentEllEntity(studentWithEll);
        ellEntity.setYearsInEll(5);

        this.sdcStudentEllRepository.save(ellEntity);

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcStudentEllEntity.yearsInEll").operation(FilterOperation.LESS_THAN_OR_EQUAL_TO).value("5").valueType(ValueType.INTEGER).build();

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
    void testReadSdcSchoolCollectionStudentPaginated_withEllYearsLessThan_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity otherStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        otherStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity studentWithEll = sdcSchoolCollectionStudentRepository.save(mockStudentEntity);
        sdcSchoolCollectionStudentRepository.save(otherStudentEntity);
        SdcStudentEllEntity ellEntity = this.createMockStudentEllEntity(studentWithEll);
        ellEntity.setYearsInEll(5);

        this.sdcStudentEllRepository.save(ellEntity);

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcStudentEllEntity.yearsInEll").operation(FilterOperation.LESS_THAN).value("1").valueType(ValueType.INTEGER).build();

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
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentPaginated_withEllYearsGreaterThanEqual_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity otherStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        otherStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity studentWithEll = sdcSchoolCollectionStudentRepository.save(mockStudentEntity);
        sdcSchoolCollectionStudentRepository.save(otherStudentEntity);
        SdcStudentEllEntity ellEntity = this.createMockStudentEllEntity(studentWithEll);
        ellEntity.setYearsInEll(5);

        this.sdcStudentEllRepository.save(ellEntity);

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcStudentEllEntity.yearsInEll").operation(FilterOperation.GREATER_THAN_OR_EQUAL_TO).value("4").valueType(ValueType.INTEGER).build();

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
    void testGetSdcSchoolCollectionStudentHeadcounts_indigenousHeadcounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
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
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                    }
                    return models.get(i);
                })
                .toList();

        var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        savedStudents.forEach(student -> {
            var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
            enrolledProg.setSdcSchoolCollectionStudentEntity(student);
            enrolledProg.setEnrolledProgramCode("33");
            enrolledProg.setCreateUser("ABC");
            enrolledProg.setUpdateUser("ABC");
            enrolledProg.setCreateDate(LocalDateTime.now());
            enrolledProg.setUpdateDate(LocalDateTime.now());
            enrolledPrograms.add(enrolledProg);
            student.setSchoolFundingCode("20");
        });


        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "indigenous")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Indigenous Language and Culture")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Not Reported'].comparisonValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Indigenous Support Services")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Not Reported'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Eligible'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[2].title", equalTo("Other Approved Indigenous Programs")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Not Reported'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[3].title", equalTo("Indigenous Ancestry")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Total Students'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[4].title", equalTo("Ordinarily Living on Reserve")))
                .andExpect(jsonPath("$.headcountHeaders[4].columns.['Total Students'].currentValue", equalTo("1")));
    }

    @Test
    void testCreateYearsInEll_whenStudentExists_shouldCreateYearsInEll() throws Exception {
        UUID schoolId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        CollectionEntity collection = this.collectionRepository.save(this.createMockCollectionEntity());
        SdcSchoolCollectionEntity schoolCollection =
            this.createMockSdcSchoolCollectionEntity(collection, schoolId, districtId);

        this.sdcSchoolCollectionRepository.save(schoolCollection);
        SdcSchoolCollectionStudentEntity studentEntity = this.createMockSchoolStudentEntity(schoolCollection);
        studentEntity.setAssignedStudentId(studentId);
        this.sdcSchoolCollectionStudentRepository.save(studentEntity);

        SdcStudentEllEntity studentEll = new SdcStudentEllEntity();
        studentEll.setStudentID(studentId);
        studentEll.setYearsInEll(4);

        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin()
            .authorities(grantedAuthority);
        String payload = asJsonString(List.of(SdcStudentEllMapper.mapper.toStructure(studentEll)));
        this.mockMvc.perform(
                post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/years-in-ell")
                .contentType(APPLICATION_JSON)
                .content(payload)
                .with(mockAuthority))
            .andDo(print())
            .andExpect(jsonPath("$.[0]createUser", equalTo(ApplicationProperties.STUDENT_DATA_COLLECTION_API)))
            .andExpect(jsonPath("$.[0]updateUser", equalTo(ApplicationProperties.STUDENT_DATA_COLLECTION_API)))
            .andExpect(jsonPath("$.[0]studentID", equalTo(studentId.toString())))
            .andExpect(jsonPath("$.[0]yearsInEll", equalTo("4")))
            .andExpect(jsonPath("$.[0]sdcStudentEllID", is(not(emptyOrNullString()))));

    }

    @Test
    void testCreateYearsInEll_whenEllAlreadyExist_shouldReturnExistingElls() throws Exception {
        UUID schoolId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        CollectionEntity collection = this.collectionRepository.save(this.createMockCollectionEntity());
        SdcSchoolCollectionEntity schoolCollection =
            this.createMockSdcSchoolCollectionEntity(collection, schoolId, districtId);

        this.sdcSchoolCollectionRepository.save(schoolCollection);
        SdcSchoolCollectionStudentEntity studentEntity = this.createMockSchoolStudentEntity(schoolCollection);
        studentEntity.setAssignedStudentId(studentId);
        this.sdcSchoolCollectionStudentRepository.save(studentEntity);

        SdcStudentEllEntity studentEll = new SdcStudentEllEntity();
        studentEll.setStudentID(studentId);
        studentEll.setYearsInEll(4);

        this.sdcSchoolCollectionStudentService
          .createOrReturnSdcStudentEll(SdcStudentEllMapper.mapper.toStructure(studentEll));

        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin()
            .authorities(grantedAuthority);
        String payload = asJsonString(List.of(SdcStudentEllMapper.mapper.toStructure(studentEll)));
        this.mockMvc.perform(
                post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/years-in-ell")
                .contentType(APPLICATION_JSON)
                .content(payload)
                .with(mockAuthority))
            .andDo(print())
            .andExpect(jsonPath("$.[0]createUser", equalTo(ApplicationProperties.STUDENT_DATA_COLLECTION_API)))
            .andExpect(jsonPath("$.[0]updateUser", equalTo(ApplicationProperties.STUDENT_DATA_COLLECTION_API)))
            .andExpect(jsonPath("$.[0]studentID", equalTo(studentId.toString())))
            .andExpect(jsonPath("$.[0]yearsInEll", equalTo("4")))
            .andExpect(jsonPath("$.[0]sdcStudentEllID", is(not(emptyOrNullString()))));

    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_specialEdHeadcounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
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
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                    }
                    return models.get(i);
                })
                .toList();

        var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);

        savedStudents.forEach(student -> {
            student.setSpecialEducationCategoryCode("20");
        });

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();
        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "special-ed")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("A - Physically Dependent")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("B - Deafblind")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[2].title", equalTo("C - Moderate to Profound Intellectual Disability")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Reported'].comparisonValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[3].title", equalTo("D - Physical Disability or Chronic Health Impairment")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[4].title", equalTo("E - Visual Impairment")))
                .andExpect(jsonPath("$.headcountHeaders[4].columns.['Reported'].comparisonValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[4].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[5].title", equalTo("F - Deaf or Hard of Hearing")))
                .andExpect(jsonPath("$.headcountHeaders[5].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[5].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[6].title", equalTo("G - Autism Spectrum Disorder")))
                .andExpect(jsonPath("$.headcountHeaders[6].columns.['Reported'].comparisonValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[6].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[7].title", equalTo("H - Intensive Behaviour Interventions or Serious Mental Illness")))
                .andExpect(jsonPath("$.headcountHeaders[7].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[7].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[8].title", equalTo("K - Mild Intellectual Disability")))
                .andExpect(jsonPath("$.headcountHeaders[8].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[8].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[9].title", equalTo("P - Gifted")))
                .andExpect(jsonPath("$.headcountHeaders[9].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[9].columns.['Eligible'].currentValue", equalTo("0")));
    }

    @Test
    void testReadSdcSchoolCollectionStudentInEnrolledProgramsOrHasNativeAncestryInd_ReturnParentsPaginatedCrit_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        stud1.setNativeAncestryInd("N");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud2.setNativeAncestryInd("Y");
        var stud3 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud3.setNativeAncestryInd("Y");
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

        final SearchCriteria criteriaColl = SearchCriteria.builder().condition(AND).key("sdcSchoolCollection.sdcSchoolCollectionID").operation(FilterOperation.EQUAL).value(sdcMockSchool.getSdcSchoolCollectionID().toString()).valueType(ValueType.UUID).build();
        final SearchCriteria criteria1 = SearchCriteria.builder().condition(OR).key("sdcStudentEnrolledProgramEntities.enrolledProgramCode").operation(FilterOperation.IN_LEFT_JOIN).value("BC").valueType(ValueType.STRING).build();
        final SearchCriteria criteria2 = SearchCriteria.builder().condition(OR).key("nativeAncestryInd").operation(FilterOperation.EQUAL).value("Y").valueType(ValueType.STRING).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteriaColl);
        criteriaList.add(criteria1);
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
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentNoneInEnrolledProgramsReturnParentsPaginatedCrit_ministryOwnershipTeamID_ShouldReturnStatusOk() throws Exception {
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
        sdcSchoolCollectionStudentValidationIssueRepository
                .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                        stud2, StudentValidationIssueSeverityCode.ERROR));

        var enrolledProg3 = new SdcSchoolCollectionStudentEnrolledProgramEntity();
        enrolledProg2.setSdcSchoolCollectionStudentEntity(stud2);
        enrolledProg2.setEnrolledProgramCode("CD");
        enrolledProg2.setCreateUser("ABC");
        enrolledProg2.setUpdateUser("ABC");
        enrolledProg2.setCreateDate(LocalDateTime.now());
        enrolledProg2.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg2);
        sdcSchoolCollectionStudentValidationIssueRepository
                .save(createMockSdcSchoolCollectionStudentValidationIssueEntity(
                        stud2, StudentValidationIssueSeverityCode.ERROR));

        final SearchCriteria criteriaColl = SearchCriteria.builder().condition(AND).key("sdcSchoolCollection.sdcSchoolCollectionID").operation(FilterOperation.EQUAL).value(sdcMockSchool.getSdcSchoolCollectionID().toString()).valueType(ValueType.UUID).build();
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcStudentEnrolledProgramEntities.enrolledProgramCode").operation(FilterOperation.NONE_IN).value("CD,EF").valueType(ValueType.STRING).build();

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
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void testCreateStudentInINDEPENDSchool_WithEnrolledIndigenousProg_ShouldReturnStudentWithProgramEligibleNo() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        school.setSchoolCategoryCode("INDEPEND");
        school.setFacilityTypeCode("STANDARD");
        school.setSchoolReportingRequirementCode("REGULAR");
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
        when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(null);
        entity.setUpdateDate(null);
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.LOADED.toString());
        entity.setDob("20040701");
        entity.setSchoolFundingCode("20");
        entity.setNativeAncestryInd("Y");
        entity.setEnrolledGradeCode("01");
        entity.setEnrolledProgramCodes("3317");
        entity.setCareerProgramCode(null);
        entity.setBandCode("0600");
        entity.setIsSchoolAged(true);
        entity.setIsAdult(false);

        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT)
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("indigenousSupportProgramNonEligReasonCode", equalTo("INDYERR")));
    }
}
