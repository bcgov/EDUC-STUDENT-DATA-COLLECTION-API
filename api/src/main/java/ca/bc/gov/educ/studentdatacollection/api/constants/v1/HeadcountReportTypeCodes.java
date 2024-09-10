package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

/**
 * The enum for school's facility type codes
 */
@Getter
public enum HeadcountReportTypeCodes {
    ENROLLMENT("enrollment"),
    FRENCH("french"),
    FRENCH_PER_SCHOOL("french-per-school"),
    ELL("ell"),
    CAREER("career"),
    INDIGENOUS("indigenous"),
    INDIGENOUS_PER_SCHOOL("indigenous-per-school"),
    SPECIAL_ED("special-ed"),
    SPECIAL_ED_PER_SCHOOL("special-ed-per-school"),
    SPECIAL_ED_CAT_PER_SCHOOL("special-ed-cat-per-school"),
    BAND_CODES("band-codes"),
    BAND_CODES_PER_SCHOOL("band-codes-per-school"),
    GRADE_ENROLLMENT("grade-enrollment"),
    CAREER_PER_SCHOOL("career-per-school"),
    ELL_PER_SCHOOL("ell-per-school"),
    REFUGEE_PER_SCHOOL("refugee-per-school"),
    ZERO_FTE_SUMMARY("zero-fte-summary"),
    INCLUSIVE_EDUCATION_VARIANCE("INCLUSIVE_EDUCATION_VARIANCE");

    private final String code;
    HeadcountReportTypeCodes(String code) { this.code = code; }
}
