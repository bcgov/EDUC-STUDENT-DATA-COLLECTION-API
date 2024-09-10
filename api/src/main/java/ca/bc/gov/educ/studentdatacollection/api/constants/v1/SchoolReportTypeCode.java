package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * The enum Event outcome.
 */
public enum SchoolReportTypeCode {

  GRADE_ENROLLMENT_HEADCOUNT("GRADE_ENROLLMENT_HEADCOUNT"),
  ELL_HEADCOUNT("ELL_HEADCOUNT"),
  SPECIAL_EDUCATION_HEADCOUNT("SPECIAL_EDUCATION_HEADCOUNT"),
  INDIGENOUS_HEADCOUNT("INDIGENOUS_HEADCOUNT"),
  BAND_RESIDENCE_HEADCOUNT("BAND_RESIDENCE_HEADCOUNT"),
  CAREER_HEADCOUNT("CAREER_HEADCOUNT"),
  FRENCH_HEADCOUNT("FRENCH_HEADCOUNT"),
  ALL_STUDENT_SCHOOL_CSV("ALL_STUDENT_SCHOOL_CSV"),
  ALL_STUDENT_ERRORS_WARNS_SCHOOL_CSV("ALL_STUDENT_ERRORS_WARNS_SCHOOL_CSV");

  @Getter
  private final String code;
  SchoolReportTypeCode(String code) {
    this.code = code;
  }

  public static Optional<SchoolReportTypeCode> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
  }

}
