package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    public static final Set<String> INDEPENDENTS = new HashSet<>(Arrays.asList(INDEPEND.getCode(), INDP_FNS.getCode()));
    SchoolCategoryCodes(String code) { this.code = code; }
}
