package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

/**
 * The enum for school's facility type codes
 */
@Getter
public enum HeadcountReportTypeCodes {
    ENROLLMENT("enrollment"),
    FRENCH("french"),
    ELL("ell"),
    CAREER("career"),
    INDIGENOUS("indigenous"),
    SPECIAL_ED("special-ed");

    private final String code;
    HeadcountReportTypeCodes(String code) { this.code = code; }
}
