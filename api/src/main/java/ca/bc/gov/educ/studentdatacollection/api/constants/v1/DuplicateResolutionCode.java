package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

public enum DuplicateResolutionCode {

  RELEASED("RELEASED", "Student removed from school."),
  GRADE_CHNG("GRADE_CHNG", "Student grade was changed."),
  RESOLVED("RESOLVED", "Student removed from conflicting program."),
  MIN_UPDT("MIN_UPDT", "Updated by Ministry Staff."),
  ;

  @Getter
  private final String code;

  @Getter
  private final String message;

  DuplicateResolutionCode(final String code, final String message) {
    this.code = code;
    this.message = message;
  }
}
