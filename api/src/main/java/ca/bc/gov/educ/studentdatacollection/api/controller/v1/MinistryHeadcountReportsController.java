package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.MinistryReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.MinistryHeadcountReports;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.reports.AllReportsService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.reports.CSVReportService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.reports.MinistryHeadcountService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SimpleHeadcountResultsTable;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Slf4j
@RequiredArgsConstructor
public class MinistryHeadcountReportsController implements MinistryHeadcountReports {

    private final MinistryHeadcountService ministryHeadcountService;
    private final CSVReportService ministryReportsService;
    private final AllReportsService allReportsService;

    @Override
    public SimpleHeadcountResultsTable getMinistryHeadcounts(UUID collectionID, String type) {
        Optional<MinistryReportTypeCode> code = MinistryReportTypeCode.findByValue(type);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        return switch(code.get()) {
            case SCHOOL_ENROLLMENT_HEADCOUNTS -> ministryHeadcountService.getAllSchoolEnrollmentHeadcounts(collectionID);
            case SCHOOL_ADDRESS_REPORT -> ministryHeadcountService.getSchoolAddressReport(collectionID);
            case FSA_REGISTRATION_REPORT -> ministryHeadcountService.getFsaRegistrationReport(collectionID);
            case INDY_SCHOOL_ENROLLMENT_HEADCOUNTS -> ministryHeadcountService.getIndySchoolsEnrollmentHeadcounts(collectionID);
            case OFFSHORE_ENROLLMENT_HEADCOUNTS -> ministryHeadcountService.getOffshoreSchoolEnrollmentHeadcounts(collectionID);
            case INDY_INCLUSIVE_ED_ENROLLMENT_HEADCOUNTS -> ministryHeadcountService.getSpecialEducationHeadcountsForIndependentsByCollectionID(collectionID);
            case INDY_INCLUSIVE_ED_FUNDING_HEADCOUNTS -> ministryHeadcountService.getSpecialEducationFundingHeadcountsForIndependentsByCollectionID(collectionID);
            case OFFSHORE_SPOKEN_LANGUAGE_HEADCOUNTS -> ministryHeadcountService.getOffshoreSpokenLanguageHeadcounts(collectionID);
            default -> new SimpleHeadcountResultsTable();
        };
    }

    public Map<String, Long> getAllSchoolHeadcounts(UUID collectionID) {
        return ministryHeadcountService.getAllSchoolHeadcounts(collectionID);
    }

    @Override
    public DownloadableReportResponse getMinistryDownloadableReport(UUID collectionID, String type) {
        Optional<MinistryReportTypeCode> code = MinistryReportTypeCode.findByValue(type);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        return switch (code.get()) {
            case SCHOOL_ENROLLMENT_HEADCOUNTS -> ministryReportsService.generateAllSchoolsHeadcounts(collectionID);
            case SCHOOL_ADDRESS_REPORT -> ministryReportsService.generatePhysicalAddressCsv(collectionID);
            case FSA_REGISTRATION_REPORT -> ministryReportsService.generateFsaRegistrationCsv(collectionID);
            case INDY_SCHOOL_ENROLLMENT_HEADCOUNTS -> ministryReportsService.generateIndySchoolsHeadcounts(collectionID);
            case OFFSHORE_ENROLLMENT_HEADCOUNTS -> ministryReportsService.generateOffshoreSchoolsHeadcounts(collectionID);
            case INDY_INCLUSIVE_ED_ENROLLMENT_HEADCOUNTS -> ministryReportsService.generateIndySpecialEducationHeadcounts(collectionID);
            case INDY_INCLUSIVE_ED_FUNDING_HEADCOUNTS -> ministryReportsService.generateIndySpecialEducationFundingHeadcounts(collectionID);
            case OFFSHORE_SPOKEN_LANGUAGE_HEADCOUNTS -> ministryReportsService.generateOffshoreSpokenLanguageHeadcounts(collectionID);
            case ENROLLED_HEADCOUNTS_AND_FTE_REPORT -> ministryReportsService.generateEnrolledHeadcountsAndFteReport(collectionID);
            case INCLUSIVE_EDUCATION_VARIANCES_ALL -> ministryReportsService.generateInclusiveEducationVarianceReport(collectionID);
            case INDY_FUNDING_REPORT_ALL -> ministryReportsService.generateIndyFundingReport(collectionID, false,  false);
            case INDY_FUNDING_REPORT_FUNDED -> ministryReportsService.generateIndyFundingReport(collectionID, false,  true);
            case ONLINE_INDY_FUNDING_REPORT -> ministryReportsService.generateIndyFundingReport(collectionID, true,  false);
            case NON_GRADUATED_ADULT_INDY_FUNDING_REPORT -> ministryReportsService.generateIndyFundingGraduateReport(collectionID);
            case REFUGEE_ENROLMENT_HEADCOUNTS_AND_FTE_REPORT -> ministryReportsService.generateRefugeeEnrolmentHeadcountsAndFteReport(collectionID);
            case POSTED_DUPLICATES -> ministryReportsService.generatePostedDuplicatesReport(collectionID);
            case ISFS_PRELIMINARY_REPORT -> ministryReportsService.generateISFSReport(collectionID);
            case INDY_SCHOOL_GRADE_FUNDING_GROUP_ENROLLED_PROGRAMS_HEADCOUNTS -> ministryReportsService.generateIndySchoolGradeFundingGroupEnrolledProgramHeadcounts(collectionID);
            default -> new DownloadableReportResponse();
        };
    }

    @Override
    public void generateAllDistrictReportsForCollection(UUID sdcDistrictCollectionID) {
        allReportsService.generateAllDistrictReportsOnDisk(sdcDistrictCollectionID);
    }
}
