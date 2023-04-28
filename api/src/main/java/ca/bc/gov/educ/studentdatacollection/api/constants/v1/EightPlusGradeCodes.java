package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum EightPlusGradeCodes {
    GRADE08("08"),
    GRADE09("09"),
    GRADE10("10"),
    GRADE11("11"),
    GRADE12("12"),
    SECONDARY_UNGRADED("SU"),
    GRADUATED_ADULT("GA")

;
    @Getter
    private final String code;
    EightPlusGradeCodes(String code) {
        this.code = code;
    }

    public static Optional<EightPlusGradeCodes> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
