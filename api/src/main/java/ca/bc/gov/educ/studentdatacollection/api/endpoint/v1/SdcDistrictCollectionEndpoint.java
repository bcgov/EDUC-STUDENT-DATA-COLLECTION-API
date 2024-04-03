package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDistrictCollection;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping(URL.BASE_URL_DISTRICT_COLLECTION)
public interface SdcDistrictCollectionEndpoint {

  @GetMapping("/{sdcDistrictCollectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_DISTRICT_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc District Collection", description = "Endpoints to get district collection entity.")
  SdcDistrictCollection getDistrictCollection(@PathVariable("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @GetMapping("/search/{districtID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_DISTRICT_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Sdc District Collection", description = "Endpoints to get active district collection entity by district id.")
  SdcDistrictCollection getActiveDistrictCollectionByDistrictId(@PathVariable("districtID") UUID districtID);

}
