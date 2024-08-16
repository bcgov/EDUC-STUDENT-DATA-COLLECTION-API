package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum SchoolEnrolmentHeader {

    SCHOOL_YEAR("School Year"),
    DISTRICT_NUMBER("District Number"),
    SCHOOL_NUMBER("School Number"),
    SCHOOL_NAME("School Name"),
    FACILITY_TYPE("Facility Type"),
    SCHOOL_CATEGORY("School Category"),
    GRADE_RANGE("Grade Range"),
    REPORT_DATE("Report Date"),
    KIND_HT_COUNT("Kind(H/T)"),
    KIND_FT_COUNT("Kind(F/T)"),
    GRADE_01_COUNT("Grade 1"),
    GRADE_02_COUNT("Grade 2"),
    GRADE_03_COUNT("Grade 3"),
    GRADE_04_COUNT("Grade 4"),
    GRADE_05_COUNT("Grade 5"),
    GRADE_06_COUNT("Grade 6"),
    GRADE_07_COUNT("Grade 7"),
    GRADE_08_COUNT("Grade 8"),
    GRADE_09_COUNT("Grade 9"),
    GRADE_10_COUNT("Grade 10"),
    GRADE_11_COUNT("Grade 11"),
    GRADE_12_COUNT("Grade 12");

    private final String code;
    SchoolEnrolmentHeader(String code) { this.code = code; }
}
