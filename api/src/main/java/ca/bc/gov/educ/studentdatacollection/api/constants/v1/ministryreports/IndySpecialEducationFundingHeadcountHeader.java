package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum IndySpecialEducationFundingHeadcountHeader {

    DISTRICT_NUMBER("District Number"),
    DISTRICT_NAME("District Name"),
    AUTHORITY_NUMBER("Authority Number"),
    AUTHORITY_NAME("Authority Name"),
    MINCODE("Mincode"),
    SCHOOL_NAME("School Name"),
    POSITIVE_CHANGE_LEVEL_1("Positive Change Level 1"),
    POSITIVE_CHANGE_LEVEL_2("Positive Change Level 2"),
    POSITIVE_CHANGE_LEVEL_3("Positive Change Level 3"),
    POSITIVE_CHANGE_SES("Positive Change SES"),
    NET_CHANGE_LEVEL_1("Net Change Level 1"),
    NET_CHANGE_LEVEL_2("Net Change Level 2"),
    NET_CHANGE_LEVEL_3("Net Change Level 3"),
    NET_CHANGE_SES("Net Change SES"),
    SEPT_LEVEL_1("September Level 1 Headcount"),
    SEPT_LEVEL_2("September Level 2 Headcount"),
    SEPT_LEVEL_3("September Level 3 Headcount"),
    SEPT_SES("September SES Headcount"),
    FEB_LEVEL_1("February Level 1 Headcount"),
    FEB_LEVEL_2("February Level 2 Headcount"),
    FEB_LEVEL_3("February Level 3 Headcount"),
    FEB_SES("February SES Headcount"),;

    private final String code;
    IndySpecialEducationFundingHeadcountHeader(String code) { this.code = code; }
}
