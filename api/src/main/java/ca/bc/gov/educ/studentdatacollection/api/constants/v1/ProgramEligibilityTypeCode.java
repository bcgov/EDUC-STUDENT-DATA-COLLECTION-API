package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

public enum ProgramEligibilityTypeCode {
  BASE("BASE"),
  SPED("SPED"),
  FRENCH("FRENCH"),
  ELL("ELL"),
  IND_SUPPORT("IND_SUPPORT"),
  CAREER_PROGRAMS("CAREER_PROGRAMS");


  @Getter
  private final String code;

  ProgramEligibilityTypeCode(String code) {
    this.code = code;
  }
}
