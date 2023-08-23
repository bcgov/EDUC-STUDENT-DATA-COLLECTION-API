package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum SchoolReportingRequirementCodes {
    REGULAR("REGULAR"),
    CSF("CSF"),
    NONE("NONE"),
    RECIPROCAL_TUITION("RT");

    @Getter
    private final String code;
    SchoolReportingRequirementCodes(String code) {
        this.code = code;
    }

    public static Optional<SchoolReportingRequirementCodes> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }

    public static List<String> getCodes() {
        return Arrays.stream(SchoolReportingRequirementCodes.values()).map(SchoolReportingRequirementCodes::getCode).collect(Collectors.toList());
    }
}
