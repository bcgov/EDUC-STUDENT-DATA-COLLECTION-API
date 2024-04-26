package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

public enum SdcDistrictCollectionStatus {

  NEW("NEW"),

  LOADED("LOADED"),

  COMPLETED("COMPLETED"),

  REVIEWED("REVIEWED");

  @Getter
  private final String code;

  SdcDistrictCollectionStatus(final String code) {
    this.code = code;
  }
}
