package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum IndigenousPrograms {
    ABORIGINAL_LANGUAGE_AND_CULTURE("29"),
    ABORIGINAL_SUPPORT_SERVICES("33"),
    OTHER_APPROVED_ABORIGINAL_PROGRAM("36")

;
    @Getter
    private final String code;
    IndigenousPrograms(String code) {
        this.code = code;
    }

    public static Optional<IndigenousPrograms> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }

    public static List<String> getCodes() {
        return Arrays.stream(IndigenousPrograms.values()).map(IndigenousPrograms::getCode).collect(Collectors.toList());
    }
}
