package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum CollectionTypeCodes {
    SEPTEMBER("SEPTEMBER", "FEBRUARY"),
    FEBRUARY("FEBRUARY", "MAY"),
    MAY("MAY", "JULY"),
    JULY("JULY", "SEPTEMBER");

    private final String typeCode;
    private final String nextCollectionToOpen;
    CollectionTypeCodes(String typeCode, String nextCollectionToOpen) {
        this.typeCode = typeCode;
        this.nextCollectionToOpen = nextCollectionToOpen;
    }

    public static Optional<CollectionTypeCodes> findByValue(String typeCode) {
        return Arrays.stream(values()).filter(code -> code.typeCode.equalsIgnoreCase(typeCode)).findFirst();
    }

}
