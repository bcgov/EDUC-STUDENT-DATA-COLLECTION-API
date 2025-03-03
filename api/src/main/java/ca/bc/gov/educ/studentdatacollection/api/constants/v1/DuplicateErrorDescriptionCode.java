package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

public enum DuplicateErrorDescriptionCode {

  HS_DUP("HS_DUP", "The student is in homeschool and can’t be claimed by more than one school unless the other school is online and the student is in grades 10-12."),
  K_TO_9_DUP("K_TO_9_DUP", "The student is in grade K-9 and can’t be claimed by more than one school."),
  K_TO_7_DUP("K_TO_7_DUP", "The student is enrolled in grade K-7 in one of the schools. Only one school may claim the student."),
  IN_8_9_DUP("IN_8_9_DUP", "The student is enrolled in grade 8-9 in one of the schools. Only one school may claim the student."),
  NON_ALTDUP("NON_ALTDUP", "The student is enrolled in two different districts or authorities not involving an Online Learning school, only one school may claim the student"),
  ALT_DUP("ALT_DUP", "The student is enrolled in a school involving an Alternate program, only one school may claim the student.");


  @Getter
  private final String code;

  @Getter
  private final String message;

  DuplicateErrorDescriptionCode(final String code, final String message) {
    this.code = code;
    this.message = message;
  }
}
