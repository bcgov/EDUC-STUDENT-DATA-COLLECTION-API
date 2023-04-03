package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import static org.springframework.http.HttpStatus.CREATED;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping(URL.BASE_URL_COLLECTION)
public interface CollectionEndpoint {

//  TODO ADD SCOPES TO CONFIG!
  @GetMapping
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  @Schema(name = "Collection", implementation = Collection.class)
  List<Collection> getAllCollections();

  @GetMapping("/{schoolID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  @Schema(name = "Collection", implementation = Collection.class)
  Collection getCollectionBySchoolId(@PathVariable("schoolID") UUID schoolID);

  @PostMapping()
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  @Schema(name = "Collection", implementation = Collection.class)
  @ResponseStatus(CREATED)
  Collection createCollection(@Validated @RequestBody Collection collection) throws JsonProcessingException;

  @DeleteMapping("/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_DELETE_SDC_COLLECTION')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Tag(name = "Collection Entity", description = "Endpoints for collection entity.")
  @Schema(name = "Collection", implementation = Collection.class)
  ResponseEntity<Void> deleteCollection(@PathVariable UUID collectionID);
}
