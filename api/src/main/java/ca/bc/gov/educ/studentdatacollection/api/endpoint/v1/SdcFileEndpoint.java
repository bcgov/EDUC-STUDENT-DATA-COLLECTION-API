package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileSummary;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RequestMapping(URL.BASE_URL)
public interface SdcFileEndpoint {

  @PostMapping("/{sdcSchoolCollectionID}/file")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Tag(name = "Endpoint to Upload an SDC file and convert to json structure.", description = "Endpoint to Upload an SDC file and convert to json structure")
  @Schema(name = "FileUpload", implementation = SdcFileUpload.class)
  ResponseEntity<SdcSchoolCollection> processSdcBatchFile(@Validated @RequestBody SdcFileUpload fileUpload, @PathVariable(name = "sdcSchoolCollectionID") String sdcSchoolCollectionID, @RequestHeader(name = "correlationID") String correlationID);

  @GetMapping("/{sdcSchoolCollectionID}/file")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to check if provided SDC file is already in progress", description = "Endpoint to check if provided SDC file is in progress")
  ResponseEntity<SdcFileSummary> isBeingProcessed(@PathVariable(name = "sdcSchoolCollectionID") String sdcSchoolCollectionID);

}
