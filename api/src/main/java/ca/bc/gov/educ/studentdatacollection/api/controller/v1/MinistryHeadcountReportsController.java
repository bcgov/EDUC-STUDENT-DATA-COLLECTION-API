package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.MinistryReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.MinistryHeadcountReports;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.MinistryHeadcountService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Slf4j
@RequiredArgsConstructor
public class MinistryHeadcountReportsController implements MinistryHeadcountReports {

    private final MinistryHeadcountService ministryHeadcountService;

    @Override
    public HeadcountResultsTable getMinistryHeadcounts(UUID collectionID, String type) {
        Optional<MinistryReportTypeCode> code = MinistryReportTypeCode.findByValue(type);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        return switch(code.get()) {
            case SCHOOL_ENROLLMENT_HEADCOUNTS -> ministryHeadcountService.getAllSchoolEnrollmentHeadcounts(collectionID);
            default -> new HeadcountResultsTable();
        };
    }
}
