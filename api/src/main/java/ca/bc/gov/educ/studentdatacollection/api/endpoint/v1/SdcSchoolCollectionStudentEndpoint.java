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
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
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
  List<SdcSchoolCollectionStudentValidationIssueErrorWarningCount> getErrorAndWarningCountBySdcSchoolCollectionID(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @GetMapping(URL.PAGINATED)
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION_STUDENT')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  CompletableFuture<Page<SdcSchoolCollectionStudent>> findAll(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                              @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                              @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                              @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);

  @GetMapping(URL.PAGINATED_CSV)
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION_STUDENT')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  default CompletableFuture<ResponseEntity<byte[]>> findAllCsv(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                                      @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                                      @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson) {
    return this.findAll(pageNumber, pageSize, sortCriteriaJson, searchCriteriaListJson)
            .thenApplyAsync(page -> {
              try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                   CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(baos), CSVFormat.DEFAULT
                           .withHeader("School Code", "School Name", "P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate", "Fee Payer", "Refugee",
                                   "Native Ancestry", "Native Status", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses"))) {

                for (SdcSchoolCollectionStudent student : page.getContent()) {
                  List<? extends Serializable> csvRow = Arrays.asList(
                          student.getSdcSchoolCollectionID(),
                          student.getSdcSchoolCollectionID(),
                          student.getStudentPen(),
                          student.getLegalFirstName() + " " + student.getLegalLastName(),
                          student.getUsualFirstName() + " " + student.getUsualLastName(),
                          student.getDob(),
                          student.getGender(),
                          student.getPostalCode(),
                          student.getLocalID(),
                          student.getEnrolledGradeCode(),
                          student.getFte(),
                          student.getIsAdult(),
                          student.getIsGraduated(),
                          student.getIsGraduated(),
                          student.getIsGraduated(),
                          student.getNativeAncestryInd(),
                          student.getNativeAncestryInd(),
                          student.getSdcSchoolCollectionStudentStatusCode(),
                          student.getBandCode(),
                          student.getHomeLanguageSpokenCode(),
                          student.getNumberOfCourses(),
                          student.getSupportBlocks(),
                          student.getOtherCourses()
                  );
                  csvPrinter.printRecord(csvRow);
                }

                csvPrinter.flush();

                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sdcSchoolCollectionStudents.csv");
                headers.add(HttpHeaders.CONTENT_TYPE, "text/csv");

                return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
              } catch (IOException e) {
                throw new RuntimeException("Failed to generate CSV", e);
              }
            });
  }

  @PostMapping
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Sdc School Collection Student", description = "Endpoints to create and update school collection student entity.")
  @Schema(name = "SdcSchoolCollectionStudent", implementation = SdcSchoolCollectionStudent.class)
  SdcSchoolCollectionStudent createAndUpdateSdcSchoolCollectionStudent(@Validated @RequestBody SdcSchoolCollectionStudent sdcSchoolCollectionStudent);

  @DeleteMapping("/{sdcSchoolCollectionStudentID}")
  @PreAuthorize("hasAuthority('SCOPE_DELETE_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  SdcSchoolCollectionStudent deleteSdcSchoolCollectionStudent(@PathVariable UUID sdcSchoolCollectionStudentID);

  @PostMapping("/soft-delete-students")
  @PreAuthorize("hasAuthority('SCOPE_DELETE_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  List<SdcSchoolCollectionStudent> softDeleteSdcSchoolCollectionStudents(@RequestBody List<UUID> sdcStudentIDs);

  @PostMapping("/years-in-ell")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  List<SdcStudentEll> createYearsInEll(@RequestBody List<SdcStudentEll> sdcStudentElls);

  @GetMapping(URL.HEADCOUNTS + "/{sdcSchoolCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION_STUDENT')")
  @Transactional(readOnly = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  SdcSchoolCollectionStudentHeadcounts getSdcSchoolCollectionStudentHeadcounts(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID,
                                                                               @RequestParam(name = "type") String type,
                                                                               @RequestParam(name = "compare", defaultValue = "false") boolean compare);
}
