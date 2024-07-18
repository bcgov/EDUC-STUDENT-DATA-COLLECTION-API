package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

/**
 * The enum for school category codes
 */
@Getter
public enum SchoolCategoryCodes {
    IMM_DATA("IMM_DATA"),
    CHILD_CARE("CHILD_CARE"),
    MISC("MISC"),
    PUBLIC("PUBLIC"),
    INDEPEND("INDEPEND"),
    FED_BAND("FED_BAND"),
    OFFSHORE("OFFSHORE"),
    EAR_LEARN("EAR_LEARN"),
    YUKON("YUKON"),
    POST_SEC("POST_SEC"),
    INDP_FNS("INDP_FNS");

    private final String code;
    SchoolCategoryCodes(String code) { this.code = code; }

    public static String[] getActiveSchoolCategoryCodes(){
        return new String[]{EAR_LEARN.getCode(), FED_BAND.getCode(), INDEPEND.getCode(), INDP_FNS.getCode(), OFFSHORE.getCode(), POST_SEC.getCode(), PUBLIC.getCode(), YUKON.getCode()};
    }
}
