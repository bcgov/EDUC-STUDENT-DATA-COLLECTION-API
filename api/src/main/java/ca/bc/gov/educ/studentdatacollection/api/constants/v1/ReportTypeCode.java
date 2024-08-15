package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * The enum Event outcome.
 */
public enum ReportTypeCode {

  GRADE_ENROLLMENT_HEADCOUNT("GRADE_ENROLLMENT_HEADCOUNT"),
  DIS_GRADE_ENROLLMENT_HEADCOUNT("DIS_GRADE_ENROLLMENT_HEADCOUNT"),
  DIS_GRADE_ENROLLMENT_HEADCOUNT_PER_SCHOOL("DIS_GRADE_ENROLLMENT_HEADCOUNT_PER_SCHOOL"),
  ELL_HEADCOUNT("ELL_HEADCOUNT"),
  DIS_ELL_HEADCOUNT("DIS_ELL_HEADCOUNT"),
  DIS_ELL_HEADCOUNT_PER_SCHOOL("DIS_ELL_HEADCOUNT_PER_SCHOOL"),
  DIS_REFUGEE_HEADCOUNT_PER_SCHOOL("DIS_REFUGEE_HEADCOUNT_PER_SCHOOL"),
  SPECIAL_EDUCATION_HEADCOUNT("SPECIAL_EDUCATION_HEADCOUNT"),
  DIS_SPECIAL_EDUCATION_HEADCOUNT("DIS_SPECIAL_EDUCATION_HEADCOUNT"),
  DIS_SPECIAL_EDUCATION_HEADCOUNT_PER_SCHOOL("DIS_SPECIAL_EDUCATION_HEADCOUNT_PER_SCHOOL"),
  INDIGENOUS_HEADCOUNT("INDIGENOUS_HEADCOUNT"),
  DIS_INDIGENOUS_HEADCOUNT("DIS_INDIGENOUS_HEADCOUNT"),
  DIS_INDIGENOUS_HEADCOUNT_PER_SCHOOL("DIS_INDIGENOUS_HEADCOUNT_PER_SCHOOL"),
  BAND_RESIDENCE_HEADCOUNT("BAND_RESIDENCE_HEADCOUNT"),
  CAREER_HEADCOUNT("CAREER_HEADCOUNT"),
  FRENCH_HEADCOUNT("FRENCH_HEADCOUNT"),
  DIS_FRENCH_HEADCOUNT("DIS_FRENCH_HEADCOUNT"),
  DIS_FRENCH_HEADCOUNT_PER_SCHOOL("DIS_FRENCH_HEADCOUNT_PER_SCHOOL"),
  DIS_CAREER_HEADCOUNT("DIS_CAREER_HEADCOUNT"),
  DIS_CAREER_HEADCOUNT_PER_SCHOOL("DIS_CAREER_HEADCOUNT_PER_SCHOOL"),
  ALL_STUDENT_SCHOOL_CSV("ALL_STUDENT_SCHOOL_CSV"),
  ALL_STUDENT_DIS_CSV("ALL_STUDENT_DIS_CSV");

  @Getter
  private final String code;
  ReportTypeCode(String code) {
    this.code = code;
  }

  public static Optional<ReportTypeCode> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
  }

}
