package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum ProgramDuplicateTypeCode {

  INDIGENOUS("INDIGENOUS"),
  CAREER("CAREER"),
  LANGUAGE("LANGUAGE"),
  SPECIAL_ED("SPECIAL_ED");

  @Getter
  private final String code;
  ProgramDuplicateTypeCode(String code) {
    this.code = code;
  }

  public static Optional<ProgramDuplicateTypeCode> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
  }

}
