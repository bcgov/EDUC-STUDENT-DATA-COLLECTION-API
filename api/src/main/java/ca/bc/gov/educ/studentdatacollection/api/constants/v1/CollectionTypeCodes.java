package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import java.util.Arrays;

import java.util.Optional;

public enum CollectionTypeCodes {
    SEPTEMBER("SEPTEMBER"),
    MAY("MAY"),
    JULY("JULY"),
    FEBRUARY("FEBRUARY");

    private final String code;
    CollectionTypeCodes(String code) {
        this.code = code;
    }

    public static Optional<CollectionTypeCodes> findByValue(String collectionTypeCode) {
        return Arrays.stream(values()).filter(typeCode -> typeCode.code.equals(collectionTypeCode)).findFirst();
    }
}
