package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum DuplicateTypeCode {

  ENROLLMENT("ENROLLMENT"),
  PROGRAM("PROGRAM");

  @Getter
  private final String code;
  DuplicateTypeCode(String code) {
    this.code = code;
  }

  public static Optional<DuplicateTypeCode> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
  }

}
