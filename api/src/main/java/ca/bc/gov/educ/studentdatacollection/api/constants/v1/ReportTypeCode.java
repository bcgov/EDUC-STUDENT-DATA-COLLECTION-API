package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * The enum Event outcome.
 */
public enum ReportTypeCode {

  GRADE_ENROLLMENT_FTE("GRADE_ENROLLMENT_FTE");

  @Getter
  private final String code;
  ReportTypeCode(String code) {
    this.code = code;
  }

  public static Optional<ReportTypeCode> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
  }

}
