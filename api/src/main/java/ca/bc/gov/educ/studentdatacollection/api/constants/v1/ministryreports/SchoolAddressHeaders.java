package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum SchoolAddressHeaders {

    MINCODE("Mincode"),
    SCHOOL_NAME("School Name"),
    ADDRESS_LINE1("Physical Address Line 1 "),
    ADDRESS_LINE2("Physical Address Line 2 "),
    CITY("Physical City "),
    PROVINCE("Physical Province"),
    POSTAL("Physical Postal ");

    private final String code;
    SchoolAddressHeaders(String code) { this.code = code; }
}
