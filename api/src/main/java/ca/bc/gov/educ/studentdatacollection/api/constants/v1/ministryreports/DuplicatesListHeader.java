package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum DuplicatesListHeader {

    ASSIGNED_PEN("Assigned PEN"),
    DISTRICT("District"),
    SCHOOL("School"),
    MINCODE("Mincode"),
    LOCAL_ID("Local ID"),
    BIRTH_DATE("Birth Date"),
    LEGAL_LAST_NAME("Legal Last Name"),
    LEGAL_FIRST_NAME("Legal Given Name"),
    LEGAL_MIDDLE_NAME("Legal Middle Name"),
    USUAL_LAST_NAME("Usual Last Name"),
    USUAL_FIRST_NAME("Usual Given Name"),
    USUAL_MIDDLE_NAME("Usual Middle Name"),
    GENDER("Gender"),
    POSTAL_CODE("Postal Code"),
    IS_ADULT("Is Adult"),
    IS_GRAD("Is Graduated"),
    GRADE("Grade"),
    FUNDING_CODE("Funding Code"),
    COURSES_FOR_GRAD("Courses For Grad"),
    SUPPORT_BLOCKS("Support Blocks"),
    YEARS_IN_ELL("Years in ELL"),
    CAREER_CODE("Career Code"),
    INDIGENOUS_ANCESTRY("Indigenous Ancestry"),
    BAND_CODE("Band Code"),
    INCLUSIVE_EDUCATION_CATEGORY("Inclusive Education Category"),
    FTE("FTE"),
    DUPLICATE_TYPE("Duplicate Type");

    private final String code;
    DuplicatesListHeader(String code) { this.code = code; }

    public static String[] getAllValuesAsStringArray(){
        return Arrays.stream(DuplicatesListHeader.values()).map(DuplicatesListHeader::getCode).toArray(String[]::new);
    }
}
