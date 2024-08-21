package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum FsaSeptRegistrationHeader {

    STUDENT_PEN("Student PEN"),
    DISTRICT_NUMBER("District Number"),
    SCHOOL_NUMBER("School Number"),
    ENROLLED_GRADE("Enrolled Grade"),
    LOCAL_ID("Student Local Id"),
    LEGAL_FIRST_NAME("Legal First Name"),
    LEGAL_LAST_NAME("Legal Last Name"),
    ;

    private final String code;
    FsaSeptRegistrationHeader(String code) { this.code = code; }
}
