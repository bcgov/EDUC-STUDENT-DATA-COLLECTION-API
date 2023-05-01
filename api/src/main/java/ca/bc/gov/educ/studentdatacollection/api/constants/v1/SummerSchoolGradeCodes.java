package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum SummerSchoolGradeCodes {
    GRADE01("01"),
    GRADE02("02"),
    GRADE03("03"),
    GRADE04("04"),
    GRADE05("05"),
    GRADE06("06"),
    GRADE07("07"),
    GRADE08("08"),
    GRADE09("09"),
    GRADE10("10"),
    GRADE11("11"),
    GRADE12("12")

;
    @Getter
    private final String code;
    SummerSchoolGradeCodes(String code) {
        this.code = code;
    }

    public static Optional<SummerSchoolGradeCodes> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
