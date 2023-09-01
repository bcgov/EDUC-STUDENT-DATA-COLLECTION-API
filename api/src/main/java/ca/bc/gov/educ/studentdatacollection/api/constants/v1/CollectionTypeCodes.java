package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum CollectionTypeCodes {
    SEPTEMBER("SEPTEMBER", new String[]{SchoolCategoryCodes.PUBLIC.getCode(), SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.OFFSHORE.getCode()}),
    FEBRUARY("FEBRUARY", new String[]{SchoolCategoryCodes.PUBLIC.getCode(), SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.OFFSHORE.getCode()}),
    MAY("MAY", new String[]{SchoolCategoryCodes.PUBLIC.getCode(), SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.OFFSHORE.getCode()}),
    JULY("JULY", new String[]{SchoolCategoryCodes.PUBLIC.getCode()});

    private final String typeCode;
    private final String[] schoolCode;
    CollectionTypeCodes(String typeCode, String[] schoolCode) {
        this.typeCode = typeCode;
        this.schoolCode = schoolCode;
    }

    public static Optional<CollectionTypeCodes> findByValue(String collectionTypeCode, String schoolTypeCode) {
        return Arrays.stream(values()).filter(code -> code.typeCode.equalsIgnoreCase(collectionTypeCode) && Arrays.asList(code.schoolCode).contains(schoolTypeCode)).findFirst();
    }
}
