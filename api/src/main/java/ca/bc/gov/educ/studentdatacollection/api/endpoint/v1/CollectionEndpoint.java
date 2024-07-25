package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.CollectionSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
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

import static org.springframework.http.HttpStatus.*;

@RequestMapping(URL.BASE_URL_COLLECTION)
public interface CollectionEndpoint {

  @GetMapping("/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  @Schema(name = "Collection", implementation = Collection.class)
  Collection getCollection(@PathVariable("collectionID") UUID collectionID);

  @GetMapping("/search/{createUser}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  @Schema(name = "Collection", implementation = Collection.class)
  List<Collection> getCollections(@PathVariable("createUser") String createUser);

  @GetMapping("/active")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value={@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  @Schema(name = "Collection", implementation = Collection.class)
  Collection getActiveCollection();

  @PostMapping()
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  @Schema(name = "Collection", implementation = Collection.class)
  @ResponseStatus(CREATED)
  Collection createCollection(@Validated @RequestBody Collection collection) throws JsonProcessingException;

  @DeleteMapping("/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_DELETE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  @Schema(name = "Collection", implementation = Collection.class)
  @ResponseStatus(NO_CONTENT)
  ResponseEntity<Void> deleteCollection(@PathVariable UUID collectionID);

  @PostMapping("/{collectionID}/in-province-duplicates")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  ResponseEntity<Void> generateProvinceDuplicates(@PathVariable("collectionID") UUID collectionID);

  @GetMapping("/{collectionID}/in-province-duplicates")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  @Transactional
  List<SdcDuplicate> getProvinceDuplicates(@PathVariable("collectionID") UUID collectionID);

  @GetMapping("/{collectionID}/monitorSdcDistrictCollections")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Collection Entity", description = "Endpoint to get monitoring objects for all sdc district collections in the collection.")
  List<MonitorSdcDistrictCollection> getMonitorSdcDistrictCollectionResponse(@PathVariable("collectionID") UUID collectionID);

  @GetMapping("/{collectionID}/monitorIndySdcSchoolCollections")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Collection Entity", description = "Endpoint to get monitoring objects for all indy sdc school collections in the collection.")
  MonitorIndySdcSchoolCollectionsResponse getMonitorIndySdcSchoolCollectionResponse(@PathVariable("collectionID") UUID collectionID);

  @GetMapping("/{collectionID}/duplicates")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Collection Entity", description = "Endpoints to find duplicates in collection.")
  List<String> findDuplicatesInCollection(@PathVariable("collectionID") UUID collectionID, @RequestParam("matchedAssignedIDs") List<String> matchedAssignedIDs);

  @PostMapping("/close-collection")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "202", description = "ACCEPTED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST."), @ApiResponse(responseCode = "409", description = "CONFLICT.")})
  @ResponseStatus(ACCEPTED)
  ResponseEntity<String> closeCollection(@Validated @RequestBody CollectionSagaData collectionSagaData) throws JsonProcessingException;

  @PostMapping("/{collectionID}/resolve-duplicates")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  ResponseEntity<Void> resolveRemainingDuplicates(@PathVariable("collectionID") UUID collectionID);
}