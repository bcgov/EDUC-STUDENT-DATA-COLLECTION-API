package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.summary.StudentDifference;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RequestMapping(URL.BASE_URL_REPORT_GENERATION)
public interface ReportGenerationEndpoint {

  @GetMapping("/sdcSchoolCollection/{sdcSchoolCollectionID}/{reportTypeCode}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  DownloadableReportResponse generateSDCSchoolReport(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID, @PathVariable("reportTypeCode") String reportTypeCode);

  @GetMapping("/sdcDistrictCollection/{sdcDistrictCollectionID}/{reportTypeCode}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  DownloadableReportResponse generateSDCDistrictReport(@PathVariable("sdcDistrictCollectionID") UUID sdcDistrictCollectionID, @PathVariable("reportTypeCode") String reportTypeCode);

  @GetMapping("/differences")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  Page<StudentDifference> getStudentDifferences(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);

}
