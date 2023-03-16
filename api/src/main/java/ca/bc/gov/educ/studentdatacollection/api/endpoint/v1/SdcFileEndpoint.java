package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcStudentCount;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequestMapping(URL.BASE_URL_SDC_FILE)
public interface SdcFileEndpoint {

  @PostMapping
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Transactional
  @Tag(name = "Endpoint to Upload an SDC file and convert to json structure.", description = "Endpoint to Upload an SDC file and convert to json structure")
  @Schema(name = "FileUpload", implementation = SdcFileUpload.class)
  ResponseEntity<Void> processSdcBatchFile(@Validated @RequestBody SdcFileUpload fileUpload, @RequestHeader(name = "correlationID") String correlationID);

  @GetMapping
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to check if provided SDC file is already in progress", description = "Endpoint to check if provided SDC file is in progress")
  ResponseEntity<List<SdcStudentCount>> isBeingProcessed(@RequestParam(name = "schoolID") String schoolID);

  @DeleteMapping
  @PreAuthorize("hasAuthority('SCOPE_DELETE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT")})
  @Transactional
  @Tag(name = "Endpoint to Delete the entire data set from this schools current upload", description = "Endpoint to Delete the entire data set from this schools current upload")
  ResponseEntity<Void> deleteAll(@RequestParam(name = "schoolID") String schoolID);

}
