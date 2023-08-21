package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum SchoolFundingCodes {
    EDUC_SERVICE_CHILDREN("05"),
    OUT_OF_PROVINCE("14"),
    STATUS_FIRST_NATION("20");

    @Getter
    private final String code;
    SchoolFundingCodes(String code) {
        this.code = code;
    }

    public static Optional<SchoolFundingCodes> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }

    public static List<String> getCodes() {
        return Arrays.stream(SchoolFundingCodes.values()).map(SchoolFundingCodes::getCode).collect(Collectors.toList());
    }
}
