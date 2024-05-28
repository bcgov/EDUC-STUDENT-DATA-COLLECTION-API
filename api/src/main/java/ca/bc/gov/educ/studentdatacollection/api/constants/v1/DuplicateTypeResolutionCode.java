package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum DuplicateTypeResolutionCode {

  CHANGE_GRADE("CHANGE_GRADE"),
  DELETE_ENROLLMENT_DUPLICATE("DELETE_ENROLLMENT_DUPLICATE"),
  PROGRAM("PROGRAM");

  private final String code;
  DuplicateTypeResolutionCode(String code) {
    this.code = code;
  }

  public static Optional<DuplicateTypeResolutionCode> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
  }

}
