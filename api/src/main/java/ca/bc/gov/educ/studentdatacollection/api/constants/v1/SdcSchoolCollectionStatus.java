package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

/**
 * The enum Pen request batch status codes.
 */
public enum SdcSchoolCollectionStatus {

  NEW("NEW"),
  DISTRICT_UPLOAD("DIS_UPLOAD"),

  SCH_D_VRFD("SCH_D_VRFD"),

  SCH_C_VRFD("SCH_C_VRFD"),

  REVIEWED("REVIEWED"),

  VERIFIED("VERIFIED"),

  SUBMITTED("SUBMITTED"),

  COMPLETED("COMPLETED"),

  LOAD_FAIL("LOADFAIL"),

  LOADED("LOADED"),

  DUP_VRFD("DUP_VRFD"),

  P_DUP_POST("P_DUP_POST"),

  P_DUP_VRFD("P_DUP_VRFD");

  /**
   * The Code.
   */
  @Getter
  private final String code;

  /**
   * Instantiates a new Pen request batch status codes.
   *
   * @param code the code
   */
  SdcSchoolCollectionStatus(final String code) {
    this.code = code;
  }
}
