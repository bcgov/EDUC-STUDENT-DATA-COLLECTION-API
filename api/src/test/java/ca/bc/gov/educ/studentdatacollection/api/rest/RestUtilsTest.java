package ca.bc.gov.educ.studentdatacollection.api.rest;

import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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