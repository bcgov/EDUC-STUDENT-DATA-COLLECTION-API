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
import java.util.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ReportGenerationController implements ReportGenerationEndpoint {

    private final GradeEnrollmentHeadcountReportService gradeEnrollmentHeadcountReportService;
    private final CareerProgramHeadcountReportService careerProgramHeadcountReportService;
    private final FrenchProgramHeadcountReportService frenchProgramHeadcountReportService;
    private final FrenchPerSchoolHeadcountReportService frenchPerSchoolHeadcountReportService;
    private final IndigenousHeadcountReportService indigenousHeadcountReportService;
    private final EllHeadcountReportService ellHeadcountReportService;
    private final SpecialEdHeadcountReportService specialEdHeadcountReportService;
    private final AllStudentLightCollectionGenerateCsvService allStudentLightCollectionGenerateCsvService;
    private final BandOfResidenceHeadcountReportService bandOfResidenceHeadcountReportService;
    private final GradeEnrollmentHeadcountPerSchoolReportService gradeEnrollmentHeadcountPerSchoolReportService;
    private final CareerProgramHeadcountPerSchoolReportService careerProgramHeadcountPerSchoolReportService;
    private final RefugeeHeadcountPerSchoolReportService refugeeHeadcountPerSchoolReportService;

    @Override
    public DownloadableReportResponse generateSDCReport(UUID collectionID, String reportTypeCode) {
        Optional<ReportTypeCode> code = ReportTypeCode.findByValue(reportTypeCode);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        return switch (code.get()) {
            case GRADE_ENROLLMENT_HEADCOUNT -> gradeEnrollmentHeadcountReportService.generateGradeEnrollmentHeadcountReport(collectionID, false);
            case DIS_GRADE_ENROLLMENT_HEADCOUNT -> gradeEnrollmentHeadcountReportService.generateGradeEnrollmentHeadcountReport(collectionID, true);
            case DIS_GRADE_ENROLLMENT_HEADCOUNT_PER_SCHOOL -> gradeEnrollmentHeadcountPerSchoolReportService.generatePerSchoolReport(collectionID);
            case CAREER_HEADCOUNT -> careerProgramHeadcountReportService.generateCareerProgramHeadcountReport(collectionID, false);
            case DIS_CAREER_HEADCOUNT -> careerProgramHeadcountReportService.generateCareerProgramHeadcountReport(collectionID, true);
            case DIS_CAREER_HEADCOUNT_PER_SCHOOL -> careerProgramHeadcountPerSchoolReportService.generateCareerProgramHeadcountPerSchoolReport(collectionID);
            case FRENCH_HEADCOUNT -> frenchProgramHeadcountReportService.generateFrenchProgramHeadcountReport(collectionID, false);
            case DIS_FRENCH_HEADCOUNT -> frenchProgramHeadcountReportService.generateFrenchProgramHeadcountReport(collectionID, true);
            case DIS_FRENCH_HEADCOUNT_PER_SCHOOL -> frenchPerSchoolHeadcountReportService.generatePerSchoolReport(collectionID);
            case INDIGENOUS_HEADCOUNT -> indigenousHeadcountReportService.generateIndigenousHeadcountReport(collectionID);
            case BAND_RESIDENCE_HEADCOUNT -> bandOfResidenceHeadcountReportService.generateBandOfResdienceReport(collectionID);
            case DIS_REFUGEE_HEADCOUNT_PER_SCHOOL -> refugeeHeadcountPerSchoolReportService.generateRefugeePerSchoolReport(collectionID);
            case ELL_HEADCOUNT -> ellHeadcountReportService.generateEllHeadcountReport(collectionID);
            case SPECIAL_EDUCATION_HEADCOUNT -> specialEdHeadcountReportService.generateSpecialEdHeadcountReport(collectionID);
            case ALL_STUDENT_SCHOOL_CSV -> allStudentLightCollectionGenerateCsvService.generateFromSdcSchoolCollectionID(collectionID);
            case ALL_STUDENT_DIS_CSV -> allStudentLightCollectionGenerateCsvService.generateFromSdcDistrictCollectionID(collectionID);
            default -> new DownloadableReportResponse();
        };
    }


}
