package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.UnsubmitSdcSchoolCollection;
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
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RequestMapping(URL.BASE_URL_SCHOOL_COLLECTION)
public interface SdcSchoolCollectionEndpoint {

  @GetMapping("/{sdcSchoolCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc School Collection", description = "Endpoints to get school collection entity.")
  SdcSchoolCollection getSchoolCollection(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @GetMapping("/{sdcSchoolCollectionID}/duplicates")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc School Collection", description = "Endpoints to get school collection duplicates.")
  List<SdcSchoolCollectionStudent>  getSchoolCollectionDuplicates(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @PutMapping("/{sdcSchoolCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Sdc School Collection", description = "Endpoints to get school collection entity.")
  @Schema(name = "SdcSchoolCollection", implementation = SdcSchoolCollection.class)
  SdcSchoolCollection updateSchoolCollection(@Validated @RequestBody SdcSchoolCollection sdcSchoolCollection, @PathVariable UUID sdcSchoolCollectionID);

  @GetMapping("/search/{schoolID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc School Collection", description = "Endpoints to get school collection entity.")
  SdcSchoolCollection getActiveSchoolCollectionBySchoolId(@PathVariable("schoolID") UUID schoolID);

  @PostMapping("/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_SCHOOL_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Tag(name = "Sdc School Collection", description = "Endpoints to get school collection entity.")
  @ResponseStatus(CREATED)
  SdcSchoolCollection createSdcSchoolCollectionByCollectionID(@Validated @RequestBody SdcSchoolCollection sdcSchoolCollection, @PathVariable("collectionID") UUID collectionID);

  @DeleteMapping("/{sdcSchoolCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_DELETE_SDC_SCHOOL_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @ResponseStatus(NO_CONTENT)
  ResponseEntity<Void> deleteSdcSchoolCollection(@PathVariable UUID sdcSchoolCollectionID);

  @GetMapping("/searchAll")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc School Collection", description = "Endpoints to get school collection entity.")
  List<SdcSchoolCollection> getAllSchoolCollections(@RequestParam(name = "schoolID", defaultValue = "") UUID schoolID,
                                                              @RequestParam(name = "sdcDistrictCollectionID", defaultValue = "") UUID sdcDistrictCollectionID);

  @PostMapping("/unsubmit")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_DISTRICT_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Sdc School Collection", description = "Endpoints to unsubmit school collection entity.")
  @Schema(name = "SdcSchoolCollection", implementation = SdcSchoolCollection.class)
  SdcSchoolCollection unsubmitSchoolCollection(@RequestBody UnsubmitSdcSchoolCollection unsubmitData);

}
