package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum EnrolledProgramCodes {
    PROGRAMME_FRANCOPHONE("05"),
    CORE_FRENCH("08"),
    EARLY_FRENCH_IMMERSION("11"),
    LATE_FRENCH_IMMERSION("14"),
    ENGLISH_LANGUAGE_LEARNING("17"),
    MANDARIN_CHINESE("20"),
    JAPANESE("23"),
    INDIAN_EDUCATION("26"),
    ABORIGINAL_LANGUAGE("29"),
    NATIVE_LANGUAGE("31"),
    ABORIGINAL_SUPPORT("33"),
    NATIVE_ALTERNATIVE("35"),
    OTHER_APPROVED_NATIVE("36"),
    CAREER_PREPARATION("40"),
    COOP("41"),
    YOUTH_WORK_IN_TRADES("42"),
    CAREER_TECHNICAL_CENTER("43"),
    ADVANCED_PLACEMENT("AD"),
    BACCALAUREATE_C("BC"),
    BACCALAUREATE_D("BD");

    @Getter
    private final String code;
    EnrolledProgramCodes(String code) {
        this.code = code;
    }

    public static Optional<EnrolledProgramCodes> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }

    public static List<String> getCodes() {
        return Arrays.stream(EnrolledProgramCodes.values()).map(EnrolledProgramCodes::getCode).collect(Collectors.toList());
    }

    public static List<String> getFrenchProgramCodes() {
        List<String> codes = new ArrayList<>();
        codes.add(PROGRAMME_FRANCOPHONE.getCode());
        codes.add(CORE_FRENCH.getCode());
        codes.add(EARLY_FRENCH_IMMERSION.getCode());
        codes.add(LATE_FRENCH_IMMERSION.getCode());
        return codes;
    }

    public static List<String> getFrenchProgramCodesWithEll() {
        List<String> codes = new ArrayList<>();
        codes.add(PROGRAMME_FRANCOPHONE.getCode());
        codes.add(CORE_FRENCH.getCode());
        codes.add(EARLY_FRENCH_IMMERSION.getCode());
        codes.add(LATE_FRENCH_IMMERSION.getCode());
        codes.add(ENGLISH_LANGUAGE_LEARNING.getCode());
        return codes;
    }

    public static List<String> getCareerProgramCodes() {
        List<String> codes = new ArrayList<>();
        codes.add(CAREER_PREPARATION.getCode());
        codes.add(COOP.getCode());
        codes.add(YOUTH_WORK_IN_TRADES.getCode());
        codes.add(CAREER_TECHNICAL_CENTER.getCode());
        return codes;
    }

    public static List<String> getIndigenousProgramCodes() {
        List<String> codes = new ArrayList<>();
        codes.add(ABORIGINAL_LANGUAGE.getCode());
        codes.add(ABORIGINAL_SUPPORT.getCode());
        codes.add(OTHER_APPROVED_NATIVE.getCode());
        return codes;
    }

    public static List<String> getELLCodes() {
        List<String> codes = new ArrayList<>();
        codes.add(ENGLISH_LANGUAGE_LEARNING.getCode());
        return codes;
    }
}
