package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

public enum SdcDistrictCollectionStatus {

  NEW("NEW"),

  LOADED("LOADED"),

  REVIEWED("REVIEWED"),

  VERIFIED("VERIFIED"),

  D_DUP_VRFD("D_DUP_VRFD"),

  COMPLETED("COMPLETED");


  @Getter
  private final String code;

  SdcDistrictCollectionStatus(final String code) {
    this.code = code;
  }
}
