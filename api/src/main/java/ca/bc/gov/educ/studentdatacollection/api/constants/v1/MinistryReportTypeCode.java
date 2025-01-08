package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum MinistryReportTypeCode {
    POSTED_DUPLICATES("posted-duplicates"),
    SCHOOL_ENROLLMENT_HEADCOUNTS("school-enrollment-headcounts"),
    SCHOOL_ADDRESS_REPORT("school-address-report"),
    INDY_SCHOOL_ENROLLMENT_HEADCOUNTS("indy-school-enrollment-headcounts"),
    FSA_REGISTRATION_REPORT("fsa-registration-report"),
    OFFSHORE_ENROLLMENT_HEADCOUNTS("offshore-enrollment-headcounts"),
    INCLUSIVE_EDUCATION_VARIANCE_HEADCOUNTS("inclusive-education-variance-headcounts"),
    INDY_INCLUSIVE_ED_ENROLLMENT_HEADCOUNTS("indy-inclusive-ed-enrollment-headcounts"),
    OFFSHORE_SPOKEN_LANGUAGE_HEADCOUNTS("offshore-languages-headcounts"),
    INDY_INCLUSIVE_ED_FUNDING_HEADCOUNTS("indy-inclusive-ed-funding-headcounts"),
    ENROLLED_HEADCOUNTS_AND_FTE_REPORT("enrolled-fte-headcounts"),
    ENROLMENT_HEADCOUNTS_AND_FTE_REPORT_FOR_OL_AND_CE_SCHOOLS("enrolment-fte-headcounts-for-ce-ol-schools"),
    INDY_FUNDING_REPORT_ALL("indy-funding-report-all"),
    INDY_FUNDING_REPORT_FUNDED("indy-funding-report-funded"),
    ONLINE_INDY_FUNDING_REPORT("online-indy-funding-report"),
    NON_GRADUATED_ADULT_INDY_FUNDING_REPORT("non-graduated-adult-indy-funding-report"),
    REFUGEE_ENROLMENT_HEADCOUNTS_AND_FTE_REPORT("refugee-enrolment-fte-headcounts")

    ;

    private final String code;
    MinistryReportTypeCode(String code) { this.code = code; }

    public static Optional<MinistryReportTypeCode> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
