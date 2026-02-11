package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SimpleHeadcountResultsTable;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RequestMapping(URL.BASE_MINISTRY_HEADCOUNTS)
public interface MinistryHeadcountReports {

    @GetMapping("/{collectionID}/{type}")
    @PreAuthorize("hasAuthority('SCOPE_READ_SDC_MINISTRY_REPORTS')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    SimpleHeadcountResultsTable getMinistryHeadcounts(@PathVariable UUID collectionID, @PathVariable(name = "type") String type);

    @GetMapping("/allSchoolHeadcounts/{collectionID}")
    @PreAuthorize("hasAuthority('SCOPE_READ_SDC_MINISTRY_REPORTS')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    Map<String, Long> getAllSchoolHeadcounts(@PathVariable UUID collectionID);

    @GetMapping("/{collectionID}/{type}/download")
    @PreAuthorize("hasAuthority('SCOPE_READ_SDC_MINISTRY_REPORTS')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    DownloadableReportResponse getMinistryDownloadableReport(@PathVariable UUID collectionID, @PathVariable(name = "type") String type);

    @PostMapping("/allReports/{sdcDistrictCollectionID}")
    @PreAuthorize("hasAuthority('SCOPE_GENERATE_ALL_DISTRICT_REPORTS')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    void generateAllDistrictReportsForCollection(@PathVariable("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

    @PostMapping("/allReports/{sdcDistrictCollectionID}/streamChunked")
    @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    void generateAllDistrictReportsStreamChunked(@PathVariable("sdcDistrictCollectionID") UUID sdcDistrictCollectionID, HttpServletResponse response) throws IOException;
}
