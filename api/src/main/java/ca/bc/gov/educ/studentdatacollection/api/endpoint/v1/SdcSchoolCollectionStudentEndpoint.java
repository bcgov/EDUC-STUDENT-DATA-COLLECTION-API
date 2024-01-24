package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentHeadcounts;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssueErrorWarningCount;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcStudentEll;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequestMapping(URL.BASE_URL_SCHOOL_COLLECTION_STUDENT)
public interface SdcSchoolCollectionStudentEndpoint {

  @GetMapping("/{sdcSchoolCollectionStudentID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION_STUDENT')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  SdcSchoolCollectionStudent getSdcSchoolCollectionStudent(@PathVariable("sdcSchoolCollectionStudentID") UUID collectionID);

  @GetMapping(URL.ERROR_WARNING_COUNT + "/{sdcSchoolCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION_STUDENT')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  SdcSchoolCollectionStudentValidationIssueErrorWarningCount getErrorAndWarningCountBySdcSchoolCollectionID(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @GetMapping(URL.PAGINATED)
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION_STUDENT')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  CompletableFuture<Page<SdcSchoolCollectionStudent>> findAll(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                              @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                              @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                              @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);

  @PutMapping("/{sdcSchoolCollectionStudentID}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Sdc School Collection Student", description = "Endpoints to update school collection student entity.")
  @Schema(name = "SdcSchoolCollectionStudent", implementation = SdcSchoolCollectionStudent.class)
  SdcSchoolCollectionStudent updateAndValidateSdcSchoolCollectionStudent(@PathVariable("sdcSchoolCollectionStudentID") UUID collectionStudentID, @Validated @RequestBody SdcSchoolCollectionStudent sdcSchoolCollectionStudent);

  @DeleteMapping("/{sdcSchoolCollectionStudentID}")
  @PreAuthorize("hasAuthority('SCOPE_DELETE_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  SdcSchoolCollectionStudent deleteSdcSchoolCollectionStudent(@PathVariable UUID sdcSchoolCollectionStudentID);

  @PostMapping("/years-in-ell")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  SdcStudentEll createYearsInEll(@RequestBody SdcStudentEll sdcStudentEll);

  @GetMapping(URL.HEADCOUNTS + "/{sdcSchoolCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION_STUDENT')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  SdcSchoolCollectionStudentHeadcounts getSdcSchoolCollectionStudentHeadcounts(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID,
                                                                               @RequestParam(name = "type") String type,
                                                                               @RequestParam(name = "compare", defaultValue = "false") boolean compare);
}
