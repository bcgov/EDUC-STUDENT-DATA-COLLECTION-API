package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum FsaFebRegistrationHeader {

    MINCODE("Mincode"),
    STUDENT_PEN("Student PEN"),
    NEXT_YEAR_GRADE("Grade"),
    LEGAL_FIRST_NAME("Legal First Name"),
    LEGAL_LAST_NAME("Legal Last Name");

    private final String code;
    FsaFebRegistrationHeader(String code) { this.code = code; }
}
