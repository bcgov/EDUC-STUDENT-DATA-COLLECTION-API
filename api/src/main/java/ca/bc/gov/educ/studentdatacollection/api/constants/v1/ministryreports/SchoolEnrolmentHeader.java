package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum SchoolEnrolmentHeader {

    SCHOOL_ENROLMENT_COLLECTION("School Enrolment Collection"),
    SCHOOL_YEAR("School Year"),
    DISTRICT_NUMBER("District Number"),
    SCHOOL_NUMBER("School Number"),
    SCHOOL_NAME("School Name"),
    FACILITY_TYPE("Facility Type"),
    SCHOOL_CATEGORY("School Category"),
    GRADE_RANGE("Grade Range"),
    REPORT_DATE("Report Date"),
    KIND_HT_COUNT("Kind(H/T) Count"),
    KIND_FT_COUNT("Kind(F/T) Count"),
    GRADE_01_COUNT("Grade 1 Count"),
    GRADE_02_COUNT("Grade 2 Count"),
    GRADE_03_COUNT("Grade 3 Count"),
    GRADE_04_COUNT("Grade 4 Count"),
    GRADE_05_COUNT("Grade 5 Count"),
    GRADE_06_COUNT("Grade 6 Count"),
    GRADE_07_COUNT("Grade 7 Count"),
    GRADE_08_COUNT("Grade 8 Count"),
    GRADE_09_COUNT("Grade 9 Count"),
    GRADE_10_COUNT("Grade 10 Count"),
    GRADE_11_COUNT("Grade 11 Count"),
    GRADE_12_COUNT("Grade 12 Count");

    private final String code;
    SchoolEnrolmentHeader(String code) { this.code = code; }
}
