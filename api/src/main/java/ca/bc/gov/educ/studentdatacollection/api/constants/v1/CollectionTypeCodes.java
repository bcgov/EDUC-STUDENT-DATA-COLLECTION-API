package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

@Getter
public enum CollectionTypeCodes {
    SEPTEMBER("SEPTEMBER"),
    FEBRUARY("FEBRUARY"),
    MAY("MAY"),
    JULY("JULY");

    private final String typeCode;
    CollectionTypeCodes(String typeCode) {
        this.typeCode = typeCode;
    }
}
