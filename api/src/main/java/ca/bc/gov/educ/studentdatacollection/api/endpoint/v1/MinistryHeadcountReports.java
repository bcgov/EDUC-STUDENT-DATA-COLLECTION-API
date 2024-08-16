package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SimpleHeadcountResultsTable;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RequestMapping(URL.BASE_MINISTRY_HEADCOUNTS)
public interface MinistryHeadcountReports {

    @GetMapping("/{collectionID}/{type}")
    @PreAuthorize("hasAuthority('SCOPE_READ_SDC_MINISTRY_REPORTS')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    SimpleHeadcountResultsTable getMinistryHeadcounts(@PathVariable UUID collectionID, @PathVariable(name = "type") String type);
}
