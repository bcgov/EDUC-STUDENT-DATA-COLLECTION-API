package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicate;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicatesByInstituteID;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping(URL.BASE_URL_DUPLICATE)
public interface SdcDuplicateEndpoint {

  @PostMapping("/{sdcDuplicateID}/type/{duplicateTypeCode}")
  @PreAuthorize("hasAuthority('SCOPE_WRITE_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional
  @Tag(name = "Sdc Duplicate", description = "Endpoints to edit and resolve sdc duplicates.")
  SdcDuplicate updateStudentAndResolveDuplicates(@PathVariable("duplicateTypeCode") String duplicateTypeCode, @PathVariable("sdcDuplicateID") UUID sdcDuplicateID, @Validated @RequestBody List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudent);

  @GetMapping("/all-provincial-in-flight/{collectionID}")
  @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION_STUDENT')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Tag(name = "Sdc Duplicate", description = "Endpoint to calculate all provincial duplicates without saving them")
  Map<UUID, SdcDuplicatesByInstituteID> getInFlightProvincialDuplicates(@PathVariable("collectionID")UUID collectionID, @RequestParam(name = "instituteType") String type);
}
