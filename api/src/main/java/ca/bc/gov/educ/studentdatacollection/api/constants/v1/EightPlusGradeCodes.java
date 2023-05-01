package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public static List<String> getNonGraduateGrades() {
        return Arrays.stream(EightPlusGradeCodes.values()).filter(val -> !val.code.equals("GA")).map(EightPlusGradeCodes::getCode).collect(Collectors.toList());
    }
}
