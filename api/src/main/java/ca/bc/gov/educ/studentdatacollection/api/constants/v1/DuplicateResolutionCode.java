package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

public enum DuplicateResolutionCode {

  RELEASED("RELEASED", "Student released from school."),
  GRADE_CHNG("GRADE_CHNG", "The students grade was changed.");


  @Getter
  private final String code;

  @Getter
  private final String message;

  DuplicateResolutionCode(final String code, final String message) {
    this.code = code;
    this.message = message;
  }
}
