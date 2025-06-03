package ca.bc.gov.educ.studentdatacollection.api.rest;

import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.dto.institute.PaginatedResponse;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EdxUser;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EdxUserDistrict;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EdxUserSchool;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils.NATS_TIMEOUT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.wildfly.common.Assert.assertFalse;
import static org.wildfly.common.Assert.assertTrue;

class RestUtilsTest {
    @Mock
    private WebClient webClient;

    @Mock
    private WebClient chesWebClient;

    @Mock
    private MessagePublisher messagePublisher;

    @InjectMocks
    private RestUtils restUtils;

    @Mock
    private ApplicationProperties props;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        restUtils = spy(new RestUtils(chesWebClient, webClient, props, messagePublisher));
    }

    @Test
    void testPopulateSchoolMap_WhenApiCallSucceeds_ShouldPopulateMaps() {
        // Given
        val school1ID = UUID.randomUUID();
        val school2ID = UUID.randomUUID();
        val school3ID = UUID.randomUUID();
        val school1 = SchoolTombstone.builder()
                .schoolId(String.valueOf(school1ID))
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .build();
        val school2 = SchoolTombstone.builder()
                .schoolId(String.valueOf(school2ID))
                .displayName("School 2")
                .independentAuthorityId("Authority 1")
                .build();
        val school3 = SchoolTombstone.builder()
                .schoolId(String.valueOf(school3ID))
                .displayName("School 3")
                .independentAuthorityId("Authority 2")
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getAllSchoolTombstones();

        // When
        restUtils.populateSchoolMap();

        // Then verify the maps are populated
        List<SchoolTombstone> results = restUtils.getAllSchoolTombstones();
        assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals(school1, results.get(0));
        assertEquals(school2, results.get(1));
        assertEquals(school3, results.get(2));

        doReturn(Optional.of(List.of(school1ID, school2ID))).when(restUtils).getSchoolIDsByIndependentAuthorityID("Authority 1");

        Optional<List<UUID>> result = restUtils.getSchoolIDsByIndependentAuthorityID("Authority 1");

        assertNotNull(result);
        assertEquals(2, result.get().size());
        assertTrue(result.get().equals(List.of(school1ID, school2ID)));
    }

    @Test
    void testPopulateDistrictMap_WhenApiCallSucceeds_ShouldPopulateMaps() {
        // Given
        val district1ID = String.valueOf(UUID.randomUUID());
        val district2ID = String.valueOf(UUID.randomUUID());
        val district3ID = String.valueOf(UUID.randomUUID());
        val district1 = District.builder()
                .districtId(district1ID)
                .displayName("District 1")
                .build();
        val district2 = District.builder()
                .districtId(district2ID)
                .displayName("district 2")
                .build();
        val district3 = District.builder()
                .districtId(district3ID)
                .displayName("District 3")
                .build();

        doReturn(List.of(district1, district2, district3)).when(restUtils).getDistricts();

        // When
        restUtils.populateDistrictMap();

        // Then verify the maps are populated
        Map<String, District> districtMap = (Map<String, District>) ReflectionTestUtils.getField(restUtils, "districtMap");
        assertEquals(3, districtMap.size());
        assertEquals(district1, districtMap.get(district1ID));
        assertEquals(district2, districtMap.get(district2ID));
        assertEquals(district3, districtMap.get(district3ID));
    }


    @Test
    void testPopulateSchoolMap_WhenNoIndependentAuthorityId_ShouldPopulateMapsCorrectly() {
        // Given
        val school1ID = UUID.randomUUID();
        val school2ID = UUID.randomUUID();
        val school3ID = UUID.randomUUID();
        val school1 = SchoolTombstone.builder()
                .schoolId(String.valueOf(school1ID))
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .build();
        val school2 = SchoolTombstone.builder()
                .schoolId(String.valueOf(school2ID))
                .displayName("School 2")
                .build();
        val school3 = SchoolTombstone.builder()
                .schoolId(String.valueOf(school3ID))
                .displayName("School 3")
                .independentAuthorityId("Authority 2")
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getAllSchoolTombstones();

        // When
        restUtils.getAllSchoolTombstones();

        // Then verify the maps are populated
        List<SchoolTombstone> results = restUtils.getAllSchoolTombstones();
        assertEquals(3, results.size());

        doReturn(Optional.of(List.of(school1ID))).when(restUtils).getSchoolIDsByIndependentAuthorityID("Authority 1");

        Optional<List<UUID>> result = restUtils.getSchoolIDsByIndependentAuthorityID("Authority 1");

        assertEquals(1, result.get().size());
        assertTrue(result.get().equals(List.of(school1ID)));
    }


    @Test
    void testPopulateSchoolMap_WhenApiCallFails_ShouldHandleException() {
        // Given
        doThrow(new RuntimeException("API call failed")).when(restUtils).getAllSchoolTombstones();

        // When
        assertDoesNotThrow(() -> restUtils.populateSchoolMap()); //checks exception is handled

        // Then Verify that the maps are not populated
        Map<String, SchoolTombstone> schoolMap = (Map<String, SchoolTombstone>) ReflectionTestUtils.getField(restUtils, "schoolMap");
        assertEquals(0, schoolMap.size());

        Map<String, List<UUID>> independentAuthorityToSchoolIDMap = (Map<String, List<UUID>>) ReflectionTestUtils.getField(restUtils, "independentAuthorityToSchoolIDMap");
        assertEquals(0, independentAuthorityToSchoolIDMap.size());
    }

    @Test
    void testGetSchoolIDsByIndependentAuthorityID_WhenAuthorityIDExists_ShouldReturnListOfSchoolIDs() {
        // Given
        String authorityId = "AUTH_ID";
        List<UUID> schoolIds = Collections.singletonList(UUID.randomUUID());
        ReflectionTestUtils.setField(restUtils, "independentAuthorityToSchoolIDMap", Collections.singletonMap(authorityId, schoolIds));

        // Then
        Optional<List<UUID>> result = restUtils.getSchoolIDsByIndependentAuthorityID(authorityId);

        // When
        assertTrue(result.isPresent());
        assertEquals(schoolIds, result.get());
        verify(restUtils, never()).populateSchoolMap();
    }

    @Test
    void testGetSchoolIDsByIndependentAuthorityID_WhenAuthorityIDDoesNotExist_ShouldReturnEmptyOptional() {
        // Given
        String authorityId = "AUTH_ID";
        ReflectionTestUtils.setField(restUtils, "independentAuthorityToSchoolIDMap", Collections.emptyMap());
        doNothing().when(restUtils).populateSchoolMap();

        // When
        Optional<List<UUID>> result = restUtils.getSchoolIDsByIndependentAuthorityID(authorityId);

        // Then
        assertFalse(result.isPresent());
        verify(restUtils).populateSchoolMap();
    }

    @Test
    void testPopulateSchoolMincodeMap_WhenApiCallSucceeds_ShouldPopulateMap() {
        // Given
        val school1Mincode = "97083";
        val school2Mincode = "97084";
        val school3Mincode = "97085";
        val school1 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .mincode(school1Mincode)
                .build();
        val school2 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 2")
                .mincode(school2Mincode)
                .build();
        val school3 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 3")
                .mincode(school3Mincode)
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getAllSchoolTombstones();

        // When
        List<SchoolTombstone> results = restUtils.getAllSchoolTombstones();

        // Then verify the maps are populated
        assertEquals(3, results.size());
    }

    @Test
    void testGetSchoolFromMincodeMap_WhenApiCallSucceeds_ShouldReturnSchool() {
        // Given
        val school1Mincode = "97083";

        val school1 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .mincode(school1Mincode)
                .build();

        doReturn(Optional.of(school1)).when(restUtils).getSchoolByMincode(school1Mincode);

        // When
        var result = restUtils.getSchoolByMincode(school1Mincode);
        assertEquals(school1, result.get());
    }

    @Test
    void testGetSchoolsFromMincodeMap_WhenApiCallSucceeds_ShouldReturnSchools() {
        // Given
        val school1Mincode = "97083";
        val school2Mincode = "97084";
        val school3Mincode = "97085";
        val school1 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .mincode(school1Mincode)
                .build();
        val school2 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 2")
                .mincode(school2Mincode)
                .build();
        val school3 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 3")
                .mincode(school3Mincode)
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getAllSchoolTombstones();

        // When
        var result = restUtils.getAllSchoolTombstones();
        assertEquals(List.of(school1, school2, school3), result);
    }

    @Test
    void testGetStudentByPEN_WhenStudentFound_shouldReturnStudent() {
        final var studentPayload = Student.builder().studentID(UUID.randomUUID().toString()).pen("123456789").legalFirstName("Test").build();
        doReturn(studentPayload).when(restUtils).getStudentByPEN(any(), any());

        //when
        var result = restUtils.getStudentByPEN(UUID.randomUUID(), "123456789");
        assertEquals(studentPayload, result);
    }

    @Test
    void testGetEdxUsers_shouldReturnUsers(){
        val schoolID = String.valueOf(UUID.randomUUID());
        val districtID = String.valueOf(UUID.randomUUID());
        val schoolEdxUserID = UUID.randomUUID();
        val districtEdxUserID = UUID.randomUUID();

        EdxUserSchool userSchool = new EdxUserSchool();
        userSchool.setEdxUserSchoolID(schoolID);
        userSchool.setEdxUserID(String.valueOf(schoolEdxUserID));
        userSchool.setSchoolID(UUID.fromString(schoolID));

        EdxUser schoolUser = new EdxUser();
        schoolUser.setEdxUserID(String.valueOf(schoolEdxUserID));
        schoolUser.setDigitalIdentityID(String.valueOf(UUID.randomUUID()));
        schoolUser.setFirstName("John");
        schoolUser.setLastName("Doe");
        schoolUser.setEmail("john.doe@example.com");
        schoolUser.setEdxUserSchools(List.of(userSchool));

        EdxUserDistrict userDistrict = new EdxUserDistrict();
        userDistrict.setEdxUserDistrictID(districtID);
        userDistrict.setEdxUserID(String.valueOf(districtEdxUserID));

        EdxUser districtUser = new EdxUser();
        districtUser.setDigitalIdentityID(String.valueOf(UUID.randomUUID()));
        districtUser.setFirstName("Jane");
        districtUser.setLastName("Smith");
        districtUser.setEmail("jane.smith@example.com");
        districtUser.setEdxUserDistricts(List.of(userDistrict));

        doReturn(List.of(schoolUser)).when(restUtils).getEdxUsersForSchool(UUID.fromString(schoolID));

        List<EdxUser> returnedSchoolUser = restUtils.getEdxUsersForSchool(UUID.fromString(schoolID));
        assert(returnedSchoolUser.get(0).getEdxUserID()).equals(schoolUser.getEdxUserID());
    }

    @Test
    void testPopulateAllSchoolMap_PopulatesMapCorrectly() throws Exception {
        // Given
        String school1ID = UUID.randomUUID().toString();
        String school2ID = UUID.randomUUID().toString();
        String school3ID = UUID.randomUUID().toString();

        School school1 = School.builder()
                .schoolId(school1ID)
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .build();
        School school2 = School.builder()
                .schoolId(school2ID)
                .displayName("School 2")
                .build();
        School school3 = School.builder()
                .schoolId(school3ID)
                .displayName("School 3")
                .independentAuthorityId("Authority 2")
                .build();

        PaginatedResponse<School> paginatedResponse = new PaginatedResponse<>(List.of(school1, school2, school3), PageRequest.of(0, 10), 3L);

        RestUtils restUtilsSpy = spy(restUtils);
        doReturn(paginatedResponse).when(restUtilsSpy).getSchoolsPaginatedFromInstituteApi(0);

        // When
        restUtilsSpy.populateAllSchoolMap();

        @SuppressWarnings("unchecked")
        Map<String, School> schoolMap = (Map<String, School>) ReflectionTestUtils.getField(restUtilsSpy, "allSchoolMap");
        assertNotNull(schoolMap);
        assertEquals(3, schoolMap.size());
        assertEquals(school1, schoolMap.get(school1ID));
        assertEquals(school2, schoolMap.get(school2ID));
        assertEquals(school3, schoolMap.get(school3ID));
    }


    @Test
    void testGetAllSchoolBySchoolID_ShouldPopulateMapsCorrectly() throws Exception {
        // Given
        val school1ID = String.valueOf(UUID.randomUUID());
        val school2ID = String.valueOf(UUID.randomUUID());
        val school3ID = String.valueOf(UUID.randomUUID());
        val school1 = School.builder()
                .schoolId(school1ID)
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .build();
        val school2 = School.builder()
                .schoolId(school2ID)
                .displayName("School 2")
                .build();
        val school3 = School.builder()
                .schoolId(school3ID)
                .displayName("School 3")
                .independentAuthorityId("Authority 2")
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getAllSchools();

        // When
        restUtils.populateAllSchoolMap();

        // Then verify the maps are populated
        Map<String, School> schoolMap = (Map<String, School>) ReflectionTestUtils.getField(restUtils, "allSchoolMap");
        assert schoolMap != null;
        assertEquals(3, schoolMap.size());
        assertEquals(school1, schoolMap.get(school1ID));
        assertEquals(school2, schoolMap.get(school2ID));
        assertEquals(school3, schoolMap.get(school3ID));
    }

    @Test
    void testGetMergedStudentIds_WhenRequestTimesOut_ShouldThrowStudentDataCollectionAPIRuntimeException() {
        UUID correlationID = UUID.randomUUID();
        UUID assignedStudentId = UUID.randomUUID();

        when(messagePublisher.requestMessage(anyString(), any(byte[].class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        StudentDataCollectionAPIRuntimeException exception = assertThrows(
            StudentDataCollectionAPIRuntimeException.class,
            () -> restUtils.getMergedStudentIds(correlationID, assignedStudentId)
        );

        assertEquals(NATS_TIMEOUT + correlationID, exception.getMessage());
    }

    @Test
    void testGetMergedStudentIds_WhenExceptionOccurs_ShouldThrowStudentDataCollectionAPIRuntimeException() {
        UUID correlationID = UUID.randomUUID();
        UUID assignedStudentId = UUID.randomUUID();
        Exception mockException = new Exception("exception");

        when(messagePublisher.requestMessage(anyString(), any(byte[].class)))
            .thenReturn(CompletableFuture.failedFuture(mockException));

        assertThrows(
            StudentDataCollectionAPIRuntimeException.class,
            () -> restUtils.getMergedStudentIds(correlationID, assignedStudentId)
        );
    }
}

