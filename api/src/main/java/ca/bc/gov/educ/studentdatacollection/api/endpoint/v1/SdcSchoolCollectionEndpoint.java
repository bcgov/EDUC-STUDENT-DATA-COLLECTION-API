package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import static org.springframework.http.HttpStatus.CREATED;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping(URL.BASE_URL_SCHOOL_COLLECTION)
public interface SdcSchoolCollectionEndpoint {

  @GetMapping("/{sdcSchoolCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc School Collection", description = "Endpoints to get school collection entity.")
  SdcSchoolCollection getSchoolCollection(@PathVariable("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

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
  SdcSchoolCollection getSchoolCollectionBySchoolId(@PathVariable("schoolID") UUID schoolID);

  @PostMapping("/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_SCHOOL_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Tag(name = "Sdc School Collection", description = "Endpoints to get school collection entity.")
  @ResponseStatus(CREATED)
  SdcSchoolCollection createSdcSchoolCollectionByCollectionID(@Validated @RequestBody SdcSchoolCollection sdcSchoolCollection, @PathVariable("collectionID") UUID collectionID);
}
