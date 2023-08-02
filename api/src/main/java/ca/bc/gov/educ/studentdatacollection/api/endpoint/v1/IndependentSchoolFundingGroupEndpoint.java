package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.IndependentSchoolFundingGroupSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
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

@RequestMapping(URL.BASE_URL_SCHOOL_FUNDING)
public interface IndependentSchoolFundingGroupEndpoint {

  @GetMapping("/{schoolFundingGroupID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SCHOOL_FUNDING_GROUP')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "IndependentSchoolFundingGroup Entity", description = "Endpoints for independent school group funding entity.")
  @Schema(name = "IndependentSchoolFundingGroup", implementation = IndependentSchoolFundingGroup.class)
  IndependentSchoolFundingGroup getIndependentSchoolFundingGroup(@PathVariable("schoolFundingGroupID") UUID schoolFundingGroupID);

  @GetMapping("/search/{schoolID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SCHOOL_FUNDING_GROUP')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "IndependentSchoolFundingGroup Entity", description = "Endpoints for independent school group funding entity.")
  @Schema(name = "IndependentSchoolFundingGroup", implementation = IndependentSchoolFundingGroup.class)
  List<IndependentSchoolFundingGroup> getIndependentSchoolFundingGroups(@PathVariable("schoolID") UUID schoolID);

  @GetMapping("/snapshot/{schoolID}/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SCHOOL_FUNDING_GROUP')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "IndependentSchoolFundingGroupSnapshot Entity", description = "Endpoints for independent school group funding entity.")
  @Schema(name = "IndependentSchoolFundingGroupSnapshot", implementation = IndependentSchoolFundingGroupSnapshot.class)
  List<IndependentSchoolFundingGroupSnapshot> getIndependentSchoolFundingGroupSnapshot(@PathVariable("schoolID") UUID schoolID, @PathVariable("collectionID") UUID collectionID);

  @PostMapping()
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SCHOOL_FUNDING_GROUP')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Tag(name = "IndependentSchoolFundingGroup Entity", description = "Endpoints for independent school group funding entity.")
  @Schema(name = "IndependentSchoolFundingGroup", implementation = IndependentSchoolFundingGroup.class)
  @ResponseStatus(CREATED)
  IndependentSchoolFundingGroup createIndependentSchoolFundingGroup(@Validated @RequestBody IndependentSchoolFundingGroup fundingGroup) throws JsonProcessingException;

  @PutMapping("/{schoolFundingGroupID}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SCHOOL_FUNDING_GROUP')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "IndependentSchoolFundingGroup Entity", description = "Endpoints for independent school group funding entity.")
  @Schema(name = "IndependentSchoolFundingGroup", implementation = IndependentSchoolFundingGroup.class)
  IndependentSchoolFundingGroup updateIndependentSchoolFundingGroup(@PathVariable("sdcSchoolCollectionStudentID") UUID schoolFundingGroupID, @Validated @RequestBody IndependentSchoolFundingGroup independentSchoolFundingGroup);

  @DeleteMapping("/{schoolFundingGroupID}")
  @PreAuthorize("hasAuthority('SCOPE_DELETE_SCHOOL_FUNDING_GROUP')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Tag(name = "IndependentSchoolFundingGroup Entity", description = "Endpoints for independent school group funding entity.")
  @Schema(name = "IndependentSchoolFundingGroup", implementation = IndependentSchoolFundingGroup.class)
  @ResponseStatus(NO_CONTENT)
  ResponseEntity<Void> deleteIndependentSchoolFundingGroup(@PathVariable UUID schoolFundingGroupID);
}
