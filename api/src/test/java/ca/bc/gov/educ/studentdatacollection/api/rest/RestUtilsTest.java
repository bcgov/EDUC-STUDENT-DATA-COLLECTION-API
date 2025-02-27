package ca.bc.gov.educ.studentdatacollection.api.rest;

import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SchoolListService;
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

    @Mock
    private SchoolListService schoolListService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        restUtils = spy(new RestUtils(chesWebClient, webClient, props, messagePublisher, schoolListService));
    }

    @Test
    void testPopulateSchoolMap_WhenApiCallSucceeds_ShouldPopulateMaps() {
        // Given
        val school1ID = String.valueOf(UUID.randomUUID());
        val school2ID = String.valueOf(UUID.randomUUID());
        val school3ID = String.valueOf(UUID.randomUUID());
        val school1 = SchoolTombstone.builder()
                .schoolId(school1ID)
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .build();
        val school2 = SchoolTombstone.builder()
                .schoolId(school2ID)
                .displayName("School 2")
                .independentAuthorityId("Authority 1")
                .build();
        val school3 = SchoolTombstone.builder()
                .schoolId(school3ID)
                .displayName("School 3")
                .independentAuthorityId("Authority 2")
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getSchools();

        // When
        restUtils.populateSchoolMap();

        // Then verify the maps are populated
        Map<String, SchoolTombstone> schoolMap = (Map<String, SchoolTombstone>) ReflectionTestUtils.getField(restUtils, "schoolMap");
        assertEquals(3, schoolMap.size());
        assertEquals(school1, schoolMap.get(school1ID));
        assertEquals(school2, schoolMap.get(school2ID));
        assertEquals(school3, schoolMap.get(school3ID));

        Map<String, List<UUID>> independentAuthorityToSchoolIDMap = (Map<String, List<UUID>>) ReflectionTestUtils.getField(restUtils, "independentAuthorityToSchoolIDMap");
        assertEquals(2, independentAuthorityToSchoolIDMap.size());
        assertTrue(independentAuthorityToSchoolIDMap.containsKey("Authority 1"));
        assertTrue(independentAuthorityToSchoolIDMap.containsKey("Authority 2"));
        assertEquals(2, independentAuthorityToSchoolIDMap.get("Authority 1").size());
        assertEquals(1, independentAuthorityToSchoolIDMap.get("Authority 2").size());
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
        val school1ID = String.valueOf(UUID.randomUUID());
        val school2ID = String.valueOf(UUID.randomUUID());
        val school3ID = String.valueOf(UUID.randomUUID());
        val school1 = SchoolTombstone.builder()
                .schoolId(school1ID)
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .build();
        val school2 = SchoolTombstone.builder()
                .schoolId(school2ID)
                .displayName("School 2")
                .build();
        val school3 = SchoolTombstone.builder()
                .schoolId(school3ID)
                .displayName("School 3")
                .independentAuthorityId("Authority 2")
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getSchools();

        // When
        restUtils.populateSchoolMap();

        // Then verify the maps are populated
        Map<String, SchoolTombstone> schoolMap = (Map<String, SchoolTombstone>) ReflectionTestUtils.getField(restUtils, "schoolMap");
        assertEquals(3, schoolMap.size());
        assertEquals(school1, schoolMap.get(school1ID));
        assertEquals(school2, schoolMap.get(school2ID));
        assertEquals(school3, schoolMap.get(school3ID));

        Map<String, List<UUID>> independentAuthorityToSchoolIDMap = (Map<String, List<UUID>>) ReflectionTestUtils.getField(restUtils, "independentAuthorityToSchoolIDMap");
        assertEquals(2, independentAuthorityToSchoolIDMap.size());
        assertTrue(independentAuthorityToSchoolIDMap.containsKey("Authority 1"));
        assertTrue(independentAuthorityToSchoolIDMap.containsKey("Authority 2"));
        assertEquals(1, independentAuthorityToSchoolIDMap.get("Authority 1").size());
        assertEquals(1, independentAuthorityToSchoolIDMap.get("Authority 2").size());
    }


    @Test
    void testPopulateSchoolMap_WhenApiCallFails_ShouldHandleException() {
        // Given
        doThrow(new RuntimeException("API call failed")).when(restUtils).getSchools();

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

        doReturn(List.of(school1, school2, school3)).when(restUtils).getSchools();

        // When
        restUtils.populateSchoolMincodeMap();

        // Then verify the maps are populated
        Map<String, SchoolTombstone> schoolMincodeMap = (Map<String, SchoolTombstone>) ReflectionTestUtils.getField(restUtils, "schoolMincodeMap");
        assertEquals(3, schoolMincodeMap.size());
        assertEquals(school1, schoolMincodeMap.get(school1Mincode));
        assertEquals(school2, schoolMincodeMap.get(school2Mincode));
        assertEquals(school3, schoolMincodeMap.get(school3Mincode));

    }

    @Test
    void testGetSchoolFromMincodeMap_WhenApiCallSucceeds_ShouldReturnSchool() {
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

        doReturn(List.of(school1, school2, school3)).when(restUtils).getSchools();

        // When
        var result = restUtils.getSchoolByMincode(school1Mincode);
        assertEquals(school1, result.get());
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

        doReturn(List.of(schoolUser, districtUser)).when(restUtils).getEdxUsers();

        List<EdxUser> returnedSchoolUser = restUtils.getEdxUsersForSchool(UUID.fromString(schoolID));
        assert(returnedSchoolUser.get(0).getEdxUserID()).equals(schoolUser.getEdxUserID());
    }

//    @Test
//    void testGetAllSchoolBySchoolID_ShouldPopulateMapsCorrectly() throws Exception {
//        // Given
//        val school1ID = String.valueOf(UUID.randomUUID());
//        val school2ID = String.valueOf(UUID.randomUUID());
//        val school3ID = String.valueOf(UUID.randomUUID());
//        val school1 = School.builder()
//                .schoolId(school1ID)
//                .displayName("School 1")
//                .independentAuthorityId("Authority 1")
//                .build();
//        val school2 = School.builder()
//                .schoolId(school2ID)
//                .displayName("School 2")
//                .build();
//        val school3 = School.builder()
//                .schoolId(school3ID)
//                .displayName("School 3")
//                .independentAuthorityId("Authority 2")
//                .build();
//
//        doReturn(List.of(school1, school2, school3)).when(restUtils).getAllSchoolList(any(), any());
//
//        // When
//        restUtils.populateAllSchoolMap();
//
//        // Then verify the maps are populated
//        Map<String, School> schoolMap = (Map<String, School>) ReflectionTestUtils.getField(restUtils, "allSchoolMap");
//        assert schoolMap != null;
//        assertEquals(3, schoolMap.size());
//        assertEquals(school1, schoolMap.get(school1ID));
//        assertEquals(school2, schoolMap.get(school2ID));
//        assertEquals(school3, schoolMap.get(school3ID));
//    }

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
