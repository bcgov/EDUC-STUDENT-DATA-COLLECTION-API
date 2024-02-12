package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RequestMapping(URL.BASE_URL_REPORT_GENERATION)
public interface ReportGenerationEndpoint {

  @GetMapping("/{sdcSchoolCollectionID}/{reportTypeCode}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  byte[] getSdcSchoolCollectionStudent(@PathVariable("sdcSchoolCollectionID") UUID collectionID, @PathVariable("reportTypeCode") String reportTypeCode);

}
