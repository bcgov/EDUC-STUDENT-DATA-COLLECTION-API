package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum CareerPrograms {
    CAREER_PREPARATION("40"),
    CO_OP("41"),
    APPRENTICESHIP("42"),
    CAREER_TECHNICAL_CENTRE("43"),

;
    @Getter
    private final String code;
    CareerPrograms(String code) {
        this.code = code;
    }

    public static Optional<CareerPrograms> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }

    public static List<String> getCodes() {
        return Arrays.stream(CareerPrograms.values()).map(CareerPrograms::getCode).collect(Collectors.toList());
    }
}
