package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum AllStudentsHeaders {

    DISTRICT("District"),
    SCHOOL("School"),
    MINCODE("Mincode"),
    SUBMITTED_PEN("Submitted PEN"),
    ASSIGNED_PEN("Assigned PEN"),
    LEGAL_LAST_NAME("Legal Last Name"),
    LEGAL_FIRST_NAME("Legal Given Name"),
    LEGAL_MIDDLE_NAME("Legal Middle Name"),
    USUAL_LAST_NAME("Usual Last Name"),
    USUAL_FIRST_NAME("Usual Given Name"),
    USUAL_MIDDLE_NAME("Usual Middle Name"),
    BIRTH_DATE("Birth Date"),
    GENDER("Gender"),
    POSTAL_CODE("Postal Code"),
    LOCAL_ID("Local ID"),
    GRADE("Grade"),
    FTE("FTE"),
    IS_ADULT("Adult"),
    IS_GRADUATE("Graduate"),
    OTHER_COURSES("Other Courses"),
    FUNDING_CODE("Funding Code"),
    LANGUAGE_CODE("Language Spoken"),
    SUPPORT_BLOCKS("Support Blocks"),
    NUMBER_OF_COURSES("Number of Courses"),
    ENROLLED_PROGRAM_CODES("Enrolled Program Codes"),
    YEARS_IN_ELL("Years In ELL"),
    CAREER_CODE("Career Code"),
    INDIGENOUS_ANCESTRY("Indigenous Ancestry"),
    BAND_CODE("Band Code"),
    INCLUSIVE_EDUCATION_CATEGORY("Inclusive Education Category");


    private final String code;
    AllStudentsHeaders(String code) { this.code = code; }
}
