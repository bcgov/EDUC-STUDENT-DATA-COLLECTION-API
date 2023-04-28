package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum FrenchPrograms {
    PROGRAMME_FRANCOPHONE("05"),
    CORE_FRENCH("08"),
    EARLY_FRENCH_IMMERSION("11"),
    LATE_FRENCH_IMMERSION("14"),
    ENGLISH_LANGUAGE_LEARNING("17")

;
    @Getter
    private final String code;
    FrenchPrograms(String code) {
        this.code = code;
    }

    public static Optional<FrenchPrograms> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }

    public static List<String> getCodes() {
        return Arrays.stream(FrenchPrograms.values()).map(FrenchPrograms::getCode).collect(Collectors.toList());
    }

    public static List<String> getFrenchProgramCodes() {
        return Arrays.stream(FrenchPrograms.values()).filter(val -> !val.code.equals("17")).map(FrenchPrograms::getCode).collect(Collectors.toList());
    }
}
