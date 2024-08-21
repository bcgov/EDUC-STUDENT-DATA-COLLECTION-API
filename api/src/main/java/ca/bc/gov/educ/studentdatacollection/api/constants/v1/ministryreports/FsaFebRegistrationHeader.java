package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum FsaFebRegistrationHeader {

    STUDENT_PEN("Student PEN"),
    DISTRICT_NUMBER("District Number"),
    SCHOOL_NUMBER("School Number"),
    NEXT_YEAR_GRADE("Next Year Grade"),
    LOCAL_ID("Student Local Id"),
    LEGAL_FIRST_NAME("Legal First Name"),
    LEGAL_LAST_NAME("Legal Last Name"),
    ;

    private final String code;
    FsaFebRegistrationHeader(String code) { this.code = code; }
}
