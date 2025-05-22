package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.IndependentSchoolFundingGroupSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@RequestMapping(URL.BASE_URL_SCHOOL_FUNDING)
public interface IndependentSchoolFundingGroupSnapshotEndpoint {

  @GetMapping("/{schoolID}/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SCHOOL_FUNDING_GROUP_SNAPSHOT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "IndependentSchoolFundingGroupSnapshot Entity", description = "Endpoints for independent school group snapshot funding entity.")
  @Schema(name = "IndependentSchoolFundingGroupSnapshot", implementation = IndependentSchoolFundingGroupSnapshot.class)
  List<IndependentSchoolFundingGroupSnapshot> getIndependentSchoolFundingGroupSnapshot(@PathVariable("schoolID") UUID schoolID, @PathVariable("collectionID") UUID collectionID);

  @GetMapping("/all/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SCHOOL_FUNDING_GROUP_SNAPSHOT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "IndependentSchoolFundingGroupSnapshot Entity", description = "Endpoints for independent school group snapshot funding entity.")
  @Schema(name = "IndependentSchoolFundingGroupSnapshot", implementation = IndependentSchoolFundingGroupSnapshot.class)
  List<IndependentSchoolFundingGroupSnapshot> getAllIndependentSchoolFundingGroupSnapshot(@PathVariable("collectionID") UUID collectionID);
}
