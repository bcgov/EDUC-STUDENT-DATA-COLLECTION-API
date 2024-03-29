package ca.bc.gov.educ.studentdatacollection.api.rest;

import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.wildfly.common.Assert.assertFalse;
import static org.wildfly.common.Assert.assertTrue;

class RestUtilsTest {
    @Mock
    private WebClient webClient;

    @Mock
    private MessagePublisher messagePublisher;

    @InjectMocks
    private RestUtils restUtils;

    @Mock
    private ApplicationProperties props;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        restUtils = spy(new RestUtils(webClient, props, messagePublisher));
    }

    @Test
    void testPopulateSchoolMap_WhenApiCallSucceeds_ShouldPopulateMaps() {
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
                .independentAuthorityId("Authority 1")
                .build();
        val school3 = School.builder()
                .schoolId(school3ID)
                .displayName("School 3")
                .independentAuthorityId("Authority 2")
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getSchools();

        // When
        restUtils.populateSchoolMap();

        // Then verify the maps are populated
        Map<String, School> schoolMap = (Map<String, School>) ReflectionTestUtils.getField(restUtils, "schoolMap");
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

        doReturn(List.of(school1, school2, school3)).when(restUtils).getSchools();

        // When
        restUtils.populateSchoolMap();

        // Then verify the maps are populated
        Map<String, School> schoolMap = (Map<String, School>) ReflectionTestUtils.getField(restUtils, "schoolMap");
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
        Map<String, School> schoolMap = (Map<String, School>) ReflectionTestUtils.getField(restUtils, "schoolMap");
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
}
