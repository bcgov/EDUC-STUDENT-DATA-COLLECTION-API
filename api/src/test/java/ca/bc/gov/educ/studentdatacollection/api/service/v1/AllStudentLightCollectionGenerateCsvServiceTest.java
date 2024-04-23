package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.reports.AllStudentLightCollectionGenerateCsvService;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

class AllStudentLightCollectionGenerateCsvServiceTest {

    @Mock
    private SdcSchoolCollectionStudentSearchService mockSearchService;

    @Mock
    private RestUtils mockRestUtils;

    @InjectMocks
    private AllStudentLightCollectionGenerateCsvService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateFromSdcSchoolCollectionID() {
        // Setup
        UUID schoolCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockStudent());

        when(mockSearchService.findAllStudentsLightBySchoolCollectionID(schoolCollectionId)).thenReturn(mockEntities);

        // Execution
        DownloadableReportResponse response = service.generateFromSdcSchoolCollectionID(schoolCollectionId);

        // Verification
        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllStudentsLightBySchoolCollectionID(schoolCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("P.E.N."));
    }

    @Test
    void testGenerateFromSdcDistrictCollectionID() {
        // Setup
        UUID districtCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockStudent());  // A method to create mock student data

        when(mockSearchService.findAllStudentsLightByDistrictCollectionId(districtCollectionId)).thenReturn(mockEntities);

        // Execution
        DownloadableReportResponse response = service.generateFromSdcDistrictCollectionID(districtCollectionId);

        // Verification
        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllStudentsLightByDistrictCollectionId(districtCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("School Code"));
    }

    @Test
    void testFormatFullName_AllNamesPresent() {
        String result = service.formatFullName("John", "Quincy", "Adams");
        assertEquals("Adams, John Quincy", result);
    }

    @Test
    void testFormatFullName_MissingMiddleName() {
        String result = service.formatFullName("John", "", "Adams");
        assertEquals("Adams, John", result);
    }

    @Test
    void testFormatFullName_MissingFirstAndMiddleNames() {
        String result = service.formatFullName("", "", "Adams");
        assertEquals("Adams", result);
    }

    @Test
    void testFormatFullName_MissingLastName() {
        String result = service.formatFullName("John", "Quincy", "");
        assertEquals("John Quincy", result);
    }

    @Test
    void testFormatFullName_AllNamesEmpty() {
        String result = service.formatFullName("", "", "");
        assertEquals("", result);
    }

    @Test
    void testParseEnrolledProgramCodes_Standard() {
        Set<String> result = service.parseEnrolledProgramCodes("ABCD");
        assertTrue(result.contains("AB") && result.contains("CD"));
        assertEquals(2, result.size());
    }

    @Test
    void testParseEnrolledProgramCodes_EmptyString() {
        Set<String> result = service.parseEnrolledProgramCodes("");
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseEnrolledProgramCodes_OddCharacters() {
        Set<String> result = service.parseEnrolledProgramCodes("ABC");
        assertTrue(result.contains("AB"));
        assertTrue(result.contains("C"));
        assertEquals(2, result.size());
    }

    @Test
    void testParseEnrolledProgramCodes_NullInput() {
        Set<String> result = service.parseEnrolledProgramCodes(null);
        assertTrue(result.isEmpty());
    }

    private SdcSchoolCollectionStudentLightEntity createMockStudent() {
        SdcSchoolCollectionStudentLightEntity student = new SdcSchoolCollectionStudentLightEntity();
        SdcSchoolCollectionEntity mockCollection = new SdcSchoolCollectionEntity();
        mockCollection.setSchoolID(UUID.randomUUID());
        student.setSdcSchoolCollectionEntity(mockCollection);
        return student;
    }
}
