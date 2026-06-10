package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public enum SdcSchoolStudentStatus {
    LOADED("LOADED", "Loaded"),
    ERROR("ERROR", "Error"),
    INFO_WARNING("INFOWARN", "Info Warning"),
    FUNDING_WARNING("FUNDWARN", "Funding Warning"),
    VERIFIED("VERIFIED", "Verified"),
    FIXABLE("FIXABLE", "Fixable"),
    DELETED("DELETED", "Deleted"),
    DEMOG_UPD("DEMOG_UPD", "Demog Updated"),
    COMPLETED("COMPLETED", "Completed");


    private static final Map<String, SdcSchoolStudentStatus> codeMap = new HashMap<>();

    static {
        for (SdcSchoolStudentStatus status : values()) {
            codeMap.put(status.getCode(), status);
        }
    }

    private final String code;
    private final String label;

    SdcSchoolStudentStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static SdcSchoolStudentStatus valueOfCode(String code) {
        return codeMap.get(code);
    }

    public static Optional<SdcSchoolStudentStatus> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> e.code.equalsIgnoreCase(value)).findFirst();
    }

    @Override
    public String toString() {
        return this.getCode();
    }
}
