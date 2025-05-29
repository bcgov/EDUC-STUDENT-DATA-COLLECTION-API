package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity;
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

import java.math.BigDecimal;
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
        assertTrue(decodedData.contains("PEN"));
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

    @Test
    void testGenerateFrenchFromSdcSchoolCollectionID() {
        UUID schoolCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockFrenchStudent());

        when(mockSearchService.findAllFrenchStudentsLightBySchoolCollectionID(schoolCollectionId)).thenReturn(mockEntities);

        DownloadableReportResponse response = service.generateFrenchFromSdcSchoolCollectionID(schoolCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllFrenchStudentsLightBySchoolCollectionID(schoolCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("French Program"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
    }

    @Test
    void testGenerateCareerFromSdcSchoolCollectionID() {
        UUID schoolCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockCareerStudent());

        when(mockSearchService.findAllCareerStudentsLightBySchoolCollectionID(schoolCollectionId)).thenReturn(mockEntities);

        DownloadableReportResponse response = service.generateCareerFromSdcSchoolCollectionID(schoolCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllCareerStudentsLightBySchoolCollectionID(schoolCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("Career Program"));
        assertTrue(decodedData.contains("Career Code"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
    }

    @Test
    void testGenerateIndigenousFromSdcSchoolCollectionID() {
        UUID schoolCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockIndigenousStudent());

        when(mockSearchService.findAllIndigenousStudentsLightBySchoolCollectionID(schoolCollectionId)).thenReturn(mockEntities);

        DownloadableReportResponse response = service.generateIndigenousFromSdcSchoolCollectionID(schoolCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllIndigenousStudentsLightBySchoolCollectionID(schoolCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("Indigenous Ancestry"));
        assertTrue(decodedData.contains("Band Code"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
    }

    @Test
    void testGenerateInclusiveFromSdcSchoolCollectionID() {
        UUID schoolCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockInclusiveStudent());

        when(mockSearchService.findAllInclusiveEdStudentsLightBySchoolCollectionId(schoolCollectionId)).thenReturn(mockEntities);

        DownloadableReportResponse response = service.generateInclusiveFromSdcSchoolCollectionID(schoolCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllInclusiveEdStudentsLightBySchoolCollectionId(schoolCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("Inclusive Education Category"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
    }

    @Test
    void testGenerateEllFromSdcSchoolCollectionID() {
        UUID schoolCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockEllStudent());

        when(mockSearchService.findAllEllStudentsLightBySchoolCollectionId(schoolCollectionId)).thenReturn(mockEntities);

        DownloadableReportResponse response = service.generateEllFromSdcSchoolCollectionID(schoolCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllEllStudentsLightBySchoolCollectionId(schoolCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("Language Program"));
        assertTrue(decodedData.contains("Years in ELL"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
    }

    @Test
    void testGenerateRefugeeFromSdcSchoolCollectionID() {
        UUID schoolCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockRefugeeStudent());

        when(mockSearchService.findAllRefugeeStudentsLightBySchoolCollectionId(schoolCollectionId)).thenReturn(mockEntities);

        DownloadableReportResponse response = service.generateRefugeeFromSdcSchoolCollectionID(schoolCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllRefugeeStudentsLightBySchoolCollectionId(schoolCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("Funding Code"));
        assertTrue(decodedData.contains("Funding Eligible"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
    }

    @Test
    void testGenerateFrenchFromSdcDistrictCollectionID() {
        UUID districtCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> mockEntities = new ArrayList<>();
        SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student = createMockFrenchStudent();
        student.setSdcSchoolCollectionEntity(createMockSdcSchoolCollectionEntity());
        mockEntities.add(student);

        when(mockSearchService.findAllFrenchStudentsLightByDistrictCollectionId(districtCollectionId)).thenReturn(mockEntities);
        FacilityTypeCode code = new FacilityTypeCode();
        code.setFacilityTypeCode("01");
        code.setLabel("ABC");
        when(mockRestUtils.getFacilityTypeCode(any())).thenReturn(Optional.of(code));
        when(mockRestUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(new SchoolTombstone()));

        DownloadableReportResponse response = service.generateFrenchFromSdcDistrictCollectionID(districtCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllFrenchStudentsLightByDistrictCollectionId(districtCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("French Program"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
        assertTrue(decodedData.contains("School Code"));
        assertTrue(decodedData.contains("School Name"));
        assertTrue(decodedData.contains("Facility Type"));
    }

    @Test
    void testGenerateCareerFromSdcDistrictCollectionID() {
        UUID districtCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> mockEntities = new ArrayList<>();
        SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student = createMockCareerStudent();
        student.setSdcSchoolCollectionEntity(createMockSdcSchoolCollectionEntity());
        mockEntities.add(student);

        when(mockSearchService.findAllCareerStudentsLightByDistrictCollectionId(districtCollectionId)).thenReturn(mockEntities);
        FacilityTypeCode code = new FacilityTypeCode();
        code.setFacilityTypeCode("01");
        code.setLabel("ABC");
        when(mockRestUtils.getFacilityTypeCode(any())).thenReturn(Optional.of(code));
        when(mockRestUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(new SchoolTombstone()));

        DownloadableReportResponse response = service.generateCareerFromSdcDistrictCollectionID(districtCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllCareerStudentsLightByDistrictCollectionId(districtCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("Career Program"));
        assertTrue(decodedData.contains("Career Code"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
        assertTrue(decodedData.contains("School Code"));
        assertTrue(decodedData.contains("School Name"));
        assertTrue(decodedData.contains("Facility Type"));
    }

    @Test
    void testGenerateIndigenousFromSdcDistrictCollectionID() {
        UUID districtCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> mockEntities = new ArrayList<>();
        SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student = createMockIndigenousStudent();
        student.setSdcSchoolCollectionEntity(createMockSdcSchoolCollectionEntity());
        mockEntities.add(student);

        when(mockSearchService.findAllIndigenousStudentsLightByDistrictCollectionId(districtCollectionId)).thenReturn(mockEntities);
        FacilityTypeCode code = new FacilityTypeCode();
        code.setFacilityTypeCode("01");
        code.setLabel("ABC");
        when(mockRestUtils.getFacilityTypeCode(any())).thenReturn(Optional.of(code));
        when(mockRestUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(new SchoolTombstone()));

        DownloadableReportResponse response = service.generateIndigenousFromSdcDistrictCollectionID(districtCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllIndigenousStudentsLightByDistrictCollectionId(districtCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("Indigenous Ancestry"));
        assertTrue(decodedData.contains("Band Code"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
        assertTrue(decodedData.contains("School Code"));
        assertTrue(decodedData.contains("School Name"));
        assertTrue(decodedData.contains("Facility Type"));
    }

    @Test
    void testGenerateInclusiveFromSdcDistrictCollectionID() {
        UUID districtCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightEntity> mockEntities = new ArrayList<>();
        SdcSchoolCollectionStudentLightEntity student = createMockInclusiveStudent();
        student.setSdcSchoolCollectionEntity(createMockSdcSchoolCollectionEntity());
        mockEntities.add(student);

        when(mockSearchService.findAllInclusiveEdStudentsLightByDistrictCollectionId(districtCollectionId)).thenReturn(mockEntities);
        FacilityTypeCode code = new FacilityTypeCode();
        code.setFacilityTypeCode("01");
        code.setLabel("ABC");
        when(mockRestUtils.getFacilityTypeCode(any())).thenReturn(Optional.of(code));
        when(mockRestUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(new SchoolTombstone()));

        DownloadableReportResponse response = service.generateInclusiveFromSdcDistrictCollectionID(districtCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllInclusiveEdStudentsLightByDistrictCollectionId(districtCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("Inclusive Education Category"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
        assertTrue(decodedData.contains("School Code"));
        assertTrue(decodedData.contains("School Name"));
        assertTrue(decodedData.contains("Facility Type"));
    }

    @Test
    void testGenerateEllFromSdcDistrictCollectionID() {
        UUID districtCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> mockEntities = new ArrayList<>();
        SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student = createMockEllStudent();
        student.setSdcSchoolCollectionEntity(createMockSdcSchoolCollectionEntity());
        mockEntities.add(student);

        when(mockSearchService.findAllEllStudentsLightByDistrictCollectionId(districtCollectionId)).thenReturn(mockEntities);
        FacilityTypeCode code = new FacilityTypeCode();
        code.setFacilityTypeCode("01");
        code.setLabel("ABC");
        when(mockRestUtils.getFacilityTypeCode(any())).thenReturn(Optional.of(code));
        when(mockRestUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(new SchoolTombstone()));

        DownloadableReportResponse response = service.generateEllFromSdcDistrictCollectionID(districtCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllEllStudentsLightByDistrictCollectionId(districtCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("Language Program"));
        assertTrue(decodedData.contains("Years in ELL"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
        assertTrue(decodedData.contains("School Code"));
        assertTrue(decodedData.contains("School Name"));
        assertTrue(decodedData.contains("Facility Type"));
    }

    @Test
    void testGenerateRefugeeFromSdcDistrictCollectionID() {
        UUID districtCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity> mockEntities = new ArrayList<>();
        SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity student = createMockRefugeeStudent();
        student.setSdcSchoolCollectionEntity(createMockSdcSchoolCollectionEntity());
        mockEntities.add(student);

        when(mockSearchService.findAllRefugeeStudentsLightByDistrictCollectionId(districtCollectionId)).thenReturn(mockEntities);
        FacilityTypeCode code = new FacilityTypeCode();
        code.setFacilityTypeCode("01");
        code.setLabel("ABC");
        when(mockRestUtils.getFacilityTypeCode(any())).thenReturn(Optional.of(code));
        when(mockRestUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(new SchoolTombstone()));

        DownloadableReportResponse response = service.generateRefugeeFromSdcDistrictCollectionID(districtCollectionId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllRefugeeStudentsLightByDistrictCollectionId(districtCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("Funding Code"));
        assertTrue(decodedData.contains("Funding Eligible"));
        assertTrue(decodedData.contains("Legal Name"));
        assertTrue(decodedData.contains("PEN"));
        assertTrue(decodedData.contains("School Code"));
        assertTrue(decodedData.contains("School Name"));
        assertTrue(decodedData.contains("Facility Type"));
    }

    private SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity createMockFrenchStudent() {
        SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student = new SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity();
        student.setStudentPen("123456789");
        student.setLegalFirstName("Jean");
        student.setLegalLastName("Dupont");
        student.setUsualFirstName("Jean");
        student.setUsualLastName("Dupont");
        student.setFte(BigDecimal.ONE);
        student.setFrenchProgramNonEligReasonCode(null);
        student.setLocalID("LID123");
        student.setIsAdult(false);
        student.setIsGraduated(false);
        student.setEnrolledGradeCode("10");
        student.setSchoolFundingCode("14");
        student.setEnrolledProgramCodes("05");
        return student;
    }

    private SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity createMockCareerStudent() {
        SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student = new SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity();
        student.setStudentPen("987654321");
        student.setLegalFirstName("Alex");
        student.setLegalLastName("Smith");
        student.setUsualFirstName("Alex");
        student.setUsualLastName("Smith");
        student.setFte(BigDecimal.ONE);
        student.setFrenchProgramNonEligReasonCode(null);
        student.setLocalID("LID456");
        student.setIsAdult(true);
        student.setIsGraduated(true);
        student.setEnrolledGradeCode("12");
        student.setSchoolFundingCode("16");
        student.setEnrolledProgramCodes("CP");
        student.setCareerProgramCode("CP");
        return student;
    }

    private SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity createMockIndigenousStudent() {
        SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student = new SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity();
        student.setStudentPen("111222333");
        student.setLegalFirstName("Mary");
        student.setLegalLastName("Johnson");
        student.setUsualFirstName("Mary");
        student.setUsualLastName("Johnson");
        student.setFte(BigDecimal.ONE);
        student.setFrenchProgramNonEligReasonCode(null);
        student.setLocalID("LID789");
        student.setIsAdult(false);
        student.setIsGraduated(false);
        student.setEnrolledGradeCode("11");
        student.setSchoolFundingCode("20");
        student.setEnrolledProgramCodes("AB");
        student.setNativeAncestryInd("Y");
        student.setBandCode("123");
        return student;
    }

    private SdcSchoolCollectionStudentLightEntity createMockInclusiveStudent() {
        SdcSchoolCollectionStudentLightEntity student = new SdcSchoolCollectionStudentLightEntity();
        student.setStudentPen("444555666");
        student.setLegalFirstName("Sam");
        student.setLegalLastName("Lee");
        student.setUsualFirstName("Sam");
        student.setUsualLastName("Lee");
        student.setFte(BigDecimal.ONE);
        student.setSpecialEducationNonEligReasonCode(null);
        student.setLocalID("LID321");
        student.setIsAdult(false);
        student.setIsGraduated(false);
        student.setEnrolledGradeCode("09");
        student.setSchoolFundingCode("14");
        student.setSpecialEducationCategoryCode("A");
        return student;
    }

    private SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity createMockEllStudent() {
        SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student = new SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity();
        student.setStudentPen("777888999");
        student.setLegalFirstName("Linh");
        student.setLegalLastName("Tran");
        student.setUsualFirstName("Linh");
        student.setUsualLastName("Tran");
        student.setFte(BigDecimal.ONE);
        student.setFrenchProgramNonEligReasonCode(null);
        student.setLocalID("LID654");
        student.setIsAdult(false);
        student.setIsGraduated(false);
        student.setEnrolledGradeCode("08");
        student.setSchoolFundingCode("14");
        student.setEnrolledProgramCodes("17");
        student.setYearsInEll(2);
        return student;
    }

    private SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity createMockRefugeeStudent() {
        SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity student = new SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity();
        student.setStudentPen("999888777");
        student.setLegalFirstName("Anna");
        student.setLegalLastName("Nguyen");
        student.setUsualFirstName("Anna");
        student.setUsualLastName("Nguyen");
        student.setFte(BigDecimal.valueOf(0.5));
        student.setFrenchProgramNonEligReasonCode("FR01");
        student.setLocalID("LID987");
        student.setIsAdult(true);
        student.setIsGraduated(false);
        student.setEnrolledGradeCode("07");
        student.setSchoolFundingCode("12");
        student.setEnrolledProgramCodes("19");
        student.setYearsInEll(3);
        return student;
    }

    private SdcSchoolCollectionStudentLightEntity createMockStudent() {
        SdcSchoolCollectionStudentLightEntity student = new SdcSchoolCollectionStudentLightEntity();
        SdcSchoolCollectionEntity mockCollection = new SdcSchoolCollectionEntity();
        mockCollection.setSchoolID(UUID.randomUUID());
        student.setSdcSchoolCollectionEntity(mockCollection);
        return student;
    }

    private SdcSchoolCollectionEntity createMockSdcSchoolCollectionEntity() {
        SdcSchoolCollectionEntity entity = new SdcSchoolCollectionEntity();
        entity.setSchoolID(UUID.randomUUID());
        return entity;
    }
}
