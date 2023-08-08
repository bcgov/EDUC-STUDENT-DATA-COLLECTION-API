package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;

import java.util.Optional;

@Getter
public enum CollectionTypeCodes {
    ENTRY1("SEPTEMBER", new String[]{Constants.PUBLIC, Constants.INDEPEND, Constants.OFFSHORE}),
    ENTRY2("FEBRUARY", new String[]{Constants.PUBLIC, Constants.INDEPEND, Constants.OFFSHORE}),
    ENTRY3("MAY", new String[]{Constants.PUBLIC, Constants.INDEPEND, Constants.OFFSHORE}),
    ENTRY4("JULY", new String[]{"PUBLIC"})
;
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
