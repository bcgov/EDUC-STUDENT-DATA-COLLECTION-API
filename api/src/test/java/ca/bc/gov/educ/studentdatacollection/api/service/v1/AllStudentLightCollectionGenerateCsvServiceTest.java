package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.reports.AllStudentLightCollectionGenerateCsvService;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.FacilityTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("Usual Name"));
        assertTrue(decodedData.contains("Birth Date"));
    }

    @Test
    void testGenerateFromSdcDistrictCollectionID() {
        // Setup
        UUID districtCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockStudent());  // A method to create mock student data

        when(mockSearchService.findAllStudentsLightByDistrictCollectionId(districtCollectionId)).thenReturn(mockEntities);
        FacilityTypeCode code = new FacilityTypeCode();
        code.setFacilityTypeCode("01");
        code.setLabel("ABC");
        when(mockRestUtils.getFacilityTypeCode(any())).thenReturn(Optional.of(code));
        when(mockRestUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(new SchoolTombstone()));
        // Execution
        DownloadableReportResponse response = service.generateFromSdcDistrictCollectionID(districtCollectionId);

        // Verification
        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllStudentsLightByDistrictCollectionId(districtCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("School Code"));
        assertTrue(decodedData.contains("School Name"));
        assertTrue(decodedData.contains("Facility Type"));
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
        Map<String, String> result = service.parseEnrolledProgramCodes("05EF", "1");
        assertEquals("1", result.get("05"));
        assertEquals("", result.get("08"));
        assertNull(result.get("EF"));
    }

    @Test
    void testParseEnrolledProgramCodes_EmptyString() {
        Map<String, String> result = service.parseEnrolledProgramCodes("", "1");
        result.values().forEach(value -> assertEquals("", value));
    }

    @Test
    void testParseEnrolledProgramCodes_NonStandardCodes() {
        Map<String, String> result = service.parseEnrolledProgramCodes("0508E", "1");
        assertEquals("1", result.get("05"));
        assertEquals("1", result.get("08"));
        assertNull(result.get("E "));
    }

    @Test
    void testParseEnrolledProgramCodes_NullInput() {
        Map<String, String> result = service.parseEnrolledProgramCodes(null, "1");
        result.values().forEach(value -> assertEquals("", value));
    }

    @Test
    void testFormatFullName_SpecialCharacters() {
        String result = service.formatFullName("Jo@n", "D'oe", "O'Reilly");
        assertEquals("O'Reilly, Jo@n D'oe", result.trim());
    }

    @Test
    void testConvertToBinary_WithN() {
        String input = "N";
        assertEquals("", service.convertToBinary(input), "Should return null for 'N'");
    }

    @Test
    void testConvertToBinary_WithY() {
        String input = "Y";
        assertEquals("1", service.convertToBinary(input), "Should return '1' for 'Y'");
    }

    @Test
    void testConvertToBinary_WithOtherString() {
        String input = "Hello";
        assertEquals(input, service.convertToBinary(input), "Should return the input string when not 'Y' or 'N'");
    }

    @Test
    void testConvertToBinary_EmptyString() {
        String input = "";
        assertEquals(input, service.convertToBinary(input), "Should return the empty string as it is");
    }

    @Test
    void testConvertToBinary_WithNull() {
        assertEquals("", service.convertToBinary(null), "Should return null when input is null");
    }

    @Test
    void testGenerateFromSdcDistrictCollectionID_withRestUtilsException() {
        UUID districtCollectionId = UUID.randomUUID();
        when(mockSearchService.findAllStudentsLightByDistrictCollectionId(districtCollectionId)).thenReturn(Collections.singletonList(createMockStudent()));
        when(mockRestUtils.getSchoolBySchoolID(any())).thenThrow(new RuntimeException("Test exception"));

        assertThrows(RuntimeException.class, () -> service.generateFromSdcDistrictCollectionID(districtCollectionId));
    }

    private SdcSchoolCollectionStudentLightEntity createMockStudent() {
        SdcSchoolCollectionStudentLightEntity student = new SdcSchoolCollectionStudentLightEntity();
        SdcSchoolCollectionEntity mockCollection = new SdcSchoolCollectionEntity();
        mockCollection.setSchoolID(UUID.randomUUID());
        student.setSdcSchoolCollectionEntity(mockCollection);
        return student;
    }
}
