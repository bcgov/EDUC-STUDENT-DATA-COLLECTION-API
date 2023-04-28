package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum AllowedAdultGradeCodes {
    GRADE10("10"),
    GRADE11("11"),
    GRADE12("12"),
    SECONDARY_UNGRADED("SU"),
    GRADUATED_ADULT("GA")

;
    @Getter
    private final String code;
    AllowedAdultGradeCodes(String code) {
        this.code = code;
    }

    public static Optional<AllowedAdultGradeCodes> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }

    public static List<String> getCodes() {
        return Arrays.stream(AllowedAdultGradeCodes.values()).map(AllowedAdultGradeCodes::getCode).collect(Collectors.toList());
    }
}
