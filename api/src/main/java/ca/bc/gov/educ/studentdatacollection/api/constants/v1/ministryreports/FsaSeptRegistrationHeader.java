package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum FsaSeptRegistrationHeader {

    MINCODE("Mincode"),
    STUDENT_PEN("Student PEN"),
    ENROLLED_GRADE("Enrolled Grade"),
    LEGAL_FIRST_NAME("Legal First Name"),
    LEGAL_LAST_NAME("Legal Last Name");

    private final String code;
    FsaSeptRegistrationHeader(String code) { this.code = code; }
}
