package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum DuplicateSeverityCode {

  ALLOWABLE("ALLOWABLE"),
  NON_ALLOWABLE("NON_ALLOW");

  @Getter
  private final String code;
  DuplicateSeverityCode(String code) {
    this.code = code;
  }

  public static Optional<DuplicateSeverityCode> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
  }

}
