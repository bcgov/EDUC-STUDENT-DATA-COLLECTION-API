package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicate;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicatesByInstituteID;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping(URL.BASE_URL_DUPLICATE)
public interface SdcDuplicateEndpoint {

  @PostMapping("/type/{duplicateTypeCode}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Sdc Duplicate", description = "Endpoints to edit and resolve sdc duplicates.")
  SdcDuplicate updateStudentAndResolveDuplicates(@PathVariable("duplicateTypeCode") String duplicateTypeCode, @Validated @RequestBody List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudent);

  @GetMapping("/all-provincial-in-flight/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Tag(name = "Sdc Duplicate", description = "Endpoint to calculate all provincial duplicates without saving them")
  Map<UUID, SdcDuplicatesByInstituteID> getInFlightProvincialDuplicates(@PathVariable("collectionID")UUID collectionID, @RequestParam(name = "instituteType") String type);

  @GetMapping("/sdcSchoolCollection/{sdcSchoolCollectionID}/provincial-duplicates")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc School Collection", description = "Endpoints to get school collection's provincial duplicates.")
  List<SdcDuplicate> getSchoolCollectionProvincialDuplicates(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @GetMapping("/sdcSchoolCollection/{sdcSchoolCollectionID}/duplicates")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc School Collection", description = "Endpoints to get school collection duplicates.")
  List<SdcSchoolCollectionStudent>  getSchoolCollectionDuplicates(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @GetMapping("/sdcSchoolCollection/{sdcSchoolCollectionID}/sdc-duplicates")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc School Collection", description = "Endpoints to get all school collection sdc duplicates.")
  List<SdcDuplicate>  getSchoolCollectionSdcDuplicates(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @GetMapping("/{sdcDuplicateID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc Duplicate", description = "Endpoints to get school collection duplicate.")
  SdcDuplicate getDuplicateByID(@PathVariable("sdcDuplicateID") UUID sdcDuplicateID);

  @PostMapping("/mark-for-review")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Sdc School Collection Student", description = "Endpoints to update PEN status on student entity.")
  ResponseEntity<Void> markPENForReview(@Validated @RequestBody SdcSchoolCollectionStudent sdcSchoolCollectionStudent);
}
