package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentHeadcounts;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RequestMapping(URL.BASE_DISTRICT_HEADCOUNTS)
public interface SdcDistrictCollectionHeadcountReports {

    @GetMapping("/{sdcDistrictCollectionID}")
    @PreAuthorize("hasAuthority('SCOPE_READ_SDC_SCHOOL_COLLECTION_STUDENT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
    SdcSchoolCollectionStudentHeadcounts getSdcSchoolCollectionStudentHeadcounts(@PathVariable("sdcDistrictCollectionID") UUID sdcDistrictCollectionID,
                                                                                 @RequestParam(name = "type") String type,
                                                                                 @RequestParam(name = "compare", defaultValue = "false") boolean compare);
}
