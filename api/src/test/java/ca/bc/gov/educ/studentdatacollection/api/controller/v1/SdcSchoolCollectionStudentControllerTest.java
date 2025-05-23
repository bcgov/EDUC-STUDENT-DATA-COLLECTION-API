package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
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
import ca.bc.gov.educ.studentdatacollection.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
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
    CollectionRepository collectionRepository;

    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    @Autowired
    SdcDuplicateRepository sdcDuplicateRepository;

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
    SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

    @Autowired
    RestUtils restUtils;
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    @AfterEach
    void cleanup(){
        sdcDuplicateRepository.deleteAll();
        sdcSchoolCollectionStudentValidationIssueRepository.deleteAll();
        sdcSchoolCollectionStudentRepository.deleteAll();
        sdcSchoolCollectionRepository.deleteAll();
        collectionRepository.deleteAll();
        sdcDistrictCollectionRepository.deleteAll();
    }

    @Test
    void testReadSdcSchoolCollectionStudentPaginated_Always_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));
        sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));

        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED+"?pageSize=2")
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andDo(MvcResult::getAsyncResult)
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void testFindAllSldHistory_Always_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var student1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        student1.setAssignedStudentId(UUID.randomUUID());
        var student2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        student2.setAssignedStudentId(UUID.randomUUID());
        sdcSchoolCollectionStudentRepository.save(student1);
        sdcSchoolCollectionStudentRepository.save(student2);

        final SearchCriteria criteria = SearchCriteria.builder().condition(null).key("assignedStudentId").operation(FilterOperation.EQUAL).value(student1.getAssignedStudentId().toString()).valueType(ValueType.UUID).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);
        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED_SLD_HISTORY)
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andDo(MvcResult::getAsyncResult)
                .andReturn();
        String snapshotDate = "$.content[?(@.assignedStudentId=='"
                + student1.getAssignedStudentId().toString()
                + "')].snapshotDate";
        this.mockMvc.perform(asyncDispatch(result))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath(snapshotDate).value((LocalDate.of(LocalDate.now().getYear(), 9, 29)).toString()));
    }

    @Test
    void testReadSdcSchoolCollectionStudentPaginated_withSdcStudentEll_ShouldReturnStatusOkWithEll() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(
            collection,
            UUID.fromString(school.getSchoolId())
        );
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity otherStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        otherStudentEntity.setAssignedStudentId(UUID.randomUUID());
        mockStudentEntity.setYearsInEll(5);
        sdcSchoolCollectionStudentRepository.save(mockStudentEntity);
        sdcSchoolCollectionStudentRepository.save(otherStudentEntity);

        final MvcResult result = this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED+"?pageSize=2")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .contentType(APPLICATION_JSON))
            .andDo(print())
            .andDo(MvcResult::getAsyncResult)
            .andReturn();

        String yearsInEll = "$.content[?(@.assignedStudentId=='"
            + mockStudentEntity.getAssignedStudentId().toString()
            + "')].yearsInEll";
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentByID_WithInvalidID_ShouldReturnStatusNotFound() throws Exception {
        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + UUID.randomUUID())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .contentType(APPLICATION_JSON))
            .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    void ErrorAndWarningCountBySdcSchoolCollectionID_WithoutErrorsAndWarnings_ShouldReturnData() throws Exception {
        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.ERROR_WARNING_COUNT + "/" + UUID.randomUUID())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .contentType(APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    void ErrorAndWarningCountBySdcSchoolCollectionID_WithErrorsAndWarnings_ShouldReturnData() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
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
                .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));

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
        this.sdcSchoolCollectionStudentRepository.save(entity);

        MvcResult apiResponse = this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
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
    }

    @Test
    void testUpdateAndValidateSdcSchoolCollectionStudent_StudentWithInvalidCodes_ShouldReturnStatusBAD_REQUEST() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setNativeAncestryInd(null);
        entity.setGender(null);
        entity.setOtherCourses("12");
        this.sdcSchoolCollectionStudentRepository.save(entity);

        MvcResult apiResponse = this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
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
        ).contains("nativeAncestryInd cannot be null");
        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("gender cannot be null");
        assertThat(apiResponse
                .getResponse()
                .getContentAsString()
        ).contains("size must be between 0 and 1");
    }

    @Test
    void testUpdateAndValidateSdcSchoolCollectionStudent_StudentWithValidationErr_ShouldUpdateStatusToERRORAndReturnBadRequest() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()));
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setStudentPen("123456789");
        entity.setPostalCode(null);

        this.sdcSchoolCollectionStudentRepository.save(entity);

        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
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
        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()));
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
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
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
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
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
        when(this.restUtils.getSchoolFundingGroupsBySchoolID(any())).thenReturn(Arrays.asList(getIndependentSchoolFundingGroup(UUID.randomUUID().toString(), "08")));
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));

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
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));

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
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setSpecialEducationCategoryCode(null);
        entity.setNumberOfCourses("0400");
        entity.setEnrolledGradeCode("01");
        this.sdcSchoolCollectionStudentRepository.save(entity);
        when(this.restUtils.getSchoolFundingGroupsBySchoolID(any())).thenReturn(Arrays.asList(getIndependentSchoolFundingGroup(UUID.randomUUID().toString(), "01")));
        String dob = "19800101";
        entity.setDob(dob);

        this.mockMvc.perform(
                post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
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
    void testUpdateAndValidateSdcSchoolCollectionStudent_updateDOBToError_ShouldReturnStudentAndNotSaveToDatabaseAndReturnWithValidationIssuesSameUpdateDate() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
        when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());
        when(this.restUtils.getSchoolFundingGroupsBySchoolID(any())).thenReturn(Arrays.asList(getIndependentSchoolFundingGroup(school.getSchoolId(), "01")));

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        var origUpdateDate = LocalDateTime.now();
        entity.setUpdateDate(origUpdateDate);
        entity.setCreateDate(origUpdateDate);
        entity.setSpecialEducationCategoryCode(null);
        entity.setNumberOfCourses("0400");
        entity.setEnrolledGradeCode("01");
        var savedStudent = this.sdcSchoolCollectionStudentRepository.save(entity);

        savedStudent.setCreateDate(null);
        savedStudent.setUpdateDate(null);

        String dob = "19800101";
        savedStudent.setDob(dob);

        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(savedStudent)))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dob", equalTo(dob)))
                .andExpect(jsonPath("$.updateDate", containsString(origUpdateDate.toString().substring(0,20))))
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
        when(this.restUtils.getSchoolFundingGroupsBySchoolID(any())).thenReturn(Arrays.asList(getIndependentSchoolFundingGroup(UUID.randomUUID().toString(), "08")));
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setNumberOfCourses("0400");
        this.sdcSchoolCollectionStudentRepository.save(entity);

        entity.setPostalCode("");

        this.mockMvc.perform(
                post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));


        final var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void testFindAllShallow_multipleOrCriteriaAndAnAndCriteriaCombined_ShouldReturnStatusOk() throws Exception {
        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));


        final var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
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
        final List<SearchCriteria> criteriaList2 = new ArrayList<>();
        criteriaList2.add(criteria2);
        criteriaList2.add(criteria3);
        criteriaList2.add(criteria4);
        criteriaList2.add(criteria5);
        criteriaList2.add(criteria6);
        criteriaList2.add(criteria7);
        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList1).build());
        searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList2).build());
        final ObjectMapper objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.PAGINATED_SHALLOW)
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void testDeleteMultiSdcSchoolCollectionStudent_WithValidPayload_ShouldReturnOkay() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_DELETE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
        var savedSdcSchoolCollectionStudent = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));
        var savedSdcSchoolCollectionStudent2 = sdcSchoolCollectionStudentRepository.save(createMockSchoolStudentEntity(sdcSchoolCollection));

        SoftDeleteRecordSet softDeleteRecordSet = new SoftDeleteRecordSet();
        softDeleteRecordSet.setSoftDeleteStudentIDs(Arrays.asList(savedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID(), savedSdcSchoolCollectionStudent2.getSdcSchoolCollectionStudentID()));
        softDeleteRecordSet.setUpdateUser("ABC");

        String payload = asJsonString(softDeleteRecordSet);
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
        var collection1 = createMockCollectionEntity();
        collection1.setSnapshotDate(LocalDate.now().minusWeeks(1));
        collection1 = collectionRepository.save(collection1);
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection1, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var collection2 = createMockCollectionEntity();
        collection2.setSnapshotDate(LocalDate.now().minusWeeks(10));
        collection2 = collectionRepository.save(collection2);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection2, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "enrollment")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Student Headcount")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['All Students'].currentValue", equalTo("2")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['All Students'].comparisonValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Adult'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['School Aged'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Preschool Aged'].currentValue", equalTo("0")))
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
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "french")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Core French")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Early French Immersion")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("0")));
    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcountsForINDP_FNSSchool_frenchHeadcounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        school.setSchoolCategoryCode("INDP_FNS");
        school.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.REGULAR.getCode());
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        school.setSchoolReportingRequirementCode("CSF");
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "french")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Programme Francophone")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("0")));

    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_careerHeadcounts() throws Exception {
        var collection1 = createMockCollectionEntity();
        collection1.setSnapshotDate(LocalDate.now().minusWeeks(1));
        collection1 = collectionRepository.save(collection1);
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection1, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var collection2 = createMockCollectionEntity();
        collection2.setSnapshotDate(LocalDate.now().minusWeeks(10));
        collection2 = collectionRepository.save(collection2);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection2, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "career")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Career Preparation")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("2")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Co-Operative Education")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("0")));
    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcountsWithComparisonToPreviousYear_ellHeadCounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        //Current year's collection for the school.
        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        //Second school is for the previous year's collections.
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                .param("type", "ell")
                .param("compare", "true")
                .contentType(APPLICATION_JSON))
            .andDo(print())
            .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("English Language Learners")))
            .andExpect(jsonPath("$.headcountHeaders[0].orderedColumnTitles", containsInRelativeOrder("Eligible", "Reported")))
            .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("1")))
            .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].currentValue", equalTo("1")));

    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_WithInvalidCollection() throws Exception {
        UUID collectionID = UUID.randomUUID();
        this.mockMvc
            .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + collectionID.toString())
                    .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var schoolEntity = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcSchoolCollectionRepository.save(schoolEntity);

        mockMvc.perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + schoolEntity.getSdcSchoolCollectionID())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity otherStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        otherStudentEntity.setAssignedStudentId(UUID.randomUUID());
        mockStudentEntity.setYearsInEll(5);
        sdcSchoolCollectionStudentRepository.save(mockStudentEntity);
        sdcSchoolCollectionStudentRepository.save(otherStudentEntity);

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("yearsInEll").operation(FilterOperation.GREATER_THAN).value("2").valueType(ValueType.INTEGER).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity otherStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        otherStudentEntity.setAssignedStudentId(UUID.randomUUID());
        mockStudentEntity.setYearsInEll(5);
        sdcSchoolCollectionStudentRepository.save(mockStudentEntity);
        sdcSchoolCollectionStudentRepository.save(otherStudentEntity);

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("yearsInEll").operation(FilterOperation.LESS_THAN_OR_EQUAL_TO).value("5").valueType(ValueType.INTEGER).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("yearsInEll").operation(FilterOperation.LESS_THAN).value("1").valueType(ValueType.INTEGER).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());
        SdcSchoolCollectionStudentEntity otherStudentEntity = createMockSchoolStudentEntity(sdcSchoolCollection);
        otherStudentEntity.setAssignedStudentId(UUID.randomUUID());
        mockStudentEntity.setYearsInEll(5);
        sdcSchoolCollectionStudentRepository.save(mockStudentEntity);
        sdcSchoolCollectionStudentRepository.save(otherStudentEntity);

        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("yearsInEll").operation(FilterOperation.GREATER_THAN_OR_EQUAL_TO).value("4").valueType(ValueType.INTEGER).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_indigenousHeadcounts() throws Exception {
        var collection1 = createMockCollectionEntity();
        collection1.setSnapshotDate(LocalDate.now().minusWeeks(1));
        collection1 = collectionRepository.save(collection1);
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection1, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var collection2 = createMockCollectionEntity();
        collection2.setSnapshotDate(LocalDate.now().minusWeeks(10));
        collection2 = collectionRepository.save(collection2);
        var secondSchool = createMockSdcSchoolCollectionEntity(collection2, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "indigenous")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Indigenous Language and Culture")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Indigenous Support Services")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Eligible'].currentValue", equalTo("2")))
                .andExpect(jsonPath("$.headcountHeaders[2].title", equalTo("Other Approved Indigenous Programs")))
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
        UUID studentId = UUID.randomUUID();

        CollectionEntity collection = this.collectionRepository.save(this.createMockCollectionEntity());
        SdcSchoolCollectionEntity schoolCollection =
            this.createMockSdcSchoolCollectionEntity(collection, schoolId);

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
        UUID studentId = UUID.randomUUID();

        CollectionEntity collection = this.collectionRepository.save(this.createMockCollectionEntity());
        SdcSchoolCollectionEntity schoolCollection =
            this.createMockSdcSchoolCollectionEntity(collection, schoolId);

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
        var collection1 = createMockCollectionEntity();
        collection1.setSnapshotDate(LocalDate.now().minusWeeks(1));
        collection1 = collectionRepository.save(collection1);
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var firstSchool = createMockSdcSchoolCollectionEntity(collection1, UUID.fromString(school.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        var collection2 = createMockCollectionEntity();
        collection2.setSnapshotDate(LocalDate.now().minusWeeks(10));
        collection2 = collectionRepository.save(collection2);

        var secondSchool = createMockSdcSchoolCollectionEntity(collection2, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Eligible'].currentValue", equalTo("0")))
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
                .andExpect(jsonPath("$.headcountHeaders[7].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[8].title", equalTo("K - Mild Intellectual Disability")))
                .andExpect(jsonPath("$.headcountHeaders[8].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[8].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[9].title", equalTo("P - Gifted")))
                .andExpect(jsonPath("$.headcountHeaders[9].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[9].columns.['Eligible'].currentValue", equalTo("0")));
    }

    @Test
    void testGetSdcSchoolCollectionStudentCategoryHeadcounts_specialEdHeadcounts() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var district = createMockDistrict();
        var districtCollection1 = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId())));
        var districtCollection2Entity = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        districtCollection2Entity.setDistrictID(UUID.fromString(district.getDistrictId()));
        districtCollection2Entity.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        var districtCollection2 = sdcDistrictCollectionRepository.save(districtCollection2Entity);

        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());

        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var schoolCollection1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        schoolCollection1.setUploadDate(null);
        schoolCollection1.setUploadFileName(null);
        schoolCollection1.setSdcDistrictCollectionID(districtCollection1.getSdcDistrictCollectionID());

        var schoolCollection2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        schoolCollection2.setUploadDate(null);
        schoolCollection2.setUploadFileName(null);
        schoolCollection2.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        schoolCollection2.setSdcDistrictCollectionID(districtCollection2.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.saveAll(Arrays.asList(schoolCollection1, schoolCollection2));

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = IntStream.range(0, models.size())
                .mapToObj(i -> {
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(schoolCollection2);
                    } else {
                        models.get(i).setSdcSchoolCollection(schoolCollection1);
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
                .perform(get(URL.BASE_URL + "/" + URL.HEADCOUNTS + "/" + districtCollection1.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "special-ed-cat-per-school")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("A - Physically Dependent")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("B - Deafblind")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[2].title", equalTo("C - Moderate to Profound Intellectual Disability")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[3].title", equalTo("D - Physical Disability or Chronic Health Impairment")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[4].title", equalTo("E - Visual Impairment")))
                .andExpect(jsonPath("$.headcountHeaders[4].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[4].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[5].title", equalTo("F - Deaf or Hard of Hearing")))
                .andExpect(jsonPath("$.headcountHeaders[5].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[5].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[6].title", equalTo("G - Autism Spectrum Disorder")))
                .andExpect(jsonPath("$.headcountHeaders[6].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[6].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[7].title", equalTo("H - Intensive Behaviour Interventions or Serious Mental Illness")))
                .andExpect(jsonPath("$.headcountHeaders[7].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[7].columns.['Eligible'].currentValue", equalTo("0")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
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
        enrolledProg3.setSdcSchoolCollectionStudentEntity(stud2);
        enrolledProg3.setEnrolledProgramCode("CD");
        enrolledProg3.setCreateUser("ABC");
        enrolledProg3.setUpdateUser("ABC");
        enrolledProg3.setCreateDate(LocalDateTime.now());
        enrolledProg3.setUpdateDate(LocalDateTime.now());
        sdcSchoolCollectionStudentEnrolledProgramRepository.save(enrolledProg3);
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
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
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

        var mockCollection = createMockCollectionEntity();
        mockCollection.setCollectionStatusCode(CollectionStatus.PROVDUPES.getCode());
        var collection = collectionRepository.save(mockCollection);
        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId()));
        var schoolCollection = sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(schoolCollection);
        entity.setCreateDate(null);
        entity.setUpdateDate(null);
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.LOADED.toString());
        entity.setDob(LocalDateTime.now().minusYears(19).format(format));
        entity.setSchoolFundingCode("20");
        entity.setNativeAncestryInd("Y");
        entity.setEnrolledGradeCode("01");
        entity.setEnrolledProgramCodes("3317");
        entity.setCareerProgramCode(null);
        entity.setBandCode("0600");
        entity.setIsSchoolAged(true);
        entity.setIsAdult(false);

        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/false")
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("indigenousSupportProgramNonEligReasonCode", equalTo("INDYERR")));
    }

    @Test
    void testMarkPENForReview_withWarning_ShouldRemovedAssignedPENAndSaveToDatabase_ReturnStatusOk() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getPenMatchResult(any(), any(), anyString())).thenReturn(PenMatchResult.builder().build());
        when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setNumberOfCourses("0400");
        entity.setAssignedPen(null);
        entity.setAssignedStudentId(null);
        this.sdcSchoolCollectionStudentRepository.save(entity);

        this.mockMvc.perform(
                        post(URL.BASE_URL_DUPLICATE +"/mark-for-review")
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk());

        val curStudentEntity = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(curStudentEntity).isPresent();
        var studentEntity = curStudentEntity.get();
        assertThat(studentEntity.getAssignedStudentId()).isNull();
        assertThat(studentEntity.getAssignedPen()).isNull();
        assertThat(studentEntity.getUnderReviewAssignedPen()).isEqualTo(entity.getAssignedPen());
        assertThat(studentEntity.getUnderReviewAssignedStudentId()).isEqualTo(entity.getAssignedStudentId());
        assertThat(studentEntity.getPenMatchResult()).isEqualTo("INREVIEW");
    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_enrollmentHeadcounts() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));


        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "enrollment")
                        .param("compare", "false")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Student Headcount")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['All Students'].currentValue", equalTo("6")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Adult'].currentValue", equalTo("2")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['School Aged'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Preschool Aged'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Grade Headcount")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.11.currentValue", equalTo("2")));

    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_specialEdHeadcounts() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));


        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "special-ed")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("A - Physically Dependent")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("B - Deafblind")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[2].title", equalTo("C - Moderate to Profound Intellectual Disability")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[3].title", equalTo("D - Physical Disability or Chronic Health Impairment")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[4].title", equalTo("E - Visual Impairment")))
                .andExpect(jsonPath("$.headcountHeaders[4].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[4].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[5].title", equalTo("F - Deaf or Hard of Hearing")))
                .andExpect(jsonPath("$.headcountHeaders[5].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[5].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[6].title", equalTo("G - Autism Spectrum Disorder")))
                .andExpect(jsonPath("$.headcountHeaders[6].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[6].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[7].title", equalTo("H - Intensive Behaviour Interventions or Serious Mental Illness")))
                .andExpect(jsonPath("$.headcountHeaders[7].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[7].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[8].title", equalTo("K - Mild Intellectual Disability")))
                .andExpect(jsonPath("$.headcountHeaders[8].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[8].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[9].title", equalTo("P - Gifted")))
                .andExpect(jsonPath("$.headcountHeaders[9].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[9].columns.['Eligible'].currentValue", equalTo("0")));

    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_specialEdVarianceHeadcounts() throws Exception {
        CollectionEntity currentCollection = createMockCollectionEntity();
        currentCollection.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
        currentCollection.setSnapshotDate(LocalDate.of(2024, 6, 2));

        CollectionEntity febCollection = createMockCollectionEntity();
        febCollection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        febCollection.setSnapshotDate(LocalDate.of(2024, 2, 2));

        CollectionEntity septCollection = createMockCollectionEntity();
        septCollection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        septCollection.setSnapshotDate(LocalDate.of(2023, 9, 2));

        collectionRepository.saveAll(Arrays.asList(currentCollection, febCollection, septCollection));

        var districtID = UUID.randomUUID();
        var currentDistrictCollection = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(currentCollection, districtID));
        var febDistrictCollection = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(febCollection, districtID));
        var septDistrictCollection = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(septCollection, districtID));

        var school1 = createMockSchool();
        school1.setDistrictId(districtID.toString());
        school1.setDisplayName("test school");
        var school2 = createMockSchool();
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var schoolEntityFeb = createMockSdcSchoolCollectionEntity(febCollection, UUID.fromString(school1.getSchoolId()));
        schoolEntityFeb.setSdcDistrictCollectionID(febDistrictCollection.getSdcDistrictCollectionID());
        var schoolEntitySept = createMockSdcSchoolCollectionEntity(septCollection, UUID.fromString(school2.getSchoolId()));
        schoolEntitySept.setSdcDistrictCollectionID(septDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.saveAll(List.of(schoolEntityFeb, schoolEntitySept));

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = IntStream.range(0, models.size())
                .mapToObj(i -> {
                    if (i % 2 == 0) {
                        models.get(i).setSpecialEducationCategoryCode("A");
                        models.get(i).setSpecialEducationNonEligReasonCode(null);
                        models.get(i).setSdcSchoolCollection(schoolEntityFeb);
                    } else {
                        models.get(i).setSpecialEducationCategoryCode("B");
                        models.get(i).setSpecialEducationNonEligReasonCode(null);
                        models.get(i).setSdcSchoolCollection(schoolEntitySept);
                    }
                    return models.get(i);
                })
                .toList();

        sdcSchoolCollectionStudentRepository.saveAll(students);

        this.mockMvc
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + currentDistrictCollection.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "INCLUSIVE_EDUCATION_VARIANCE")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headcountResultsTable.headers", hasSize(49)))
                .andExpect(jsonPath("$.headcountResultsTable.rows", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.headcountResultsTable.rows[0].title.currentValue", equalTo("Snapshot Date")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[1].title.currentValue", equalTo("Level 1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[1].['Total Feb'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[1].['Total Sep'].currentValue", equalTo("2")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[1].['Total Variance'].currentValue", equalTo("2")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[1].['01 Feb'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[1].['01 Sep'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[1].['01 Variance'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[1].['02 Variance'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[1].['10 Variance'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[2].title.currentValue", equalTo("A - Physically Dependent")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[2].['Total Feb'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[2].['Total Sep'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[2].['Total Variance'].currentValue", equalTo("4")));
    }


    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_specialEdHeadcountsByGrade() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));


        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "special-ed-per-school")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("A - Physically Dependent")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].currentValue", equalTo("1")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[*].['title'].currentValue", containsInAnyOrder("0000001 - School1", "0000002 - School2", "All Schools")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['01'].currentValue", contains("2")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['02'].currentValue", contains("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['10'].currentValue", contains("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['01'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['02'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['10'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows", hasSize(3)));
    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_enrollmentBySchoolHeadcounts_WithCompareTrue() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));


        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "grade-enrollment")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Student Headcount")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['All Students'].currentValue", equalTo("6")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['All Students'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Adult'].currentValue", equalTo("2")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['School Aged'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Preschool Aged'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Grade Headcount")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.11.currentValue", equalTo("2")));

    }

    @Test
    void testSdcDistrictCollectionStudentHeadcounts_careerHeadcounts() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));


        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "career")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Career Preparation")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("6")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Co-Operative Education")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("0")));
    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_frenchCombinedHeadcounts() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        school1.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.REGULAR.getCode());

        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());
        school2.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.REGULAR.getCode());

        var school3 = createMockSchool();
        school3.setDisplayName("School3");
        school3.setMincode("0000003");
        school3.setDistrictId(districtID.toString());
        school3.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());

        var school4 = createMockSchool();
        school4.setDisplayName("School4");
        school4.setMincode("0000004");
        school4.setDistrictId(districtID.toString());
        school4.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
        when(this.restUtils.getSchoolBySchoolID(school3.getSchoolId())).thenReturn(Optional.of(school3));
        when(this.restUtils.getSchoolBySchoolID(school4.getSchoolId())).thenReturn(Optional.of(school4));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        secondSchool.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        var thirdSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school3.getSchoolId()));
        thirdSchool.setUploadDate(null);
        thirdSchool.setUploadFileName(null);
        thirdSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var fourthSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school4.getSchoolId()));
        fourthSchool.setUploadDate(null);
        fourthSchool.setUploadFileName(null);
        fourthSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        fourthSchool.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchool, secondSchool, thirdSchool, fourthSchool));

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var schools = Arrays.asList(firstSchool, secondSchool, thirdSchool, fourthSchool);
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = IntStream.range(0, models.size())
                .mapToObj(i -> {
                    models.get(i).setSdcSchoolCollection(schools.get(i % schools.size()));

                    return models.get(i);
                })
                .toList();

        var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        savedStudents.forEach(student -> {
            var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
            enrolledProg.setSdcSchoolCollectionStudentEntity(student);
            if (Objects.equals(student.getGender(), "M"))
                enrolledProg.setEnrolledProgramCode("14");
            else
                enrolledProg.setEnrolledProgramCode("05");
            enrolledProg.setCreateUser("ABC");
            enrolledProg.setUpdateUser("ABC");
            enrolledProg.setCreateDate(LocalDateTime.now());
            enrolledProg.setUpdateDate(LocalDateTime.now());
            enrolledPrograms.add(enrolledProg);
        });

        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        this.mockMvc
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "french")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Core French")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Early French Immersion")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Eligible'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[2].title", equalTo("Late French Immersion")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Eligible'].currentValue", equalTo("2")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Eligible'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[3].title", equalTo("Programme Francophone")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Eligible'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Eligible'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Eligible'].comparisonValue", equalTo("0")));
    }

    @Test
    void testGetSdcDistrictCollectionSchoolAndGrades_frenchCombinedTable() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        school1.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.REGULAR.getCode());

        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());
        school2.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.REGULAR.getCode());

        var school3 = createMockSchool();
        school3.setDisplayName("School3");
        school3.setMincode("0000003");
        school3.setDistrictId(districtID.toString());
        school3.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());

        var school4 = createMockSchool();
        school4.setDisplayName("School4");
        school4.setMincode("0000004");
        school4.setDistrictId(districtID.toString());
        school4.setSchoolReportingRequirementCode(SchoolReportingRequirementCodes.CSF.getCode());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
        when(this.restUtils.getSchoolBySchoolID(school3.getSchoolId())).thenReturn(Optional.of(school3));
        when(this.restUtils.getSchoolBySchoolID(school4.getSchoolId())).thenReturn(Optional.of(school4));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        secondSchool.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        var thirdSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school3.getSchoolId()));
        thirdSchool.setUploadDate(null);
        thirdSchool.setUploadFileName(null);
        thirdSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var fourthSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school4.getSchoolId()));
        fourthSchool.setUploadDate(null);
        fourthSchool.setUploadFileName(null);
        fourthSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        fourthSchool.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchool, secondSchool, thirdSchool, fourthSchool));

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var schools = Arrays.asList(firstSchool, secondSchool, thirdSchool, fourthSchool);
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = new ArrayList<>(IntStream.range(0, models.size())
                .mapToObj(i -> {
                    models.get(i).setSdcSchoolCollection(schools.get(i % schools.size()));

                    return models.get(i);
                })
                .toList());

        var fourthSchoolStudentCustom = createMockSchoolStudentEntity(fourthSchool);
        fourthSchoolStudentCustom.setGender("F");
        students.add(fourthSchoolStudentCustom);

        var savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        savedStudents.forEach(student -> {
            var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
            enrolledProg.setSdcSchoolCollectionStudentEntity(student);
            if (Objects.equals(student.getGender(), "M"))
                enrolledProg.setEnrolledProgramCode("14");
            else
                enrolledProg.setEnrolledProgramCode("05");
            enrolledProg.setCreateUser("ABC");
            enrolledProg.setUpdateUser("ABC");
            enrolledProg.setCreateDate(LocalDateTime.now());
            enrolledProg.setUpdateDate(LocalDateTime.now());
            enrolledPrograms.add(enrolledProg);
        });

        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        this.mockMvc
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "french-per-school")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[2].title", equalTo("Late French Immersion")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Eligible'].currentValue", equalTo("2")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Eligible'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[3].title", equalTo("Programme Francophone")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Eligible'].currentValue", equalTo("5")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Eligible'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[*].['title'].currentValue", containsInAnyOrder("0000001 - School1", "0000002 - School2", "0000003 - School3", "0000004 - School4", "All Schools")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['01'].currentValue", contains("2")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['02'].currentValue", contains("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['10'].currentValue", contains("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['GA'].currentValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['KF'].currentValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['01'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['02'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['10'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['GA'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['KF'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows", hasSize(5)));
    }

    @Test
    void testSdcDistrictCollectionStudentHeadcounts_careerBySchoolIDHeadcounts_WithCompareTrue() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));


        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "career-per-school")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Career Preparation")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("6")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Co-Operative Education")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("0")));
    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_indigenousHeadcounts() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "indigenous")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Indigenous Language and Culture")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].title", equalTo("Indigenous Support Services")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[1].columns.['Eligible'].currentValue", equalTo("6")))
                .andExpect(jsonPath("$.headcountHeaders[2].title", equalTo("Other Approved Indigenous Programs")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Reported'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[2].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[3].title", equalTo("Indigenous Ancestry")))
                .andExpect(jsonPath("$.headcountHeaders[3].columns.['Total Students'].currentValue", equalTo("4")))
                .andExpect(jsonPath("$.headcountHeaders[4].title", equalTo("Ordinarily Living on Reserve")))
                .andExpect(jsonPath("$.headcountHeaders[4].columns.['Total Students'].currentValue", equalTo("1")));
    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_bandResidenceTable() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                        models.get(i).setBandCode("0500");
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                        models.get(i).setBandCode("0501");
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "band-codes")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountResultsTable.rows[*].['title'].currentValue", containsInAnyOrder("0500 - KWANLIN DUN", "All Bands & Students")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0500 - KWANLIN DUN')].['Headcount'].currentValue", contains("4")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0500 - KWANLIN DUN')].['FTE'].currentValue", contains("2.72")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0500 - KWANLIN DUN')].['Headcount'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0500 - KWANLIN DUN')].['FTE'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows", hasSize(2)));
    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_bandHeadcounts_WithCompare() throws Exception {

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collection.setSnapshotDate(LocalDate.now().minusWeeks(1));
        collection.setCreateDate(LocalDateTime.of(Year.now().getValue(), Month.JANUARY, 1, 0, 0));
        collection = collectionRepository.save(collection);

        var collection2 = createMockCollectionEntity();
        collection2.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collection2.setSnapshotDate(LocalDate.now().minusWeeks(10));
        collection2.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        collection2 = collectionRepository.save(collection2);

        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        firstSchool.setCollectionEntity(collection);
        firstSchool.setCreateDate(LocalDateTime.of(Year.now().getValue(), Month.JANUARY, 1, 0, 0));
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        secondSchool.setCollectionEntity(collection2);
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
                    models.get(i).setNativeAncestryInd("Y");
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                        models.get(i).setBandCode("0500");
                    } else {
                        models.get(i).setFte(BigDecimal.TEN);
                        models.get(i).setSdcSchoolCollection(firstSchool);
                        models.get(i).setBandCode("0500");
                    }
                    return models.get(i);
                })
                .toList();

        sdcSchoolCollectionStudentRepository.saveAll(students);

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "band-codes")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                //this is correct two of the students are in deleted - total of 6 is correct
                .andExpect(jsonPath("$.headcountResultsTable.rows[*].['title'].currentValue", containsInAnyOrder("0500 - KWANLIN DUN", "All Bands & Students")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0500 - KWANLIN DUN')].['Headcount'].currentValue", contains("2")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0500 - KWANLIN DUN')].['FTE'].currentValue", contains("20.00")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].comparisonValue=='0500 - KWANLIN DUN')].['Headcount'].comparisonValue", contains("4")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].comparisonValue=='0500 - KWANLIN DUN')].['FTE'].comparisonValue", contains("2.72")))
                .andExpect(jsonPath("$.headcountResultsTable.rows", hasSize(2)));
    }

    @Test
    void testGetSdcSchoolCollectionStudentHeadcounts_bandHeadcountsManyPrevBands_WithCompare() throws Exception {

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collection.setSnapshotDate(LocalDate.now().minusWeeks(1));
        collection.setCreateDate(LocalDateTime.of(Year.now().getValue(), Month.JANUARY, 1, 0, 0));
        collection = collectionRepository.save(collection);
        var collection2 = createMockCollectionEntity();
        collection2.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collection2.setSnapshotDate(LocalDate.now().minusWeeks(10));
        collection2.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        collection2 = collectionRepository.save(collection2);

        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity1 = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));
        var mockDistrictCollectionEntity2 = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection2, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity1.getSdcDistrictCollectionID());
        firstSchool.setCollectionEntity(collection);
        firstSchool.setCreateDate(LocalDateTime.of(Year.now().getValue(), Month.JANUARY, 1, 0, 0));
        var secondSchool = createMockSdcSchoolCollectionEntity(collection2, UUID.fromString(school1.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity2.getSdcDistrictCollectionID());
        secondSchool.setCollectionEntity(collection2);
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
                    models.get(i).setNativeAncestryInd("Y");
                    models.get(i).setSdcSchoolCollectionStudentStatusCode("CREATED");
                    if (i == 0) {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                        models.get(i).setBandCode("0500");
                    } else if (i == 1) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                        models.get(i).setBandCode("0500");
                    } else if (i == 2) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                        models.get(i).setBandCode("0501");
                    } else if (i == 3) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                        models.get(i).setBandCode("0502");
                    } else if (i == 4) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                        models.get(i).setBandCode("0503");
                    } else if (i == 5) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                        models.get(i).setBandCode("0504");
                    } else if (i == 6) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                        models.get(i).setBandCode("0505");
                    } else if (i == 7) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                        models.get(i).setBandCode("0506");
                    } else if (i == 8) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                        models.get(i).setBandCode("0507");
                    }
                    return models.get(i);
                })
                .toList();

        sdcSchoolCollectionStudentRepository.saveAll(students);

        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/" + URL.HEADCOUNTS + "/" + firstSchool.getSdcSchoolCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "band-codes")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountResultsTable.rows[*].['title'].currentValue", containsInAnyOrder("0500 - KWANLIN DUN", "All Bands & Students")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0500 - KWANLIN DUN')].['Headcount'].currentValue", contains("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0500 - KWANLIN DUN')].['FTE'].currentValue", contains("0.79")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].comparisonValue=='0500 - KWANLIN DUN')].['Headcount'].comparisonValue", contains("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].comparisonValue=='0500 - KWANLIN DUN')].['FTE'].comparisonValue", contains("1.23")))
                .andExpect(jsonPath("$.headcountResultsTable.rows", hasSize(2)));
    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_ellHeadCounts_WithCompare() throws Exception {

        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "ell")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("English Language Learners")))
                .andExpect(jsonPath("$.headcountHeaders[0].orderedColumnTitles", containsInRelativeOrder("Eligible", "Reported")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("5")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Reported'].currentValue", equalTo("5")));

    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_indigenousHeadcountsByGrade() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "indigenous-per-school")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Indigenous Language and Culture")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[*].['title'].currentValue", containsInAnyOrder("0000001 - School1", "0000002 - School2", "All Schools")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['01'].currentValue", contains("2")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['02'].currentValue", contains("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['10'].currentValue", contains("1")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['01'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['02'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Schools')].['10'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows", hasSize(3)));
    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_bandResidenceTablePerSchool() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
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
                        models.get(i).setBandCode("0500");
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                        models.get(i).setBandCode("0501");
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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "band-codes-per-school")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Indigenous Language and Culture")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].currentValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountHeaders[0].columns.['Eligible'].comparisonValue", equalTo("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[*].['title'].currentValue", containsInAnyOrder("0000001 - School1", "0000002 - School2", "All Bands & Students")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Bands & Students')].['Headcount'].currentValue", contains("6")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Bands & Students')].['FTE'].currentValue", contains("5.18")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0000001 - School1')].['FTE'].currentValue", contains("2.46")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Bands & Students')].['Headcount'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='All Bands & Students')].['FTE'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0000001 - School1')].['FTE'].comparisonValue", contains("0")))
                .andExpect(jsonPath("$.headcountResultsTable.rows", hasSize(3)));
    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_bandResidenceTablePerSchool_tableValues() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());
        var school3 = createMockSchool();
        school3.setDisplayName("School3");
        school3.setMincode("0000003");
        school3.setDistrictId(districtID.toString());
        var school4 = createMockSchool();
        school4.setDisplayName("School4");
        school4.setMincode("0000004");
        school4.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
        when(this.restUtils.getSchoolBySchoolID(school3.getSchoolId())).thenReturn(Optional.of(school3));
        when(this.restUtils.getSchoolBySchoolID(school4.getSchoolId())).thenReturn(Optional.of(school4));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var thirdSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school3.getSchoolId()));
        thirdSchool.setUploadDate(null);
        thirdSchool.setUploadFileName(null);
        thirdSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var fourthSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school4.getSchoolId()));
        fourthSchool.setUploadDate(null);
        fourthSchool.setUploadFileName(null);
        fourthSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());

        sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchool, secondSchool, thirdSchool, fourthSchool));

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();

        List<SdcSchoolCollectionStudentEntity> students = new ArrayList<>();
        UUID[] schoolIds = {firstSchool.getSdcSchoolCollectionID(), secondSchool.getSdcSchoolCollectionID(), thirdSchool.getSdcSchoolCollectionID(), fourthSchool.getSdcSchoolCollectionID()};
        String[] bandCodes = {"0500", "0501", "0600", "0601"};

        for (var model : models) {
            for (int i = 0; i < schoolIds.length; i++) {
                var newModel = new SdcSchoolCollectionStudentEntity();
                BeanUtils.copyProperties(model, newModel);
                newModel.setSdcSchoolCollection(SdcSchoolCollectionEntity.builder().sdcSchoolCollectionID(schoolIds[i]).build());
                if (Objects.equals(newModel.getGender(), "F") && i == 2) {
                    continue;
                }
                if (Objects.equals(newModel.getGender(), "M") && i == 3) {
                    continue;
                }
                newModel.setBandCode(bandCodes[i]);
                students.add(newModel);
            }
        }

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
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "band-codes-per-school")
                        .param("compare", "false")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountResultsTable.rows[*].['title'].currentValue", containsInAnyOrder("0000001 - School1", "0000002 - School2", "0000003 - School3", "0000004 - School4", "All Bands & Students")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0000001 - School1')].['FTE'].currentValue", contains("5.18")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0000001 - School1')].['Headcount'].currentValue", contains("6")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0000002 - School2')].['FTE'].currentValue", contains("5.18")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0000002 - School2')].['Headcount'].currentValue", contains("6")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0000003 - School3')].['FTE'].currentValue", contains("2.46")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0000003 - School3')].['Headcount'].currentValue", contains("2")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0000004 - School4')].['FTE'].currentValue", contains("2.72")))
                .andExpect(jsonPath("$.headcountResultsTable.rows[?(@.['title'].currentValue=='0000004 - School4')].['Headcount'].currentValue", contains("4")))
                .andExpect(jsonPath("$.headcountResultsTable.rows", hasSize(5)));
    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_zerofte_tableValues() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());
        var school3 = createMockSchool();
        school3.setDisplayName("School3");
        school3.setMincode("0000003");
        school3.setDistrictId(districtID.toString());
        var school4 = createMockSchool();
        school4.setDisplayName("School4");
        school4.setMincode("0000004");
        school4.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
        when(this.restUtils.getSchoolBySchoolID(school3.getSchoolId())).thenReturn(Optional.of(school3));
        when(this.restUtils.getSchoolBySchoolID(school4.getSchoolId())).thenReturn(Optional.of(school4));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var thirdSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school3.getSchoolId()));
        thirdSchool.setUploadDate(null);
        thirdSchool.setUploadFileName(null);
        thirdSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var fourthSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school4.getSchoolId()));
        fourthSchool.setUploadDate(null);
        fourthSchool.setUploadFileName(null);
        fourthSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());

        sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchool, secondSchool, thirdSchool, fourthSchool));

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();

        List<SdcSchoolCollectionStudentEntity> students = new ArrayList<>();
        UUID[] schoolIds = {firstSchool.getSdcSchoolCollectionID(), secondSchool.getSdcSchoolCollectionID(), thirdSchool.getSdcSchoolCollectionID(), fourthSchool.getSdcSchoolCollectionID()};
        String[] bandCodes = {"0500", "0501", "0600", "0601"};

        for (var model : models) {
            for (int i = 0; i < schoolIds.length; i++) {
                var newModel = new SdcSchoolCollectionStudentEntity();
                BeanUtils.copyProperties(model, newModel);
                newModel.setSdcSchoolCollection(SdcSchoolCollectionEntity.builder().sdcSchoolCollectionID(schoolIds[i]).build());
                if (Objects.equals(newModel.getGender(), "F") && i == 2) {
                    continue;
                }
                if (Objects.equals(newModel.getGender(), "M") && i == 3) {
                    continue;
                }
                newModel.setBandCode(bandCodes[i]);
                newModel.setFte(BigDecimal.ZERO);
                newModel.setFteZeroReasonCode(ZeroFteReasonCodes.OUT_OF_PROVINCE.getCode());
                students.add(newModel);
            }
        }
        sdcSchoolCollectionStudentRepository.saveAll(students);

        this.mockMvc
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "zero-fte-summary")
                        .param("compare", "false")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Non-Funded Students")));

    }

    @Test
    void testGetSdcDistrictCollectionStudentHeadcounts_zerofte_withCompare_tableValues() throws Exception {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        var mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));

        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());
        var school3 = createMockSchool();
        school3.setDisplayName("School3");
        school3.setMincode("0000003");
        school3.setDistrictId(districtID.toString());
        var school4 = createMockSchool();
        school4.setDisplayName("School4");
        school4.setMincode("0000004");
        school4.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));
        when(this.restUtils.getSchoolBySchoolID(school3.getSchoolId())).thenReturn(Optional.of(school3));
        when(this.restUtils.getSchoolBySchoolID(school4.getSchoolId())).thenReturn(Optional.of(school4));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var thirdSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school3.getSchoolId()));
        thirdSchool.setUploadDate(null);
        thirdSchool.setUploadFileName(null);
        thirdSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var fourthSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school4.getSchoolId()));
        fourthSchool.setUploadDate(null);
        fourthSchool.setUploadFileName(null);
        fourthSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());

        sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchool, secondSchool, thirdSchool, fourthSchool));

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();

        List<SdcSchoolCollectionStudentEntity> students = new ArrayList<>();
        UUID[] schoolIds = {firstSchool.getSdcSchoolCollectionID(), secondSchool.getSdcSchoolCollectionID(), thirdSchool.getSdcSchoolCollectionID(), fourthSchool.getSdcSchoolCollectionID()};
        String[] bandCodes = {"0500", "0501", "0600", "0601"};

        for (var model : models) {
            for (int i = 0; i < schoolIds.length; i++) {
                var newModel = new SdcSchoolCollectionStudentEntity();
                BeanUtils.copyProperties(model, newModel);
                newModel.setSdcSchoolCollection(SdcSchoolCollectionEntity.builder().sdcSchoolCollectionID(schoolIds[i]).build());
                if (Objects.equals(newModel.getGender(), "F") && i == 2) {
                    continue;
                }
                if (Objects.equals(newModel.getGender(), "M") && i == 3) {
                    continue;
                }
                newModel.setBandCode(bandCodes[i]);
                newModel.setFte(BigDecimal.ZERO);
                newModel.setFteZeroReasonCode(ZeroFteReasonCodes.OUT_OF_PROVINCE.getCode());
                students.add(newModel);
            }
        }
        sdcSchoolCollectionStudentRepository.saveAll(students);

        //Create History Collection - Begin
        CollectionEntity previousCollection = collectionRepository.save(createMockCollectionEntity());
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-05-30");
        previousCollection.setCloseDate(LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT));
        previousCollection.setCollectionStatusCode("COMPLETED");
        var previousDistrictID = UUID.randomUUID();
        var mockPreviousDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(previousCollection, previousDistrictID));

        var previousSchool1 = createMockSchool();
        previousSchool1.setDisplayName("School1");
        previousSchool1.setMincode("0000001");
        previousSchool1.setDistrictId(districtID.toString());
        var previousSchool2 = createMockSchool();
        previousSchool2.setDisplayName("School2");
        previousSchool2.setMincode("0000002");
        previousSchool2.setDistrictId(districtID.toString());
        var previousSchool3 = createMockSchool();
        previousSchool3.setDisplayName("School3");
        previousSchool3.setMincode("0000003");
        previousSchool3.setDistrictId(districtID.toString());
        var previousSchool4 = createMockSchool();
        previousSchool4.setDisplayName("School4");
        previousSchool4.setMincode("0000004");
        previousSchool4.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(previousSchool1.getSchoolId())).thenReturn(Optional.of(previousSchool1));
        when(this.restUtils.getSchoolBySchoolID(previousSchool2.getSchoolId())).thenReturn(Optional.of(previousSchool2));
        when(this.restUtils.getSchoolBySchoolID(previousSchool3.getSchoolId())).thenReturn(Optional.of(previousSchool3));
        when(this.restUtils.getSchoolBySchoolID(previousSchool4.getSchoolId())).thenReturn(Optional.of(previousSchool4));

        var previousFirstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(previousSchool1.getSchoolId()));
        previousFirstSchool.setUploadDate(null);
        previousFirstSchool.setUploadFileName(null);
        previousFirstSchool.setSdcDistrictCollectionID(mockPreviousDistrictCollectionEntity.getSdcDistrictCollectionID());
        var previousSecondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(previousSchool2.getSchoolId()));
        previousSecondSchool.setUploadDate(null);
        previousSecondSchool.setUploadFileName(null);
        previousSecondSchool.setSdcDistrictCollectionID(mockPreviousDistrictCollectionEntity.getSdcDistrictCollectionID());
        var previousThirdSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(previousSchool3.getSchoolId()));
        previousThirdSchool.setUploadDate(null);
        previousThirdSchool.setUploadFileName(null);
        previousThirdSchool.setSdcDistrictCollectionID(mockPreviousDistrictCollectionEntity.getSdcDistrictCollectionID());
        var previousFourthSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(previousSchool4.getSchoolId()));
        previousFourthSchool.setUploadDate(null);
        previousFourthSchool.setUploadFileName(null);
        previousFourthSchool.setSdcDistrictCollectionID(mockPreviousDistrictCollectionEntity.getSdcDistrictCollectionID());

        sdcSchoolCollectionRepository.saveAll(Arrays.asList(previousFirstSchool, previousSecondSchool, previousThirdSchool, previousFourthSchool));

        final File previousFile = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> previousEntities = new ObjectMapper().readValue(previousFile, new TypeReference<>() {
        });
        var previousModels = previousEntities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();

        List<SdcSchoolCollectionStudentEntity> previousStudents = new ArrayList<>();
        UUID[] previousSchoolIds = {previousFirstSchool.getSdcSchoolCollectionID(), previousSecondSchool.getSdcSchoolCollectionID(), previousThirdSchool.getSdcSchoolCollectionID(), previousFourthSchool.getSdcSchoolCollectionID()};

        for (var model : previousModels) {
            for (int i = 0; i < previousSchoolIds.length; i++) {
                var newModel = new SdcSchoolCollectionStudentEntity();
                BeanUtils.copyProperties(model, newModel);
                newModel.setSdcSchoolCollection(SdcSchoolCollectionEntity.builder().sdcSchoolCollectionID(previousSchoolIds[i]).build());
                if (Objects.equals(newModel.getGender(), "F") && i == 2) {
                    continue;
                }
                if (Objects.equals(newModel.getGender(), "M") && i == 3) {
                    continue;
                }
                newModel.setBandCode(bandCodes[i]);
                newModel.setFte(BigDecimal.ZERO);
                newModel.setFteZeroReasonCode(ZeroFteReasonCodes.OUT_OF_PROVINCE.getCode());
                previousStudents.add(newModel);
            }
        }
        sdcSchoolCollectionStudentRepository.saveAll(previousStudents);

        //Create History Collection - End

        this.mockMvc
                .perform(get(URL.BASE_DISTRICT_HEADCOUNTS + "/" + mockDistrictCollectionEntity.getSdcDistrictCollectionID())
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("type", "zero-fte-summary")
                        .param("compare", "true")
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.headcountHeaders[0].title", equalTo("Non-Funded Students")));

    }

    @Test
    void testReadSdcSchoolCollectionStudentPaginated_withCollectionID_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        stud1.setSchoolFundingCode("14");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcSchoolCollection.collectionEntity.collectionID").operation(FilterOperation.EQUAL).value(collection.getCollectionID().toString()).valueType(ValueType.UUID).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void testReadSdcSchoolCollectionStudentPaginatedSlice_withCollectionID_ShouldReturnStatusOk() throws Exception {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        stud1.setSchoolFundingCode("14");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        final SearchCriteria criteria = SearchCriteria.builder().condition(AND).key("sdcSchoolCollection.collectionEntity.collectionID").operation(FilterOperation.EQUAL).value(collection.getCollectionID().toString()).valueType(ValueType.UUID).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);

        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED_SLICE)
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void testUpdatePENStatus_withPenCodeNEW_ShouldUpdateStatusAndReturnStatusOk() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();

        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));

        val assignedStudentId = UUID.randomUUID();
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setPenMatchResult("INREVIEW");
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity.setAssignedStudentId(assignedStudentId);
        entity.setAssignedPen("123456789");
        this.sdcSchoolCollectionStudentRepository.save(entity);

        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/update-pen/type/" + "NEW")
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val curStudentEntity = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(curStudentEntity).isPresent();
        var studentEntity = curStudentEntity.get();
        assertThat(studentEntity.getPenMatchResult()).isEqualTo("NEW");
        assertThat(studentEntity.getAssignedPen()).isEqualTo("123456789");
        assertThat(studentEntity.getAssignedStudentId()).isEqualTo(assignedStudentId);
    }

    @Test
    void testUpdatePENStatus_withPenCodeMATCH_ShouldUpdateStatusAndReturnStatusOk() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);

        var school = this.createMockSchool();

        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(this.restUtils.getGradStatusResult(any(), any())).thenReturn(GradStatusResult.builder().build());

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection,UUID.fromString(school.getSchoolId())));
        var assignedPenUUID = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setPenMatchResult(null);
        entity.setAssignedStudentId(assignedPenUUID);
        entity.setAssignedPen("123456789");
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateDate(null);
        entity.setCreateDate(null);
        entity = this.sdcSchoolCollectionStudentRepository.save(entity);

        this.mockMvc.perform(
                        post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/update-pen/type/" + "MATCH")
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)))
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val curStudentEntity = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(curStudentEntity).isPresent();
        var studentEntity = curStudentEntity.get();
        assertThat(studentEntity.getPenMatchResult()).isEqualTo("MATCH");
        assertThat(studentEntity.getAssignedPen()).isEqualTo("123456789");
        assertThat(studentEntity.getAssignedStudentId()).isEqualTo(assignedPenUUID);
    }

    @Test
    void testMoveSldRecords() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(
                grantedAuthority);
        UUID randomStudentID = UUID.randomUUID();
        String fromPen = "123456789";
        String toPen = "987654321";

        Student toStudent = new Student();
        toStudent.setStudentID(randomStudentID.toString());
        toStudent.setPen(toPen);
        when(restUtils.getStudentByPEN(any(), any())).thenReturn(toStudent);

        SdcSchoolCollectionStudentEntity studentEntity = new SdcSchoolCollectionStudentEntity();
        var collection = createMockCollectionEntity();
        collectionRepository.save(collection);
        var sdcSchoolCollection = createMockSdcSchoolCollectionEntity(collection,UUID.randomUUID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollection);
        studentEntity.setSdcSchoolCollection(sdcSchoolCollection);
        studentEntity.setAssignedStudentId(UUID.randomUUID()); // some existing ID
        studentEntity.setAssignedPen(fromPen);
        studentEntity = sdcSchoolCollectionStudentRepository.save(studentEntity);

        SldMove sldMove = new SldMove();
        sldMove.setToStudentPen(toPen);
        sldMove.setSdcSchoolCollectionIdsToUpdate(List.of(studentEntity.getSdcSchoolCollectionStudentID()));

        this.mockMvc
            .perform(
                post(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT + "/move-sld")
                    .contentType(APPLICATION_JSON)
                    .content(asJsonString(sldMove))
                    .with(mockAuthority))
                    .andDo(print())
                    .andExpect(status().isOk());

        var savedEntity = sdcSchoolCollectionStudentRepository.findAll();
        assertThat(savedEntity).hasSize(1);
        assertThat(savedEntity.get(0).getAssignedStudentId()).isEqualTo(randomStudentID);
        assertThat(savedEntity.get(0).getAssignedPen()).isEqualTo(toPen);
    }
}
