package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.ReportGenerationEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.reports.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
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
public class ReportGenerationController implements ReportGenerationEndpoint {

    private final GradeEnrollmentHeadcountReportService gradeEnrollmentHeadcountReportService;
    private final CareerProgramHeadcountReportService careerProgramHeadcountReportService;
    private final FrenchProgramHeadcountReportService frenchProgramHeadcountReportService;
    private final IndigenousHeadcountReportService indigenousHeadcountReportService;
    private final EllHeadcountReportService ellHeadcountReportService;
    private final SpecialEdHeadcountReportService specialEdHeadcountReportService;

    @Override
    public DownloadableReportResponse generateSDCReport(UUID collectionID, String reportTypeCode) {
        Optional<ReportTypeCode> code = ReportTypeCode.findByValue(reportTypeCode);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        switch(code.get()){
            case GRADE_ENROLLMENT_HEADCOUNT:
                return gradeEnrollmentHeadcountReportService.generateGradeEnrollmentHeadcountReport(collectionID);
            case CAREER_HEADCOUNT:
                return careerProgramHeadcountReportService.generateCareerProgramHeadcountReport(collectionID);
            case FRENCH_HEADCOUNT:
                return frenchProgramHeadcountReportService.generateFrenchProgramHeadcountReport(collectionID);
            case INDIGENOUS_HEADCOUNT:
                return indigenousHeadcountReportService.generateIndigenousHeadcountReport(collectionID);
            case ELL_HEADCOUNT:
                return ellHeadcountReportService.generateEllHeadcountReport(collectionID);
            case SPECIAL_EDUCATION_HEADCOUNT:
                return specialEdHeadcountReportService.generateSpecialEdHeadcountReport(collectionID);
            default:
                return new DownloadableReportResponse();
        }
    }
}
