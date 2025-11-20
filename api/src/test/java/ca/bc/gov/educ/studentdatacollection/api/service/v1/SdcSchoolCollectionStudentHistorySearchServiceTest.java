package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentHistoryPaginationRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentHistoryRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Search;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValueType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static ca.bc.gov.educ.studentdatacollection.api.struct.v1.Condition.AND;
import static ca.bc.gov.educ.studentdatacollection.api.struct.v1.Condition.OR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SdcSchoolCollectionStudentHistorySearchServiceTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private SdcSchoolCollectionStudentHistorySearchService searchService;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    @Autowired
    private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

    @Autowired
    private SdcSchoolCollectionStudentHistoryPaginationRepository historyPaginationRepository;

    @Autowired
    private SdcSchoolCollectionStudentHistoryRepository historyRepository;

    @Autowired
    private RestUtils restUtils;

    private SdcSchoolCollectionEntity sdcSchoolCollection;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final SdcSchoolCollectionStudentMapper mapper = SdcSchoolCollectionStudentMapper.mapper;
    @Autowired
    private SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;

    /**
     * Helper method to create a history record from a student entity
     */
    private SdcSchoolCollectionStudentHistoryEntity createHistoryFromStudent(SdcSchoolCollectionStudentEntity student) {
        var history = new SdcSchoolCollectionStudentHistoryEntity();
        history.setSdcSchoolCollectionStudentID(student.getSdcSchoolCollectionStudentID());
        history.setSdcSchoolCollectionID(student.getSdcSchoolCollection().getSdcSchoolCollectionID());
        history.setLocalID(student.getLocalID());
        history.setStudentPen(student.getStudentPen());
        history.setLegalFirstName(student.getLegalFirstName());
        history.setLegalMiddleNames(student.getLegalMiddleNames());
        history.setLegalLastName(student.getLegalLastName());
        history.setUsualFirstName(student.getUsualFirstName());
        history.setUsualMiddleNames(student.getUsualMiddleNames());
        history.setUsualLastName(student.getUsualLastName());
        history.setDob(student.getDob());
        history.setGender(student.getGender());
        history.setSpecialEducationCategoryCode(student.getSpecialEducationCategoryCode());
        history.setSchoolFundingCode(student.getSchoolFundingCode());
        history.setNativeAncestryInd(student.getNativeAncestryInd());
        history.setHomeLanguageSpokenCode(student.getHomeLanguageSpokenCode());
        history.setOtherCourses(student.getOtherCourses());
        history.setSupportBlocks(student.getSupportBlocks());
        history.setEnrolledGradeCode(student.getEnrolledGradeCode());
        history.setEnrolledProgramCodes(student.getEnrolledProgramCodes());
        history.setCreateUser(student.getCreateUser());
        history.setCreateDate(student.getCreateDate());
        history.setUpdateUser(student.getUpdateUser());
        history.setUpdateDate(student.getUpdateDate());
        return history;
    }

    @BeforeEach
    void setup() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcSchoolCollection = sdcSchoolCollectionRepository.save(sdcMockSchool);
    }

    @AfterEach
    void cleanup() {
        sdcSchoolCollectionStudentRepository.deleteAll();
        sdcSchoolCollectionStudentHistoryRepository.deleteAll();
        sdcSchoolCollectionRepository.deleteAll();
        collectionRepository.deleteAll();
    }

    @Test
    void testFindAll_WithEmptySpecification_ShouldReturnAllHistoryRecords() throws ExecutionException, InterruptedException {
        // Given
        var student = createMockSchoolStudentEntity(sdcSchoolCollection);
        student.setLegalFirstName("JOHN");
        var savedStudent = sdcSchoolCollectionStudentRepository.save(student);

        var history = createHistoryFromStudent(savedStudent);
        historyRepository.save(history);

        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(new Sort.Order(Sort.Direction.DESC, "createDate"));

        // When
        var result = searchService.findAll(null, 0, 10, sorts).get();

        // Then
        assertNotNull(result);
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    void testFindAll_WithPagination_ShouldReturnPaginatedResults() throws ExecutionException, InterruptedException {
        // Given
        for (int i = 0; i < 15; i++) {
            var student = createMockSchoolStudentEntity(sdcSchoolCollection);
            student.setLegalFirstName("STUDENT" + i);
            var savedStudent = sdcSchoolCollectionStudentRepository.save(student);

            var history = createHistoryFromStudent(savedStudent);
            historyRepository.save(history);
        }

        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(new Sort.Order(Sort.Direction.ASC, "legalFirstName"));

        // When
        var page1 = searchService.findAll(null, 0, 5, sorts).get();
        var page2 = searchService.findAll(null, 1, 5, sorts).get();

        // Then
        assertNotNull(page1);
        assertNotNull(page2);
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page1.getNumber()).isEqualTo(0);
        assertThat(page2.getNumber()).isEqualTo(1);
    }

    @Test
    void testSetSpecificationAndSortCriteria_WithValidSearchCriteria_ShouldBuildSpecification() throws Exception {
        // Given
        var student = createMockSchoolStudentEntity(sdcSchoolCollection);
        student.setLegalFirstName("JANE");
        student.setLegalLastName("DOE");
        var savedStudent = sdcSchoolCollectionStudentRepository.save(student);

        var history = createHistoryFromStudent(savedStudent);
        historyRepository.save(history);

        final SearchCriteria criteria = SearchCriteria.builder()
                .condition(null)
                .key("legalLastName")
                .operation(FilterOperation.EQUAL)
                .value("DOE")
                .valueType(ValueType.STRING)
                .build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);
        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        List<Sort.Order> sorts = new ArrayList<>();

        // When
        Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> spec =
                searchService.setSpecificationAndSortCriteria("", criteriaJSON, objectMapper, sorts);

        // Then
        assertNotNull(spec);
        var result = searchService.findAll(spec, 0, 10, sorts).get();
        assertNotNull(result);
    }

    @Test
    void testSetSpecificationAndSortCriteria_WithMultipleCriteria_ShouldBuildComplexSpecification() throws Exception {
        // Given
        var student = createMockSchoolStudentEntity(sdcSchoolCollection);
        student.setLegalFirstName("ALICE");
        student.setLegalLastName("SMITH");
        student.setEnrolledGradeCode("10");
        var savedStudent = sdcSchoolCollectionStudentRepository.save(student);

        var history = createHistoryFromStudent(savedStudent);
        historyRepository.save(history);

        final SearchCriteria criteria1 = SearchCriteria.builder()
                .condition(AND)
                .key("legalLastName")
                .operation(FilterOperation.EQUAL)
                .value("SMITH")
                .valueType(ValueType.STRING)
                .build();

        final SearchCriteria criteria2 = SearchCriteria.builder()
                .condition(AND)
                .key("enrolledGradeCode")
                .operation(FilterOperation.EQUAL)
                .value("10")
                .valueType(ValueType.STRING)
                .build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria1);
        criteriaList.add(criteria2);
        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        List<Sort.Order> sorts = new ArrayList<>();

        // When
        Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> spec =
                searchService.setSpecificationAndSortCriteria("", criteriaJSON, objectMapper, sorts);

        // Then
        assertNotNull(spec);
        var result = searchService.findAll(spec, 0, 10, sorts).get();
        assertNotNull(result);
    }

    @Test
    void testSetSpecificationAndSortCriteria_WithORCondition_ShouldBuildORSpecification() throws Exception {
        // Given
        var student1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        student1.setLegalFirstName("ROBERT");
        var savedStudent1 = sdcSchoolCollectionStudentRepository.save(student1);
        historyRepository.save(createHistoryFromStudent(savedStudent1));

        var student2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        student2.setLegalFirstName("WILLIAM");
        var savedStudent2 = sdcSchoolCollectionStudentRepository.save(student2);
        historyRepository.save(createHistoryFromStudent(savedStudent2));

        final SearchCriteria criteria1 = SearchCriteria.builder()
                .condition(OR)
                .key("legalFirstName")
                .operation(FilterOperation.CONTAINS)
                .value("ROBERT")
                .valueType(ValueType.STRING)
                .build();

        final SearchCriteria criteria2 = SearchCriteria.builder()
                .condition(OR)
                .key("legalFirstName")
                .operation(FilterOperation.CONTAINS)
                .value("WILLIAM")
                .valueType(ValueType.STRING)
                .build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria1);
        criteriaList.add(criteria2);
        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        List<Sort.Order> sorts = new ArrayList<>();

        // When
        Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> spec =
                searchService.setSpecificationAndSortCriteria("", criteriaJSON, objectMapper, sorts);

        // Then
        assertNotNull(spec);
        var result = searchService.findAll(spec, 0, 10, sorts).get();
        assertNotNull(result);
    }

    @Test
    void testSetSpecificationAndSortCriteria_WithSortCriteria_ShouldBuildSortOrders() throws Exception {
        // Given
        var student1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        student1.setLegalLastName("ADAMS");
        var savedStudent1 = sdcSchoolCollectionStudentRepository.save(student1);
        historyRepository.save(createHistoryFromStudent(savedStudent1));

        var student2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        student2.setLegalLastName("ZULU");
        var savedStudent2 = sdcSchoolCollectionStudentRepository.save(student2);
        historyRepository.save(createHistoryFromStudent(savedStudent2));

        final Map<String, String> sortMap = new LinkedHashMap<>();
        sortMap.put("legalLastName", "ASC");

        final String sortJSON = objectMapper.writeValueAsString(sortMap);
        List<Sort.Order> actualSorts = new ArrayList<>();

        // When
        Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> spec =
                searchService.setSpecificationAndSortCriteria(sortJSON, "", objectMapper, actualSorts);

        // Then
        assertThat(actualSorts).isNotEmpty();
        assertThat(actualSorts.get(0).getProperty()).isEqualTo("legalLastName");
        assertThat(actualSorts.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void testSetSpecificationAndSortCriteria_WithEmptyCriteria_ShouldReturnNullSpecification() throws Exception {
        // Given
        List<Sort.Order> sorts = new ArrayList<>();

        // When
        Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> spec =
                searchService.setSpecificationAndSortCriteria("", "", objectMapper, sorts);

        // Then
        assertNull(spec);
    }

    @Test
    void testFindAll_WithUUIDFilter_ShouldReturnMatchingRecords() throws Exception {
        // Given
        var student = createMockSchoolStudentEntity(sdcSchoolCollection);
        student.setLegalFirstName("TESTUSER");
        var savedStudent = sdcSchoolCollectionStudentRepository.save(student);

        historyRepository.save(createHistoryFromStudent(savedStudent));

        final SearchCriteria criteria = SearchCriteria.builder()
                .condition(null)
                .key("sdcSchoolCollectionStudentID")
                .operation(FilterOperation.EQUAL)
                .value(savedStudent.getSdcSchoolCollectionStudentID().toString())
                .valueType(ValueType.UUID)
                .build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);
        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        List<Sort.Order> sorts = new ArrayList<>();

        // When
        Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> spec =
                searchService.setSpecificationAndSortCriteria("", criteriaJSON, objectMapper, sorts);
        var result = searchService.findAll(spec, 0, 10, sorts).get();

        // Then
        assertNotNull(result);
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    void testFindAll_WithContainsFilter_ShouldReturnMatchingRecords() throws Exception {
        // Given
        var student = createMockSchoolStudentEntity(sdcSchoolCollection);
        student.setLegalFirstName("CHRISTOPHER");
        var savedStudent = sdcSchoolCollectionStudentRepository.save(student);

        historyRepository.save(createHistoryFromStudent(savedStudent));

        final SearchCriteria criteria = SearchCriteria.builder()
                .condition(null)
                .key("legalFirstName")
                .operation(FilterOperation.CONTAINS)
                .value("CHRIS")
                .valueType(ValueType.STRING)
                .build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);
        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        List<Sort.Order> sorts = new ArrayList<>();

        // When
        Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> spec =
                searchService.setSpecificationAndSortCriteria("", criteriaJSON, objectMapper, sorts);
        var result = searchService.findAll(spec, 0, 10, sorts).get();

        // Then
        assertNotNull(result);
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    void testFindAll_WithNotEqualFilter_ShouldReturnMatchingRecords() throws Exception {
        // Given
        var student1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        student1.setEnrolledGradeCode("10");
        var savedStudent1 = sdcSchoolCollectionStudentRepository.save(student1);
        historyRepository.save(createHistoryFromStudent(savedStudent1));

        var student2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        student2.setEnrolledGradeCode("11");
        var savedStudent2 = sdcSchoolCollectionStudentRepository.save(student2);
        historyRepository.save(createHistoryFromStudent(savedStudent2));

        final SearchCriteria criteria = SearchCriteria.builder()
                .condition(null)
                .key("enrolledGradeCode")
                .operation(FilterOperation.NOT_EQUAL)
                .value("10")
                .valueType(ValueType.STRING)
                .build();

        final List<SearchCriteria> criteriaList = new ArrayList<>();
        criteriaList.add(criteria);
        final List<Search> searches = new LinkedList<>();
        searches.add(Search.builder().searchCriteriaList(criteriaList).build());

        final String criteriaJSON = objectMapper.writeValueAsString(searches);
        List<Sort.Order> sorts = new ArrayList<>();

        // When
        Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> spec =
                searchService.setSpecificationAndSortCriteria("", criteriaJSON, objectMapper, sorts);
        var result = searchService.findAll(spec, 0, 10, sorts).get();

        // Then
        assertNotNull(result);
    }

    @Test
    void testFindAll_WithMultipleSortOrders_ShouldApplySorting() throws Exception {
        // Given
        var student1 = createMockSchoolStudentEntity(sdcSchoolCollection);
        student1.setLegalLastName("SMITH");
        student1.setLegalFirstName("ADAM");
        var savedStudent1 = sdcSchoolCollectionStudentRepository.save(student1);
        historyRepository.save(createHistoryFromStudent(savedStudent1));

        var student2 = createMockSchoolStudentEntity(sdcSchoolCollection);
        student2.setLegalLastName("SMITH");
        student2.setLegalFirstName("ZACK");
        var savedStudent2 = sdcSchoolCollectionStudentRepository.save(student2);
        historyRepository.save(createHistoryFromStudent(savedStudent2));

        final Map<String, String> sortMap = new LinkedHashMap<>();
        sortMap.put("legalLastName", "ASC");
        sortMap.put("legalFirstName", "DESC");

        final String sortJSON = objectMapper.writeValueAsString(sortMap);
        List<Sort.Order> actualSorts = new ArrayList<>();

        // When
        Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> spec =
                searchService.setSpecificationAndSortCriteria(sortJSON, "", objectMapper, actualSorts);
        var result = searchService.findAll(spec, 0, 10, actualSorts).get();

        // Then
        assertNotNull(result);
        assertThat(actualSorts).hasSize(2);
    }
}


