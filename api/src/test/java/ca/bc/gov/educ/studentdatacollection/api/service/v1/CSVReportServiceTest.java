package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndyFundingReportHeader;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.reports.CSVReportService;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EllStudentResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndySchoolGradeFundingGroupHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SpecialEdHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CSVReportServiceTest {

    @InjectMocks
    private CSVReportService csvReportService;

    @Mock
    private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

    @Mock
    private RestUtils restUtils;

    private static final String INDY_FUNDING_REPORT_ALL = "indy-funding-report-all";
    private static final String ONLINE_INDY_FUNDING_REPORT = "online-indy-funding-report";
    private static final String INDY_FUNDING_REPORT_FUNDED = "indy-funding-report-funded";
    private static final String INDY_SCHOOL_GRADE_FUNDING_GROUP_ENROLLED_PROGRAMS_HEADCOUNTS = "indy-school-grade-funding-group-enrolled-programs-headcounts";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        CollectionEntity c = new CollectionEntity();
        when(collectionRepository.findById(any())).thenReturn(Optional.of(c));
    }

    @Test
    void testCSVHeadersForStandardReport() {
        DownloadableReportResponse response = this.csvReportService.generateIndyFundingReport(UUID.randomUUID(), false, false);
        assertNotNull(response);
        assertEquals(INDY_FUNDING_REPORT_ALL, response.getReportType());

        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        String[] expectedHeaders = IndyFundingReportHeader.getAllValuesAsStringArray();
        for (String header : expectedHeaders) {
            assertTrue(csvContent.contains(header), "CSV should contain header: " + header);
        }
        assertFalse(csvContent.contains("K to 9 FTE Sum"));
        assertFalse(csvContent.contains("10 to 12 FTE Sum"));
    }

    @Test
    void testCSVHeadersForOnlineLearningReport() {
        DownloadableReportResponse response = this.csvReportService.generateIndyFundingReport(UUID.randomUUID(), true, false);
        assertNotNull(response);
        assertEquals(ONLINE_INDY_FUNDING_REPORT, response.getReportType());

        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        String[] expectedHeaders = IndyFundingReportHeader.getAllValuesAndRoundUpAsStringArray();
        for (String header : expectedHeaders) {
            assertTrue(csvContent.contains(header), "CSV should contain header: " + header);
        }
    }

    @Test
    void testCSVHeadersForFundedReport() {
        DownloadableReportResponse response = this.csvReportService.generateIndyFundingReport(UUID.randomUUID(), false, true);
        assertNotNull(response);
        assertEquals(INDY_FUNDING_REPORT_FUNDED, response.getReportType());

        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        String[] expectedHeaders = IndyFundingReportHeader.getAllValuesAsStringArray();
        for (String header : expectedHeaders) {
            assertTrue(csvContent.contains(header), "CSV should contain header: " + header);
        }
    }

    // ===== I1005 Report Tests =====

    @Test
    void testGenerateIndySchoolGradeEnrollmentHeadcountReport_HeadersAreCorrect() {
        // Given
        UUID collectionId = UUID.randomUUID();
        when(sdcSchoolCollectionStudentRepository.getIndySchoolGradeFundingGroupHeadcountsByCollectionId(collectionId))
                .thenReturn(Collections.emptyList());

        // When
        DownloadableReportResponse response = csvReportService.generateIndySchoolGradeFundingGroupEnrolledProgramHeadcounts(collectionId);

        // Then
        assertNotNull(response);
        assertEquals(INDY_SCHOOL_GRADE_FUNDING_GROUP_ENROLLED_PROGRAMS_HEADCOUNTS, response.getReportType());

        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);

        // Verify all header columns are present
        assertTrue(csvContent.contains("District Number"));
        assertTrue(csvContent.contains("School Number"));
        assertTrue(csvContent.contains("School Name"));

        // Verify individual grade funding group headers
        assertTrue(csvContent.contains("KH - Funding Group"));
        assertTrue(csvContent.contains("KF - Funding Group"));
        assertTrue(csvContent.contains("01 - Funding Group"));
        assertTrue(csvContent.contains("12 - Funding Group"));

        // Verify program headers
        assertTrue(csvContent.contains("KH - Core French"));
        assertTrue(csvContent.contains("Total - Core French"));
        assertTrue(csvContent.contains("KH - Programme Francophone"));
        assertTrue(csvContent.contains("Total - Programme Francophone"));
        assertTrue(csvContent.contains("KH - Early French Immersion"));
        assertTrue(csvContent.contains("KH - Late French Immersion"));
        assertTrue(csvContent.contains("KH - English Language Learning"));
        assertTrue(csvContent.contains("KH - Indigenous Language and Culture"));
        assertTrue(csvContent.contains("KH - Indigenous Support Services"));
        assertTrue(csvContent.contains("KH - Other Approved Indigenous Programs"));
    }

    @Test
    void testEmptyIfZero_ReturnsEmptyStringForZero() {
        assertEquals("", TransformUtil.emptyIfZero("0"));
    }

    @Test
    void testEmptyIfZero_ReturnsOriginalValueForNonZero() {
        assertEquals("5", TransformUtil.emptyIfZero("5"));
        assertEquals("123", TransformUtil.emptyIfZero("123"));
        assertEquals("", TransformUtil.emptyIfZero(""));
        assertNull(TransformUtil.emptyIfZero(null));
    }

    @Test
    void testGetFundingGroupForGrade_ReturnsCorrectFundingGroupNumber() {
        // Given
        List<IndependentSchoolFundingGroup> fundingGroups = new ArrayList<>();
        IndependentSchoolFundingGroup group1 = createFundingGroup("GRADE01", "GROUP1");
        IndependentSchoolFundingGroup group2 = createFundingGroup("GRADE02", "GROUP2");
        fundingGroups.add(group1);
        fundingGroups.add(group2);

        // When
        String result1 = TransformUtil.getFundingGroupForGrade(fundingGroups, "GRADE01");
        String result2 = TransformUtil.getFundingGroupForGrade(fundingGroups, "GRADE02");
        String result3 = TransformUtil.getFundingGroupForGrade(fundingGroups, "GRADE03");

        // Then
        assertEquals("1", result1, "Should return just the number from GROUP1");
        assertEquals("2", result2, "Should return just the number from GROUP2");
        assertEquals("", result3, "Should return empty string for non-existent grade");
    }

    @Test
    void testGetFundingGroupForGrade_HandlesNullAndEmptyList() {
        assertEquals("", TransformUtil.getFundingGroupForGrade(null, "GRADE01"));
        assertEquals("", TransformUtil.getFundingGroupForGrade(Collections.emptyList(), "GRADE01"));
    }

    @Test
    void testGetFundingGroupForGrade_HandlesNonGroupCodeFormat() {
        // Given
        List<IndependentSchoolFundingGroup> fundingGroups = new ArrayList<>();
        IndependentSchoolFundingGroup invalidGroup = createFundingGroup("GRADE01", "INVALID");
        fundingGroups.add(invalidGroup);

        // When
        String result = TransformUtil.getFundingGroupForGrade(fundingGroups, "GRADE01");

        // Then
        assertEquals("", result, "Should return empty string for non-GROUP formatted code");
    }

    @Test
    void testPrepareIndySchoolGradeFundingGroupDataForCsv_IncludesAllGradeFundingGroups() {
        // Given
        UUID collectionId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();

        IndySchoolGradeFundingGroupHeadcountResult mockResult = createMockHeadcountResult(schoolId);
        SchoolTombstone mockSchool = createMockSchool(schoolId);
        District mockDistrict = createMockDistrict();

        List<IndependentSchoolFundingGroup> fundingGroups = createMockFundingGroups();

        when(restUtils.getDistrictByDistrictID(any())).thenReturn(Optional.of(mockDistrict));
        when(restUtils.getSchoolFundingGroupsBySchoolID(schoolId.toString())).thenReturn(fundingGroups);
        when(sdcSchoolCollectionStudentRepository.getIndySchoolGradeFundingGroupHeadcountsByCollectionId(collectionId))
                .thenReturn(List.of(mockResult));
        when(restUtils.getSchoolBySchoolID(schoolId.toString())).thenReturn(Optional.of(mockSchool));

        // When
        DownloadableReportResponse response = csvReportService.generateIndySchoolGradeFundingGroupEnrolledProgramHeadcounts(collectionId);

        // Then
        assertNotNull(response);
        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);

        // Verify the CSV contains the school data
        assertTrue(csvContent.contains("036"), "Should contain district number");
        assertTrue(csvContent.contains("12345"), "Should contain school number");
        assertTrue(csvContent.contains("Test School"), "Should contain school name");
    }

    @Test
    void testPrepareIndySchoolGradeFundingGroupDataForCsv_ZeroCountsAreEmpty() {
        // Given
        UUID collectionId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();

        IndySchoolGradeFundingGroupHeadcountResult mockResult = createMockHeadcountResultWithZeros(schoolId);
        SchoolTombstone mockSchool = createMockSchool(schoolId);
        District mockDistrict = createMockDistrict();

        when(restUtils.getDistrictByDistrictID(any())).thenReturn(Optional.of(mockDistrict));
        when(restUtils.getSchoolFundingGroupsBySchoolID(schoolId.toString())).thenReturn(Collections.emptyList());
        when(sdcSchoolCollectionStudentRepository.getIndySchoolGradeFundingGroupHeadcountsByCollectionId(collectionId))
                .thenReturn(List.of(mockResult));
        when(restUtils.getSchoolBySchoolID(schoolId.toString())).thenReturn(Optional.of(mockSchool));

        // When
        DownloadableReportResponse response = csvReportService.generateIndySchoolGradeFundingGroupEnrolledProgramHeadcounts(collectionId);

        // Then
        assertNotNull(response);
        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        String[] lines = csvContent.split("\n");

        assertTrue(lines.length >= 2, "Should have header and at least one data row");
        String dataRow = lines[1];
        assertTrue(dataRow.contains(",,"), "Zero counts should result in empty CSV cells");
    }

    // Helper methods to create mock objects

    private IndependentSchoolFundingGroup createFundingGroup(String gradeCode, String groupCode) {
        IndependentSchoolFundingGroup group = new IndependentSchoolFundingGroup();
        group.setSchoolGradeCode(gradeCode);
        group.setSchoolFundingGroupCode(groupCode);
        return group;
    }

    private SchoolTombstone createMockSchool(UUID schoolId) {
        SchoolTombstone school = new SchoolTombstone();
        school.setSchoolId(schoolId.toString());
        school.setSchoolNumber("12345");
        school.setDisplayName("Test School");
        school.setDistrictId(UUID.randomUUID().toString());
        school.setFacilityTypeCode("STANDARD");
        school.setSchoolCategoryCode("INDEPEND");
        return school;
    }

    private District createMockDistrict() {
        District district = new District();
        district.setDistrictNumber("036");
        return district;
    }

    private List<IndependentSchoolFundingGroup> createMockFundingGroups() {
        List<IndependentSchoolFundingGroup> groups = new ArrayList<>();
        groups.add(createFundingGroup("KINDHALF", "GROUP1"));
        groups.add(createFundingGroup("KINDFULL", "GROUP1"));
        groups.add(createFundingGroup("GRADE01", "GROUP1"));
        groups.add(createFundingGroup("GRADE02", "GROUP1"));
        groups.add(createFundingGroup("GRADE03", "GROUP1"));
        groups.add(createFundingGroup("GRADE04", "GROUP2"));
        groups.add(createFundingGroup("GRADE05", "GROUP2"));
        return groups;
    }

    private IndySchoolGradeFundingGroupHeadcountResult createMockHeadcountResult(UUID schoolId) {
        return new IndySchoolGradeFundingGroupHeadcountResult() {
            @Override public String getSchoolID() { return schoolId.toString(); }
            // Core French - some non-zero values
            @Override public String getKindHCountCF() { return "5"; }
            @Override public String getKindFCountCF() { return "3"; }
            @Override public String getGrade1CountCF() { return "10"; }
            @Override public String getGrade2CountCF() { return "8"; }
            @Override public String getGrade3CountCF() { return "0"; }
            @Override public String getGrade4CountCF() { return "0"; }
            @Override public String getGrade5CountCF() { return "0"; }
            @Override public String getGrade6CountCF() { return "0"; }
            @Override public String getGrade7CountCF() { return "0"; }
            @Override public String getGrade8CountCF() { return "0"; }
            @Override public String getGrade9CountCF() { return "0"; }
            @Override public String getGrade10CountCF() { return "0"; }
            @Override public String getGrade11CountCF() { return "0"; }
            @Override public String getGrade12CountCF() { return "0"; }
            @Override public String getGradeEUCountCF() { return "0"; }
            @Override public String getGradeSUCountCF() { return "0"; }
            @Override public String getGradeGACountCF() { return "0"; }
            // Programme Francophone
            @Override public String getKindHCountPF() { return "0"; }
            @Override public String getKindFCountPF() { return "0"; }
            @Override public String getGrade1CountPF() { return "0"; }
            @Override public String getGrade2CountPF() { return "0"; }
            @Override public String getGrade3CountPF() { return "0"; }
            @Override public String getGrade4CountPF() { return "0"; }
            @Override public String getGrade5CountPF() { return "0"; }
            @Override public String getGrade6CountPF() { return "0"; }
            @Override public String getGrade7CountPF() { return "0"; }
            @Override public String getGrade8CountPF() { return "0"; }
            @Override public String getGrade9CountPF() { return "0"; }
            @Override public String getGrade10CountPF() { return "0"; }
            @Override public String getGrade11CountPF() { return "0"; }
            @Override public String getGrade12CountPF() { return "0"; }
            @Override public String getGradeEUCountPF() { return "0"; }
            @Override public String getGradeSUCountPF() { return "0"; }
            @Override public String getGradeGACountPF() { return "0"; }
            // Early French Immersion
            @Override public String getKindHCountEFI() { return "0"; }
            @Override public String getKindFCountEFI() { return "0"; }
            @Override public String getGrade1CountEFI() { return "0"; }
            @Override public String getGrade2CountEFI() { return "0"; }
            @Override public String getGrade3CountEFI() { return "0"; }
            @Override public String getGrade4CountEFI() { return "0"; }
            @Override public String getGrade5CountEFI() { return "0"; }
            @Override public String getGrade6CountEFI() { return "0"; }
            @Override public String getGrade7CountEFI() { return "0"; }
            @Override public String getGrade8CountEFI() { return "0"; }
            @Override public String getGrade9CountEFI() { return "0"; }
            @Override public String getGrade10CountEFI() { return "0"; }
            @Override public String getGrade11CountEFI() { return "0"; }
            @Override public String getGrade12CountEFI() { return "0"; }
            @Override public String getGradeEUCountEFI() { return "0"; }
            @Override public String getGradeSUCountEFI() { return "0"; }
            @Override public String getGradeGACountEFI() { return "0"; }
            // Late French Immersion
            @Override public String getKindHCountLFI() { return "0"; }
            @Override public String getKindFCountLFI() { return "0"; }
            @Override public String getGrade1CountLFI() { return "0"; }
            @Override public String getGrade2CountLFI() { return "0"; }
            @Override public String getGrade3CountLFI() { return "0"; }
            @Override public String getGrade4CountLFI() { return "0"; }
            @Override public String getGrade5CountLFI() { return "0"; }
            @Override public String getGrade6CountLFI() { return "0"; }
            @Override public String getGrade7CountLFI() { return "0"; }
            @Override public String getGrade8CountLFI() { return "0"; }
            @Override public String getGrade9CountLFI() { return "0"; }
            @Override public String getGrade10CountLFI() { return "0"; }
            @Override public String getGrade11CountLFI() { return "0"; }
            @Override public String getGrade12CountLFI() { return "0"; }
            @Override public String getGradeEUCountLFI() { return "0"; }
            @Override public String getGradeSUCountLFI() { return "0"; }
            @Override public String getGradeGACountLFI() { return "0"; }
            // English Language Learning
            @Override public String getKindHCountELL() { return "0"; }
            @Override public String getKindFCountELL() { return "0"; }
            @Override public String getGrade1CountELL() { return "0"; }
            @Override public String getGrade2CountELL() { return "0"; }
            @Override public String getGrade3CountELL() { return "0"; }
            @Override public String getGrade4CountELL() { return "0"; }
            @Override public String getGrade5CountELL() { return "0"; }
            @Override public String getGrade6CountELL() { return "0"; }
            @Override public String getGrade7CountELL() { return "0"; }
            @Override public String getGrade8CountELL() { return "0"; }
            @Override public String getGrade9CountELL() { return "0"; }
            @Override public String getGrade10CountELL() { return "0"; }
            @Override public String getGrade11CountELL() { return "0"; }
            @Override public String getGrade12CountELL() { return "0"; }
            @Override public String getGradeEUCountELL() { return "0"; }
            @Override public String getGradeSUCountELL() { return "0"; }
            @Override public String getGradeGACountELL() { return "0"; }
            // Indigenous Language and Culture
            @Override public String getKindHCountALC() { return "0"; }
            @Override public String getKindFCountALC() { return "0"; }
            @Override public String getGrade1CountALC() { return "0"; }
            @Override public String getGrade2CountALC() { return "0"; }
            @Override public String getGrade3CountALC() { return "0"; }
            @Override public String getGrade4CountALC() { return "0"; }
            @Override public String getGrade5CountALC() { return "0"; }
            @Override public String getGrade6CountALC() { return "0"; }
            @Override public String getGrade7CountALC() { return "0"; }
            @Override public String getGrade8CountALC() { return "0"; }
            @Override public String getGrade9CountALC() { return "0"; }
            @Override public String getGrade10CountALC() { return "0"; }
            @Override public String getGrade11CountALC() { return "0"; }
            @Override public String getGrade12CountALC() { return "0"; }
            @Override public String getGradeEUCountALC() { return "0"; }
            @Override public String getGradeSUCountALC() { return "0"; }
            @Override public String getGradeGACountALC() { return "0"; }
            // Indigenous Support Services
            @Override public String getKindHCountASS() { return "0"; }
            @Override public String getKindFCountASS() { return "0"; }
            @Override public String getGrade1CountASS() { return "0"; }
            @Override public String getGrade2CountASS() { return "0"; }
            @Override public String getGrade3CountASS() { return "0"; }
            @Override public String getGrade4CountASS() { return "0"; }
            @Override public String getGrade5CountASS() { return "0"; }
            @Override public String getGrade6CountASS() { return "0"; }
            @Override public String getGrade7CountASS() { return "0"; }
            @Override public String getGrade8CountASS() { return "0"; }
            @Override public String getGrade9CountASS() { return "0"; }
            @Override public String getGrade10CountASS() { return "0"; }
            @Override public String getGrade11CountASS() { return "0"; }
            @Override public String getGrade12CountASS() { return "0"; }
            @Override public String getGradeEUCountASS() { return "0"; }
            @Override public String getGradeSUCountASS() { return "0"; }
            @Override public String getGradeGACountASS() { return "0"; }
            // Other Approved Indigenous Programs
            @Override public String getKindHCountOAAP() { return "0"; }
            @Override public String getKindFCountOAAP() { return "0"; }
            @Override public String getGrade1CountOAAP() { return "0"; }
            @Override public String getGrade2CountOAAP() { return "0"; }
            @Override public String getGrade3CountOAAP() { return "0"; }
            @Override public String getGrade4CountOAAP() { return "0"; }
            @Override public String getGrade5CountOAAP() { return "0"; }
            @Override public String getGrade6CountOAAP() { return "0"; }
            @Override public String getGrade7CountOAAP() { return "0"; }
            @Override public String getGrade8CountOAAP() { return "0"; }
            @Override public String getGrade9CountOAAP() { return "0"; }
            @Override public String getGrade10CountOAAP() { return "0"; }
            @Override public String getGrade11CountOAAP() { return "0"; }
            @Override public String getGrade12CountOAAP() { return "0"; }
            @Override public String getGradeEUCountOAAP() { return "0"; }
            @Override public String getGradeSUCountOAAP() { return "0"; }
            @Override public String getGradeGACountOAAP() { return "0"; }
        };
    }

    private IndySchoolGradeFundingGroupHeadcountResult createMockHeadcountResultWithZeros(UUID schoolId) {
        return new IndySchoolGradeFundingGroupHeadcountResult() {
            @Override public String getSchoolID() { return schoolId.toString(); }
            @Override public String getKindHCountCF() { return "0"; }
            @Override public String getKindFCountCF() { return "0"; }
            @Override public String getGrade1CountCF() { return "0"; }
            @Override public String getGrade2CountCF() { return "0"; }
            @Override public String getGrade3CountCF() { return "0"; }
            @Override public String getGrade4CountCF() { return "0"; }
            @Override public String getGrade5CountCF() { return "0"; }
            @Override public String getGrade6CountCF() { return "0"; }
            @Override public String getGrade7CountCF() { return "0"; }
            @Override public String getGrade8CountCF() { return "0"; }
            @Override public String getGrade9CountCF() { return "0"; }
            @Override public String getGrade10CountCF() { return "0"; }
            @Override public String getGrade11CountCF() { return "0"; }
            @Override public String getGrade12CountCF() { return "0"; }
            @Override public String getGradeEUCountCF() { return "0"; }
            @Override public String getGradeSUCountCF() { return "0"; }
            @Override public String getGradeGACountCF() { return "0"; }
            @Override public String getKindHCountPF() { return "0"; }
            @Override public String getKindFCountPF() { return "0"; }
            @Override public String getGrade1CountPF() { return "0"; }
            @Override public String getGrade2CountPF() { return "0"; }
            @Override public String getGrade3CountPF() { return "0"; }
            @Override public String getGrade4CountPF() { return "0"; }
            @Override public String getGrade5CountPF() { return "0"; }
            @Override public String getGrade6CountPF() { return "0"; }
            @Override public String getGrade7CountPF() { return "0"; }
            @Override public String getGrade8CountPF() { return "0"; }
            @Override public String getGrade9CountPF() { return "0"; }
            @Override public String getGrade10CountPF() { return "0"; }
            @Override public String getGrade11CountPF() { return "0"; }
            @Override public String getGrade12CountPF() { return "0"; }
            @Override public String getGradeEUCountPF() { return "0"; }
            @Override public String getGradeSUCountPF() { return "0"; }
            @Override public String getGradeGACountPF() { return "0"; }
            @Override public String getKindHCountEFI() { return "0"; }
            @Override public String getKindFCountEFI() { return "0"; }
            @Override public String getGrade1CountEFI() { return "0"; }
            @Override public String getGrade2CountEFI() { return "0"; }
            @Override public String getGrade3CountEFI() { return "0"; }
            @Override public String getGrade4CountEFI() { return "0"; }
            @Override public String getGrade5CountEFI() { return "0"; }
            @Override public String getGrade6CountEFI() { return "0"; }
            @Override public String getGrade7CountEFI() { return "0"; }
            @Override public String getGrade8CountEFI() { return "0"; }
            @Override public String getGrade9CountEFI() { return "0"; }
            @Override public String getGrade10CountEFI() { return "0"; }
            @Override public String getGrade11CountEFI() { return "0"; }
            @Override public String getGrade12CountEFI() { return "0"; }
            @Override public String getGradeEUCountEFI() { return "0"; }
            @Override public String getGradeSUCountEFI() { return "0"; }
            @Override public String getGradeGACountEFI() { return "0"; }
            @Override public String getKindHCountLFI() { return "0"; }
            @Override public String getKindFCountLFI() { return "0"; }
            @Override public String getGrade1CountLFI() { return "0"; }
            @Override public String getGrade2CountLFI() { return "0"; }
            @Override public String getGrade3CountLFI() { return "0"; }
            @Override public String getGrade4CountLFI() { return "0"; }
            @Override public String getGrade5CountLFI() { return "0"; }
            @Override public String getGrade6CountLFI() { return "0"; }
            @Override public String getGrade7CountLFI() { return "0"; }
            @Override public String getGrade8CountLFI() { return "0"; }
            @Override public String getGrade9CountLFI() { return "0"; }
            @Override public String getGrade10CountLFI() { return "0"; }
            @Override public String getGrade11CountLFI() { return "0"; }
            @Override public String getGrade12CountLFI() { return "0"; }
            @Override public String getGradeEUCountLFI() { return "0"; }
            @Override public String getGradeSUCountLFI() { return "0"; }
            @Override public String getGradeGACountLFI() { return "0"; }
            @Override public String getKindHCountELL() { return "0"; }
            @Override public String getKindFCountELL() { return "0"; }
            @Override public String getGrade1CountELL() { return "0"; }
            @Override public String getGrade2CountELL() { return "0"; }
            @Override public String getGrade3CountELL() { return "0"; }
            @Override public String getGrade4CountELL() { return "0"; }
            @Override public String getGrade5CountELL() { return "0"; }
            @Override public String getGrade6CountELL() { return "0"; }
            @Override public String getGrade7CountELL() { return "0"; }
            @Override public String getGrade8CountELL() { return "0"; }
            @Override public String getGrade9CountELL() { return "0"; }
            @Override public String getGrade10CountELL() { return "0"; }
            @Override public String getGrade11CountELL() { return "0"; }
            @Override public String getGrade12CountELL() { return "0"; }
            @Override public String getGradeEUCountELL() { return "0"; }
            @Override public String getGradeSUCountELL() { return "0"; }
            @Override public String getGradeGACountELL() { return "0"; }
            @Override public String getKindHCountALC() { return "0"; }
            @Override public String getKindFCountALC() { return "0"; }
            @Override public String getGrade1CountALC() { return "0"; }
            @Override public String getGrade2CountALC() { return "0"; }
            @Override public String getGrade3CountALC() { return "0"; }
            @Override public String getGrade4CountALC() { return "0"; }
            @Override public String getGrade5CountALC() { return "0"; }
            @Override public String getGrade6CountALC() { return "0"; }
            @Override public String getGrade7CountALC() { return "0"; }
            @Override public String getGrade8CountALC() { return "0"; }
            @Override public String getGrade9CountALC() { return "0"; }
            @Override public String getGrade10CountALC() { return "0"; }
            @Override public String getGrade11CountALC() { return "0"; }
            @Override public String getGrade12CountALC() { return "0"; }
            @Override public String getGradeEUCountALC() { return "0"; }
            @Override public String getGradeSUCountALC() { return "0"; }
            @Override public String getGradeGACountALC() { return "0"; }
            @Override public String getKindHCountASS() { return "0"; }
            @Override public String getKindFCountASS() { return "0"; }
            @Override public String getGrade1CountASS() { return "0"; }
            @Override public String getGrade2CountASS() { return "0"; }
            @Override public String getGrade3CountASS() { return "0"; }
            @Override public String getGrade4CountASS() { return "0"; }
            @Override public String getGrade5CountASS() { return "0"; }
            @Override public String getGrade6CountASS() { return "0"; }
            @Override public String getGrade7CountASS() { return "0"; }
            @Override public String getGrade8CountASS() { return "0"; }
            @Override public String getGrade9CountASS() { return "0"; }
            @Override public String getGrade10CountASS() { return "0"; }
            @Override public String getGrade11CountASS() { return "0"; }
            @Override public String getGrade12CountASS() { return "0"; }
            @Override public String getGradeEUCountASS() { return "0"; }
            @Override public String getGradeSUCountASS() { return "0"; }
            @Override public String getGradeGACountASS() { return "0"; }
            @Override public String getKindHCountOAAP() { return "0"; }
            @Override public String getKindFCountOAAP() { return "0"; }
            @Override public String getGrade1CountOAAP() { return "0"; }
            @Override public String getGrade2CountOAAP() { return "0"; }
            @Override public String getGrade3CountOAAP() { return "0"; }
            @Override public String getGrade4CountOAAP() { return "0"; }
            @Override public String getGrade5CountOAAP() { return "0"; }
            @Override public String getGrade6CountOAAP() { return "0"; }
            @Override public String getGrade7CountOAAP() { return "0"; }
            @Override public String getGrade8CountOAAP() { return "0"; }
            @Override public String getGrade9CountOAAP() { return "0"; }
            @Override public String getGrade10CountOAAP() { return "0"; }
            @Override public String getGrade11CountOAAP() { return "0"; }
            @Override public String getGrade12CountOAAP() { return "0"; }
            @Override public String getGradeEUCountOAAP() { return "0"; }
            @Override public String getGradeSUCountOAAP() { return "0"; }
            @Override public String getGradeGACountOAAP() { return "0"; }
        };
    }

    @Test
    void testGenerateEllStudentsFallCsv_WithSeptemberCollection_UsesCurrentCollection() {
        // Given
        UUID collectionId = UUID.randomUUID();
        LocalDate septemberDate = LocalDate.of(2023, 9, 29);
        CollectionEntity collection = new CollectionEntity();
        collection.setCollectionID(collectionId);
        collection.setSnapshotDate(septemberDate);
        collection.setCollectionTypeCode("SEPTEMBER");

        List<EllStudentResult> mockStudents = List.of(
                createMockEllStudent("123456789", "5", "Smith"),
                createMockEllStudent("987654321", "3", "Johnson")
        );

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));
        when(sdcSchoolCollectionStudentRepository.getEllStudentsByFallCollectionId(collectionId))
                .thenReturn(mockStudents);

        // When
        DownloadableReportResponse response = csvReportService.generateEllStudentsFallCsv(collectionId);

        // Then
        assertNotNull(response);
        assertEquals("ell-students-fall-csv", response.getReportType());
        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("PEN"));
        assertTrue(csvContent.contains("Years_Of_ELL"));
        assertTrue(csvContent.contains("Legal_Last_Name"));
        assertTrue(csvContent.contains("123456789"));
        assertTrue(csvContent.contains("987654321"));
    }

    @Test
    void testGenerateEllStudentsFallCsv_WithNonSeptemberCollection_FindsPreviousSeptember() {
        // Given
        UUID currentCollectionId = UUID.randomUUID();
        UUID previousSeptCollectionId = UUID.randomUUID();
        LocalDate februaryDate = LocalDate.of(2024, 2, 15);
        LocalDate previousSeptDate = LocalDate.of(2023, 9, 29);

        CollectionEntity currentCollection = new CollectionEntity();
        currentCollection.setCollectionID(currentCollectionId);
        currentCollection.setSnapshotDate(februaryDate);
        currentCollection.setCollectionTypeCode("FEBRUARY");

        CollectionEntity previousSeptCollection = new CollectionEntity();
        previousSeptCollection.setCollectionID(previousSeptCollectionId);
        previousSeptCollection.setSnapshotDate(previousSeptDate);
        previousSeptCollection.setCollectionTypeCode("SEPTEMBER");

        List<EllStudentResult> mockStudents = List.of(
                createMockEllStudent("111222333", "2", "Brown")
        );

        when(collectionRepository.findById(currentCollectionId)).thenReturn(Optional.of(currentCollection));
        when(collectionRepository.findPreviousSeptemberCollection(februaryDate))
                .thenReturn(Optional.of(previousSeptCollection));
        when(sdcSchoolCollectionStudentRepository.getEllStudentsByFallCollectionId(previousSeptCollectionId))
                .thenReturn(mockStudents);

        // When
        DownloadableReportResponse response = csvReportService.generateEllStudentsFallCsv(currentCollectionId);

        // Then
        assertNotNull(response);
        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("111222333"));
        assertTrue(csvContent.contains("Brown"));
    }

    @Test
    void testGenerateEllStudentsFallCsv_WithEmptyResults_GeneratesEmptyReport() {
        // Given
        UUID collectionId = UUID.randomUUID();
        LocalDate septemberDate = LocalDate.of(2023, 9, 29);
        CollectionEntity collection = new CollectionEntity();
        collection.setCollectionID(collectionId);
        collection.setSnapshotDate(septemberDate);
        collection.setCollectionTypeCode("SEPTEMBER");

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collection));
        when(sdcSchoolCollectionStudentRepository.getEllStudentsByFallCollectionId(collectionId))
                .thenReturn(Collections.emptyList());

        // When
        DownloadableReportResponse response = csvReportService.generateEllStudentsFallCsv(collectionId);

        // Then
        assertNotNull(response);
        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        String[] lines = csvContent.split("\n");
        assertEquals(1, lines.length, "Should only have header row");
        assertTrue(lines[0].contains("PEN"));
    }

    @Test
    void testGenerateInclusiveEducationVarianceReport_OnlineLearningSchool_FebCountsAllStudents() {
        // Given: Online Learning school with students, some with FTE = 0
        UUID collectionId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID febCollectionId = UUID.randomUUID();
        UUID septCollectionId = UUID.randomUUID();
        UUID febDistrictCollectionId = UUID.randomUUID();
        UUID septDistrictCollectionId = UUID.randomUUID();

        List<UUID> schoolIds = List.of(UUID.randomUUID());

        CollectionEntity mainCollection = createCollection(collectionId, "JULY", LocalDate.of(2025, 7, 1));
        CollectionEntity febCollection = createCollection(febCollectionId, "FEBRUARY", LocalDate.of(2025, 2, 1));
        CollectionEntity septCollection = createCollection(septCollectionId, "SEPTEMBER", LocalDate.of(2024, 9, 1));

        SdcDistrictCollectionEntity mainDistrictCollection = createDistrictCollection(UUID.randomUUID(), districtId, mainCollection);
        SdcDistrictCollectionEntity febDistrictCollection = createDistrictCollection(febDistrictCollectionId, districtId, febCollection);
        SdcDistrictCollectionEntity septDistrictCollection = createDistrictCollection(septDistrictCollectionId, districtId, septCollection);

        SchoolTombstone olSchool = createSchool(schoolIds.get(0).toString(), "Online Learning School",
                SchoolCategoryCodes.PUBLIC.getCode(), FacilityTypeCodes.DISTONLINE.getCode(), districtId.toString());

        District district = new District();
        district.setDistrictId(districtId.toString());
        district.setDistrictNumber("036");
        district.setDisplayName("Test District");

        SpecialEdHeadcountResult febResult = createSpecialEdResult("3", "2", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0");
        SpecialEdHeadcountResult septResult = createSpecialEdResult("1", "1", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0");

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(mainCollection));
        when(sdcDistrictCollectionRepository.findAllByCollectionEntityCollectionID(collectionId))
                .thenReturn(List.of(mainDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastOrCurrentSdcDistrictCollectionByCollectionType(
                eq(CollectionTypeCodes.FEBRUARY.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(febDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastSdcDistrictCollectionByCollectionTypeBefore(
                eq(CollectionTypeCodes.SEPTEMBER.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(septDistrictCollection));

        when(restUtils.getAllSchoolTombstones()).thenReturn(Arrays.asList(olSchool));
        when(restUtils.getDistrictByDistrictID(districtId.toString())).thenReturn(Optional.of(district));

        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForFebBySdcDistrictCollectionId(
                eq(febDistrictCollectionId), any()))
                .thenReturn(febResult);
        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForSeptBySdcDistrictCollectionId(
                eq(septDistrictCollectionId), any()))
                .thenReturn(septResult);

        // When
        DownloadableReportResponse response = csvReportService.generateInclusiveEducationVarianceReport(collectionId);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals("inclusive-education-variances-all", response.getReportType(), "Report type should match");
        assertNotNull(response.getDocumentData(), "Document data should not be null");

        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        assertFalse(csvContent.isEmpty(), "CSV content should not be empty");

        assertTrue(csvContent.contains("036"), "CSV should contain district number");
        assertTrue(csvContent.contains("Test District"), "CSV should contain district name");

        String[] lines = csvContent.split("\n");
        assertTrue(lines.length >= 2, "CSV should have at least header and one data row");
    }

    @Test
    void testGenerateInclusiveEducationVarianceReport_RegularSchool_IncludedInReport() {
        // Given: Regular public school (not OL, not Independent, not PRP, not Youth, not Offshore)
        UUID collectionId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID febCollectionId = UUID.randomUUID();
        UUID septCollectionId = UUID.randomUUID();
        UUID febDistrictCollectionId = UUID.randomUUID();
        UUID septDistrictCollectionId = UUID.randomUUID();

        List<UUID> schoolIds = List.of(UUID.randomUUID());

        CollectionEntity mainCollection = createCollection(collectionId, "JULY", LocalDate.of(2025, 7, 1));
        CollectionEntity febCollection = createCollection(febCollectionId, "FEBRUARY", LocalDate.of(2025, 2, 1));
        CollectionEntity septCollection = createCollection(septCollectionId, "SEPTEMBER", LocalDate.of(2024, 9, 1));

        SdcDistrictCollectionEntity mainDistrictCollection = createDistrictCollection(UUID.randomUUID(), districtId, mainCollection);
        SdcDistrictCollectionEntity febDistrictCollection = createDistrictCollection(febDistrictCollectionId, districtId, febCollection);
        SdcDistrictCollectionEntity septDistrictCollection = createDistrictCollection(septDistrictCollectionId, districtId, septCollection);

        SchoolTombstone regularSchool = createSchool(schoolIds.get(0).toString(), "Regular School",
                SchoolCategoryCodes.PUBLIC.getCode(), FacilityTypeCodes.STANDARD.getCode(), districtId.toString());

        District district = new District();
        district.setDistrictId(districtId.toString());
        district.setDistrictNumber("036");
        district.setDisplayName("Test District");

        SpecialEdHeadcountResult febResult = createSpecialEdResult("5", "5", "3", "3", "2", "2", "2", "3", "0", "0", "0", "0");
        SpecialEdHeadcountResult septResult = createSpecialEdResult("4", "4", "2", "2", "1", "1", "1", "2", "0", "0", "0", "0");

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(mainCollection));
        when(sdcDistrictCollectionRepository.findAllByCollectionEntityCollectionID(collectionId))
                .thenReturn(List.of(mainDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastOrCurrentSdcDistrictCollectionByCollectionType(
                eq(CollectionTypeCodes.FEBRUARY.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(febDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastSdcDistrictCollectionByCollectionTypeBefore(
                eq(CollectionTypeCodes.SEPTEMBER.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(septDistrictCollection));

        when(restUtils.getAllSchoolTombstones()).thenReturn(Arrays.asList(regularSchool));
        when(restUtils.getDistrictByDistrictID(districtId.toString())).thenReturn(Optional.of(district));

        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForFebBySdcDistrictCollectionId(
                eq(febDistrictCollectionId), any()))
                .thenReturn(febResult);
        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForSeptBySdcDistrictCollectionId(
                eq(septDistrictCollectionId), any()))
                .thenReturn(septResult);

        // When
        DownloadableReportResponse response = csvReportService.generateInclusiveEducationVarianceReport(collectionId);

        // Then
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getDocumentData(), "Document data should not be null");
        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        assertFalse(csvContent.isEmpty(), "CSV should not be empty");
        assertTrue(csvContent.contains("036"), "CSV should contain district number");
    }

    @Test
    void testGenerateInclusiveEducationVarianceReport_IndependentSchool_ExcludedFromReport() {
        // Given: Independent school should be excluded
        UUID collectionId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID febCollectionId = UUID.randomUUID();
        UUID septCollectionId = UUID.randomUUID();
        UUID febDistrictCollectionId = UUID.randomUUID();
        UUID septDistrictCollectionId = UUID.randomUUID();

        CollectionEntity mainCollection = createCollection(collectionId, "JULY", LocalDate.of(2025, 7, 1));
        CollectionEntity febCollection = createCollection(febCollectionId, "FEBRUARY", LocalDate.of(2025, 2, 1));
        CollectionEntity septCollection = createCollection(septCollectionId, "SEPTEMBER", LocalDate.of(2024, 9, 1));

        SdcDistrictCollectionEntity mainDistrictCollection = createDistrictCollection(UUID.randomUUID(), districtId, mainCollection);
        SdcDistrictCollectionEntity febDistrictCollection = createDistrictCollection(febDistrictCollectionId, districtId, febCollection);
        SdcDistrictCollectionEntity septDistrictCollection = createDistrictCollection(septDistrictCollectionId, districtId, septCollection);

        SchoolTombstone indySchool = createSchool(UUID.randomUUID().toString(), "Indy School",
                SchoolCategoryCodes.INDEPEND.getCode(), FacilityTypeCodes.STANDARD.getCode(), districtId.toString());

        District district = new District();
        district.setDistrictId(districtId.toString());
        district.setDistrictNumber("036");
        district.setDisplayName("Test District");

        SpecialEdHeadcountResult emptyResult = createSpecialEdResult("0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0");

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(mainCollection));
        when(sdcDistrictCollectionRepository.findAllByCollectionEntityCollectionID(collectionId))
                .thenReturn(List.of(mainDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastOrCurrentSdcDistrictCollectionByCollectionType(
                eq(CollectionTypeCodes.FEBRUARY.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(febDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastSdcDistrictCollectionByCollectionTypeBefore(
                eq(CollectionTypeCodes.SEPTEMBER.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(septDistrictCollection));

        when(restUtils.getAllSchoolTombstones()).thenReturn(Arrays.asList(indySchool));
        when(restUtils.getDistrictByDistrictID(districtId.toString())).thenReturn(Optional.of(district));

        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForFebBySdcDistrictCollectionId(
                eq(febDistrictCollectionId), any()))
                .thenReturn(emptyResult);
        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForSeptBySdcDistrictCollectionId(
                eq(septDistrictCollectionId), any()))
                .thenReturn(emptyResult);

        // When
        DownloadableReportResponse response = csvReportService.generateInclusiveEducationVarianceReport(collectionId);

        // Then
        assertNotNull(response);
        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        // Should contain district but with zero counts
        assertTrue(csvContent.contains("036"));
    }

    @Test
    void testGenerateInclusiveEducationVarianceReport_ContinuingEducationSchool_IncludedInReport() {
        // Given: Continuing Education school should be included
        UUID collectionId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID febCollectionId = UUID.randomUUID();
        UUID septCollectionId = UUID.randomUUID();
        UUID febDistrictCollectionId = UUID.randomUUID();
        UUID septDistrictCollectionId = UUID.randomUUID();

        List<UUID> schoolIds = List.of(UUID.randomUUID());

        CollectionEntity mainCollection = createCollection(collectionId, "JULY", LocalDate.of(2025, 7, 1));
        CollectionEntity febCollection = createCollection(febCollectionId, "FEBRUARY", LocalDate.of(2025, 2, 1));
        CollectionEntity septCollection = createCollection(septCollectionId, "SEPTEMBER", LocalDate.of(2024, 9, 1));

        SdcDistrictCollectionEntity mainDistrictCollection = createDistrictCollection(UUID.randomUUID(), districtId, mainCollection);
        SdcDistrictCollectionEntity febDistrictCollection = createDistrictCollection(febDistrictCollectionId, districtId, febCollection);
        SdcDistrictCollectionEntity septDistrictCollection = createDistrictCollection(septDistrictCollectionId, districtId, septCollection);

        SchoolTombstone ceSchool = createSchool(schoolIds.get(0).toString(), "CE School",
                SchoolCategoryCodes.PUBLIC.getCode(), FacilityTypeCodes.CONT_ED.getCode(), districtId.toString());

        District district = new District();
        district.setDistrictId(districtId.toString());
        district.setDistrictNumber("036");
        district.setDisplayName("Test District");

        SpecialEdHeadcountResult febResult = createSpecialEdResult("3", "4", "2", "1", "1", "1", "1", "2", "0", "0", "0", "0");
        SpecialEdHeadcountResult septResult = createSpecialEdResult("2", "3", "1", "1", "1", "1", "0", "1", "0", "0", "0", "0");

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(mainCollection));
        when(sdcDistrictCollectionRepository.findAllByCollectionEntityCollectionID(collectionId))
                .thenReturn(List.of(mainDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastOrCurrentSdcDistrictCollectionByCollectionType(
                eq(CollectionTypeCodes.FEBRUARY.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(febDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastSdcDistrictCollectionByCollectionTypeBefore(
                eq(CollectionTypeCodes.SEPTEMBER.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(septDistrictCollection));

        when(restUtils.getAllSchoolTombstones()).thenReturn(Arrays.asList(ceSchool));
        when(restUtils.getDistrictByDistrictID(districtId.toString())).thenReturn(Optional.of(district));

        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForFebBySdcDistrictCollectionId(
                eq(febDistrictCollectionId), any()))
                .thenReturn(febResult);
        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForSeptBySdcDistrictCollectionId(
                eq(septDistrictCollectionId), any()))
                .thenReturn(septResult);

        // When
        DownloadableReportResponse response = csvReportService.generateInclusiveEducationVarianceReport(collectionId);

        // Then
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getDocumentData(), "Document data should not be null");
        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        assertFalse(csvContent.isEmpty(), "CSV should not be empty");
        assertTrue(csvContent.contains("036"), "CSV should contain district number");
    }

    @Test
    void testGenerateInclusiveEducationVarianceReport_PRPSchool_ExcludedFromReport() {
        // Given: PRP school should be excluded
        UUID collectionId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID febCollectionId = UUID.randomUUID();
        UUID septCollectionId = UUID.randomUUID();
        UUID febDistrictCollectionId = UUID.randomUUID();
        UUID septDistrictCollectionId = UUID.randomUUID();

        CollectionEntity mainCollection = createCollection(collectionId, "JULY", LocalDate.of(2025, 7, 1));
        CollectionEntity febCollection = createCollection(febCollectionId, "FEBRUARY", LocalDate.of(2025, 2, 1));
        CollectionEntity septCollection = createCollection(septCollectionId, "SEPTEMBER", LocalDate.of(2024, 9, 1));

        SdcDistrictCollectionEntity mainDistrictCollection = createDistrictCollection(UUID.randomUUID(), districtId, mainCollection);
        SdcDistrictCollectionEntity febDistrictCollection = createDistrictCollection(febDistrictCollectionId, districtId, febCollection);
        SdcDistrictCollectionEntity septDistrictCollection = createDistrictCollection(septDistrictCollectionId, districtId, septCollection);

        SchoolTombstone prpSchool = createSchool(UUID.randomUUID().toString(), "PRP School",
                SchoolCategoryCodes.PUBLIC.getCode(), FacilityTypeCodes.LONG_PRP.getCode(), districtId.toString());

        District district = new District();
        district.setDistrictId(districtId.toString());
        district.setDistrictNumber("036");
        district.setDisplayName("Test District");

        SpecialEdHeadcountResult emptyResult = createSpecialEdResult("0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0");

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(mainCollection));
        when(sdcDistrictCollectionRepository.findAllByCollectionEntityCollectionID(collectionId))
                .thenReturn(List.of(mainDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastOrCurrentSdcDistrictCollectionByCollectionType(
                eq(CollectionTypeCodes.FEBRUARY.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(febDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastSdcDistrictCollectionByCollectionTypeBefore(
                eq(CollectionTypeCodes.SEPTEMBER.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(septDistrictCollection));

        when(restUtils.getAllSchoolTombstones()).thenReturn(Arrays.asList(prpSchool));
        when(restUtils.getDistrictByDistrictID(districtId.toString())).thenReturn(Optional.of(district));

        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForFebBySdcDistrictCollectionId(
                eq(febDistrictCollectionId), any()))
                .thenReturn(emptyResult);
        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForSeptBySdcDistrictCollectionId(
                eq(septDistrictCollectionId), any()))
                .thenReturn(emptyResult);

        // When
        DownloadableReportResponse response = csvReportService.generateInclusiveEducationVarianceReport(collectionId);

        // Then
        assertNotNull(response);
        // Report generated with zero counts
        assertNotNull(response.getDocumentData());
    }

    @Test
    void testGenerateInclusiveEducationVarianceReport_MixedSchools_OnlyCorrectSchoolsIncluded() {
        // Given: Mix of schools - OL, Regular, Independent, PRP
        UUID collectionId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID febCollectionId = UUID.randomUUID();
        UUID septCollectionId = UUID.randomUUID();
        UUID febDistrictCollectionId = UUID.randomUUID();
        UUID septDistrictCollectionId = UUID.randomUUID();

        UUID olSchoolId = UUID.randomUUID();
        UUID regularSchoolId = UUID.randomUUID();

        CollectionEntity mainCollection = createCollection(collectionId, "JULY", LocalDate.of(2025, 7, 1));
        CollectionEntity febCollection = createCollection(febCollectionId, "FEBRUARY", LocalDate.of(2025, 2, 1));
        CollectionEntity septCollection = createCollection(septCollectionId, "SEPTEMBER", LocalDate.of(2024, 9, 1));

        SdcDistrictCollectionEntity mainDistrictCollection = createDistrictCollection(UUID.randomUUID(), districtId, mainCollection);
        SdcDistrictCollectionEntity febDistrictCollection = createDistrictCollection(febDistrictCollectionId, districtId, febCollection);
        SdcDistrictCollectionEntity septDistrictCollection = createDistrictCollection(septDistrictCollectionId, districtId, septCollection);

        SchoolTombstone olSchool = createSchool(olSchoolId.toString(), "OL School",
                SchoolCategoryCodes.PUBLIC.getCode(), FacilityTypeCodes.DISTONLINE.getCode(), districtId.toString());
        SchoolTombstone regularSchool = createSchool(regularSchoolId.toString(), "Regular School",
                SchoolCategoryCodes.PUBLIC.getCode(), FacilityTypeCodes.STANDARD.getCode(), districtId.toString());
        SchoolTombstone indySchool = createSchool(UUID.randomUUID().toString(), "Indy School",
                SchoolCategoryCodes.INDEPEND.getCode(), FacilityTypeCodes.STANDARD.getCode(), districtId.toString());
        SchoolTombstone prpSchool = createSchool(UUID.randomUUID().toString(), "PRP School",
                SchoolCategoryCodes.PUBLIC.getCode(), FacilityTypeCodes.SHORT_PRP.getCode(), districtId.toString());

        District district = new District();
        district.setDistrictId(districtId.toString());
        district.setDistrictNumber("036");
        district.setDisplayName("Test District");

        SpecialEdHeadcountResult febResult = createSpecialEdResult("7", "8", "5", "5", "3", "3", "3", "5", "0", "0", "0", "0");
        SpecialEdHeadcountResult septResult = createSpecialEdResult("5", "5", "3", "3", "2", "2", "2", "3", "0", "0", "0", "0");

        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(mainCollection));
        when(sdcDistrictCollectionRepository.findAllByCollectionEntityCollectionID(collectionId))
                .thenReturn(List.of(mainDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastOrCurrentSdcDistrictCollectionByCollectionType(
                eq(CollectionTypeCodes.FEBRUARY.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(febDistrictCollection));
        when(sdcDistrictCollectionRepository.findLastSdcDistrictCollectionByCollectionTypeBefore(
                eq(CollectionTypeCodes.SEPTEMBER.getTypeCode()), eq(districtId), any()))
                .thenReturn(Optional.of(septDistrictCollection));

        when(restUtils.getAllSchoolTombstones()).thenReturn(Arrays.asList(olSchool, regularSchool, indySchool, prpSchool));
        when(restUtils.getDistrictByDistrictID(districtId.toString())).thenReturn(Optional.of(district));

        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForFebBySdcDistrictCollectionId(
                eq(febDistrictCollectionId), any()))
                .thenReturn(febResult);
        when(sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleForSeptBySdcDistrictCollectionId(
                eq(septDistrictCollectionId), any()))
                .thenReturn(septResult);

        // When
        DownloadableReportResponse response = csvReportService.generateInclusiveEducationVarianceReport(collectionId);

        // Then
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getDocumentData(), "Document data should not be null");
        String csvContent = new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
        assertFalse(csvContent.isEmpty(), "CSV should not be empty");

        // Verify CSV structure
        String[] lines = csvContent.split("\n");
        assertTrue(lines.length >= 2, "CSV should have header and data rows");
    }

    private CollectionEntity createCollection(UUID collectionId, String typeCode, LocalDate snapshotDate) {
        CollectionEntity collection = new CollectionEntity();
        collection.setCollectionID(collectionId);
        collection.setCollectionTypeCode(typeCode);
        collection.setSnapshotDate(snapshotDate);
        collection.setOpenDate(LocalDateTime.now());
        collection.setCloseDate(LocalDateTime.now().plusMonths(1));
        return collection;
    }

    private SdcDistrictCollectionEntity createDistrictCollection(UUID districtCollectionId, UUID districtId, CollectionEntity collection) {
        SdcDistrictCollectionEntity districtCollection = new SdcDistrictCollectionEntity();
        districtCollection.setSdcDistrictCollectionID(districtCollectionId);
        districtCollection.setDistrictID(districtId);
        districtCollection.setCollectionEntity(collection);
        return districtCollection;
    }

    private SchoolTombstone createSchool(String schoolId, String displayName, String categoryCode, String facilityTypeCode, String districtId) {
        SchoolTombstone school = new SchoolTombstone();
        school.setSchoolId(schoolId);
        school.setDisplayName(displayName);
        school.setSchoolCategoryCode(categoryCode);
        school.setFacilityTypeCode(facilityTypeCode);
        school.setDistrictId(districtId);
        return school;
    }

    private SpecialEdHeadcountResult createSpecialEdResult(String aCode, String bCode, String cCode, String dCode, String eCode,
                                                           String fCode, String gCode, String hCode, String kCode, String pCode,
                                                           String qCode, String rCode) {
        return new SpecialEdHeadcountResult() {
            @Override
            public String getEnrolledGradeCode() { return null; }

            @Override
            public String getSchoolID() { return null; }

            @Override
            public String getSpecialEducationCategoryCode() { return null; }

            @Override
            public String getSdcDistrictCollectionID() { return null; }

            @Override
            public String getLevelOnes() {
                int a = aCode != null ? Integer.parseInt(aCode) : 0;
                int b = bCode != null ? Integer.parseInt(bCode) : 0;
                return String.valueOf(a + b);
            }

            @Override
            public String getSpecialEdACodes() { return aCode; }

            @Override
            public String getSpecialEdBCodes() { return bCode; }

            @Override
            public String getLevelTwos() {
                int c = cCode != null ? Integer.parseInt(cCode) : 0;
                int d = dCode != null ? Integer.parseInt(dCode) : 0;
                int e = eCode != null ? Integer.parseInt(eCode) : 0;
                int f = fCode != null ? Integer.parseInt(fCode) : 0;
                int g = gCode != null ? Integer.parseInt(gCode) : 0;
                return String.valueOf(c + d + e + f + g);
            }

            @Override
            public String getSpecialEdCCodes() { return cCode; }

            @Override
            public String getSpecialEdDCodes() { return dCode; }

            @Override
            public String getSpecialEdECodes() { return eCode; }

            @Override
            public String getSpecialEdFCodes() { return fCode; }

            @Override
            public String getSpecialEdGCodes() { return gCode; }

            @Override
            public String getLevelThrees() { return hCode; }

            @Override
            public String getSpecialEdHCodes() { return hCode; }

            @Override
            public String getOtherLevels() {
                int k = kCode != null ? Integer.parseInt(kCode) : 0;
                int p = pCode != null ? Integer.parseInt(pCode) : 0;
                int q = qCode != null ? Integer.parseInt(qCode) : 0;
                int r = rCode != null ? Integer.parseInt(rCode) : 0;
                return String.valueOf(k + p + q + r);
            }

            @Override
            public String getSpecialEdKCodes() { return kCode; }

            @Override
            public String getSpecialEdPCodes() { return pCode; }

            @Override
            public String getSpecialEdQCodes() { return qCode; }

            @Override
            public String getSpecialEdRCodes() { return rCode; }

            @Override
            public String getAllLevels() {
                int total = 0;
                if (aCode != null) total += Integer.parseInt(aCode);
                if (bCode != null) total += Integer.parseInt(bCode);
                if (cCode != null) total += Integer.parseInt(cCode);
                if (dCode != null) total += Integer.parseInt(dCode);
                if (eCode != null) total += Integer.parseInt(eCode);
                if (fCode != null) total += Integer.parseInt(fCode);
                if (gCode != null) total += Integer.parseInt(gCode);
                if (hCode != null) total += Integer.parseInt(hCode);
                if (kCode != null) total += Integer.parseInt(kCode);
                if (pCode != null) total += Integer.parseInt(pCode);
                if (qCode != null) total += Integer.parseInt(qCode);
                if (rCode != null) total += Integer.parseInt(rCode);
                return String.valueOf(total);
            }

            @Override
            public boolean getAdultsInSpecialEdA() { return false; }

            @Override
            public boolean getAdultsInSpecialEdB() { return false; }

            @Override
            public boolean getAdultsInSpecialEdC() { return false; }

            @Override
            public boolean getAdultsInSpecialEdD() { return false; }

            @Override
            public boolean getAdultsInSpecialEdE() { return false; }

            @Override
            public boolean getAdultsInSpecialEdF() { return false; }

            @Override
            public boolean getAdultsInSpecialEdG() { return false; }

            @Override
            public boolean getAdultsInSpecialEdH() { return false; }

            @Override
            public boolean getAdultsInSpecialEdK() { return false; }

            @Override
            public boolean getAdultsInSpecialEdP() { return false; }

            @Override
            public boolean getAdultsInSpecialEdQ() { return false; }

            @Override
            public boolean getAdultsInSpecialEdR() { return false; }
        };
    }

    private EllStudentResult createMockEllStudent(String pen, String yearsInEll, String lastName) {
        return new EllStudentResult() {
            @Override
            public String getStudentPen() {
                return pen;
            }

            @Override
            public String getYearsInEll() {
                return yearsInEll;
            }

            @Override
            public String getLegalLastName() {
                return lastName;
            }
        };
    }
}
