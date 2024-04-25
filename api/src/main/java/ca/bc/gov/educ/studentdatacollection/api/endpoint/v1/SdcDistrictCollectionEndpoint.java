package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.MonitorSdcSchoolCollectionsResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDistrictCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicate;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolFileSummary;
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

@RequestMapping(URL.BASE_URL_DISTRICT_COLLECTION)
public interface SdcDistrictCollectionEndpoint {

  @GetMapping("/{sdcDistrictCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_DISTRICT_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc District Collection", description = "Endpoints to get district collection entity.")
  SdcDistrictCollection getDistrictCollection(@PathVariable("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @GetMapping("/{sdcDistrictCollectionID}/in-district-duplicates")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc District Collection", description = "Endpoints to get in district collection duplicates.")
  List<SdcDuplicate> getDistrictCollectionDuplicates(@PathVariable("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @GetMapping("/search/{districtID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_DISTRICT_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc District Collection", description = "Endpoints to get active district collection entity by district id.")
  SdcDistrictCollection getActiveDistrictCollectionByDistrictId(@PathVariable("districtID") UUID districtID);

  @PostMapping("/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_DISTRICT_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Tag(name = "Sdc District Collection", description = "Endpoint to create sdc district collection entity.")
  @ResponseStatus(CREATED)
  SdcDistrictCollection createSdcDistrictCollectionByCollectionID(@Validated @RequestBody SdcDistrictCollection sdcDistrictCollection, @PathVariable("collectionID") UUID collectionID);

  @DeleteMapping("/{sdcDistrictCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_DELETE_SDC_DISTRICT_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @ResponseStatus(NO_CONTENT)
  ResponseEntity<Void> deleteSdcDistrictCollection(@PathVariable UUID sdcDistrictCollectionID);

  @GetMapping("/{sdcDistrictCollectionID}/fileProgress")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_DISTRICT_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc District Collection", description = "Endpoint to get the status of all school collections within district currently being processed")
  List<SdcSchoolFileSummary> getSchoolCollectionsInProgress(@PathVariable(name = "sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @GetMapping("/{sdcDistrictCollectionID}/monitorSdcSchoolCollections")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_DISTRICT_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc District Collection", description = "Endpoint to get monitoring objects for all sdc school collections in the sdc district collection.")
  MonitorSdcSchoolCollectionsResponse getMonitorSdcSchoolCollectionResponse(@PathVariable("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @PutMapping("/{sdcDistrictCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_DISTRICT_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Sdc District Collection", description = "Endpoints to get district collection entity.")
  @Schema(name = "SdcDistrictCollection", implementation = SdcDistrictCollection.class)
  SdcDistrictCollection updateDistrictCollection(@Validated @RequestBody SdcDistrictCollection sdcDistrictCollection, @PathVariable UUID sdcDistrictCollectionID);

}
