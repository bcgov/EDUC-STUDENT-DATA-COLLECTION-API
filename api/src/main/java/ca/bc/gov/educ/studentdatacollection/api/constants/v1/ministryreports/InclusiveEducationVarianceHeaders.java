package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum InclusiveEducationVarianceHeaders {

    DISTRICT_ID("District ID"),
    SPED_TYPE("Inclusive Education Type"),
    KIND_HT_COUNT_FEB("Kind(H/T) - February"),
    KIND_FT_COUNT_FEB("Kind(F/T) - February"),
    GRADE_01_COUNT_FEB("Grade 1 - February"),
    GRADE_02_COUNT_FEB("Grade 2 - February"),
    GRADE_03_COUNT_FEB("Grade 3 - February"),
    GRADE_04_COUNT_FEB("Grade 4 - February"),
    GRADE_05_COUNT_FEB("Grade 5 - February"),
    GRADE_06_COUNT_FEB("Grade 6 - February"),
    GRADE_07_COUNT_FEB("Grade 7 - February"),
    GRADE_EU_COUNT_FEB("Grade EU - February"),
    GRADE_08_COUNT_FEB("Grade 8 - February"),
    GRADE_09_COUNT_FEB("Grade 9 - February"),
    GRADE_10_COUNT_FEB("Grade 10 - February"),
    GRADE_11_COUNT_FEB("Grade 11 - February"),
    GRADE_12_COUNT_FEB("Grade 12 - February"),
    GRADE_SU_COUNT_FEB("Grade SU - February"),
    GRADE_GA_COUNT_FEB("Grade GA - February"),
    GRADE_HS_COUNT_FEB("Grade HS - February"),
    TOTAL_COUNT_FEB("Total - February"),
    KIND_HT_COUNT_SEPT("Kind(H/T) - September"),
    KIND_FT_COUNT_SEPT("Kind(F/T) - September"),
    GRADE_01_COUNT_SEPT("Grade 1 - September"),
    GRADE_02_COUNT_SEPT("Grade 2 - September"),
    GRADE_03_COUNT_SEPT("Grade 3 - September"),
    GRADE_04_COUNT_SEPT("Grade 4 - September"),
    GRADE_05_COUNT_SEPT("Grade 5 - September"),
    GRADE_06_COUNT_SEPT("Grade 6 - September"),
    GRADE_07_COUNT_SEPT("Grade 7 - September"),
    GRADE_EU_COUNT_SEPT("Grade EU - September"),
    GRADE_08_COUNT_SEPT("Grade 8 - September"),
    GRADE_09_COUNT_SEPT("Grade 9 - September"),
    GRADE_10_COUNT_SEPT("Grade 10 - September"),
    GRADE_11_COUNT_SEPT("Grade 11 - September"),
    GRADE_12_COUNT_SEPT("Grade 12 - September"),
    GRADE_SU_COUNT_SEPT("Grade SU - September"),
    GRADE_GA_COUNT_SEPT("Grade GA - September"),
    GRADE_HS_COUNT_SEPT("Grade HS - September"),
    TOTAL_COUNT_SEPT("Total - September");


    private final String code;
    InclusiveEducationVarianceHeaders(String code) { this.code = code; }
}
