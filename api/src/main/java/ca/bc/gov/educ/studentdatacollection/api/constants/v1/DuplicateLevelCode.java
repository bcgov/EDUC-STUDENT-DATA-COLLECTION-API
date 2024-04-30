package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum DuplicateLevelCode {

  IN_DIST("IN_DIST"),
  PROVINCIAL("PROVINCIAL");

  @Getter
  private final String code;
  DuplicateLevelCode(String code) {
    this.code = code;
  }

  public static Optional<DuplicateLevelCode> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
  }

}
