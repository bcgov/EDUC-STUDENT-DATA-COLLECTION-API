package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndyFundingReportHeader;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.reports.CSVReportService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CSVReportServiceTest {

    @InjectMocks
    private CSVReportService csvReportService;

    @Mock
    private SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

    @Mock
    private CollectionRepository collectionRepository;

    private static final String INDY_FUNDING_REPORT_ALL = "indy-funding-report-all";
    private static final String ONLINE_INDY_FUNDING_REPORT = "online-indy-funding-report";
    private static final String INDY_FUNDING_REPORT_FUNDED = "indy-funding-report-funded";

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
}
