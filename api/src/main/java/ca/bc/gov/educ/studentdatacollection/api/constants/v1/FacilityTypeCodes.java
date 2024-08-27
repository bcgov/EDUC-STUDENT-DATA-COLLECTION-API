package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * The enum for school's facility type codes
 */
@Getter
public enum FacilityTypeCodes {
    PROVINCIAL("PROVINCIAL"),
    DIST_CONT("DIST_CONT"),
    ELEC_DELIV("ELEC_DELIV"),
    STANDARD("STANDARD"),
    CONT_ED("CONT_ED"),
    DIST_LEARN("DIST_LEARN"),
    ALT_PROGS("ALT_PROGS"),
    STRONG_CEN("STRONG_CEN"),
    STRONG_OUT("STRONG_OUT"),
    SHORT_PRP("SHORT_PRP"),
    LONG_PRP("LONG_PRP"),
    SUMMER("SUMMER"),
    YOUTH("YOUTH"),
    DISTONLINE("DISTONLINE"),
    POST_SEC("POST_SEC"),
    JUSTB4PRO("JUSTB4PRO");

    private final String code;
    FacilityTypeCodes(String code) { this.code = code; }

    public static String[] getFacilityCodesWithoutOLAndCE(){
        return new String[]{ALT_PROGS.getCode(), JUSTB4PRO.getCode(), LONG_PRP.getCode(), POST_SEC.getCode(), SHORT_PRP.getCode(), STANDARD.getCode(), STRONG_CEN.getCode(), STRONG_OUT.getCode(), SUMMER.getCode(), YOUTH.getCode()};
    }

    public static List<String> getOnlineFacilityTypeCodes() {
        List<String> codes = new ArrayList<>();
        codes.add(DIST_LEARN.getCode());
        codes.add(DISTONLINE.getCode());
        return codes;
    }
}
