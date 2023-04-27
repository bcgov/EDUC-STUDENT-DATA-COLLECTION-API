package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValueType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    RestUtils restUtils;

    @AfterEach
    void cleanup(){
        sdcSchoolCollectionStudentRepository.deleteAll();
        sdcSchoolCollectionRepository.deleteAll();
        collectionRepository.deleteAll();
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
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        var sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);

        var stud1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        stud1.setLegalFirstName("JAM");
        var stud2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        sdcSchoolCollectionStudentRepository.save(stud1);
        sdcSchoolCollectionStudentRepository.save(stud2);
        final SearchCriteria criteria = SearchCriteria.builder().key("legalFirstName").operation(FilterOperation.EQUAL).value("JAM").valueType(ValueType.STRING).build();
        final SearchCriteria criteria2 = SearchCriteria.builder().key("gender").operation(FilterOperation.EQUAL).value("M").valueType(ValueType.STRING).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);
        criteriaList.add(criteria2);

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
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
        final SearchCriteria criteriaDate = SearchCriteria.builder().key("createDate").operation(FilterOperation.BETWEEN).value(yesterday + "," + tomorrow).valueType(ValueType.DATE_TIME).build();
        final SearchCriteria criteria = SearchCriteria.builder().key("legalFirstName").operation(FilterOperation.EQUAL).value("JAM").valueType(ValueType.STRING).build();
        final SearchCriteria criteria2 = SearchCriteria.builder().key("gender").operation(FilterOperation.EQUAL).value("M").valueType(ValueType.STRING).build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteriaDate);
        criteriaList.add(criteria);
        criteriaList.add(criteria2);

        final var objectMapper = new ObjectMapper();
        final String criteriaJSON = objectMapper.writeValueAsString(criteriaList);
        final MvcResult result = this.mockMvc
                .perform(get(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT+URL.PAGINATED)
                        .with(jwt().jwt((jwt) -> jwt.claim("scope", "READ_SDC_SCHOOL_COLLECTION_STUDENT")))
                        .param("searchCriteriaList", criteriaJSON)
                        .contentType(APPLICATION_JSON))
                .andReturn();
        this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
    }

}
