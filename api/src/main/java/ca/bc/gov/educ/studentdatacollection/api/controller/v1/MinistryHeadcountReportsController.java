package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.MinistryReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.MinistryHeadcountReports;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.reports.CSVReportService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.reports.MinistryHeadcountService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SimpleHeadcountResultsTable;
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
public class MinistryHeadcountReportsController implements MinistryHeadcountReports {

    private final MinistryHeadcountService ministryHeadcountService;
    private final CSVReportService ministryReportsService;

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
            case OFFSHORE_SPOKEN_LANGUAGE_HEADCOUNTS -> ministryHeadcountService.getOffshoreSpokenLanguageHeadcounts(collectionID);
            case INDY_INCLUSIVE_ED_FUNDING_HEADCOUNTS -> ministryHeadcountService.getSpecialEducationFundingHeadcountsForIndependentsByCollectionID(collectionID);
            default -> new SimpleHeadcountResultsTable();
        };
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
            case OFFSHORE_SPOKEN_LANGUAGE_HEADCOUNTS -> ministryReportsService.generateOffshoreSpokenLanguageHeadcounts(collectionID);
            case INDY_INCLUSIVE_ED_FUNDING_HEADCOUNTS -> ministryReportsService.generateIndySpecialEducationFundingHeadcounts(collectionID);
            case ENROLLED_HEADCOUNTS_AND_FTE_REPORT -> ministryReportsService.generateEnrolledHeadcountsAndFteReport(collectionID);
            case ENROLMENT_HEADCOUNTS_AND_FTE_REPORT_FOR_OL_AND_CE_SCHOOLS -> ministryReportsService.generateEnrolmentHeadcountsAndFteReportForCEAndOLSchools(collectionID);
            case INDY_FUNDING_REPORT -> ministryReportsService.generateIndyFundingReport(collectionID, false, false);
            case ONLINE_INDY_FUNDING_REPORT -> ministryReportsService.generateIndyFundingReport(collectionID, true, false);
            case NON_GRADUATED_ADULT_INDY_FUNDING_REPORT -> ministryReportsService.generateIndyFundingReport(collectionID, false, true);
            case REFUGEE_ENROLMENT_HEADCOUNTS_AND_FTE_REPORT -> ministryReportsService.generateRefugeeEnrolmentHeadcountsAndFteReport(collectionID);
            case POSTED_DUPLICATES -> ministryReportsService.generatePostedDuplicatesReport(collectionID);
            default -> new DownloadableReportResponse();
        };
    }


}
