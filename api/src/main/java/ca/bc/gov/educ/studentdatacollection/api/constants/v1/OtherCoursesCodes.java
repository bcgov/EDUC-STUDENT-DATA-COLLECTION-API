package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum OtherCoursesCodes {
    ZERO("0"),
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),

;
    @Getter
    private final String code;
    OtherCoursesCodes(String code) {
        this.code = code;
    }

    public static Optional<OtherCoursesCodes> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }

    public static Optional<OtherCoursesCodes> matchSupportBlockValue(String value) {
        List<OtherCoursesCodes> supportBlockSublist = Arrays.stream(values()).filter(e -> !e.code.equals("9")).toList();
        return supportBlockSublist.stream().filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
