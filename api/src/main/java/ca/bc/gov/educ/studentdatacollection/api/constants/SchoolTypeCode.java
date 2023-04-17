package ca.bc.gov.educ.studentdatacollection.api.constants;

import java.util.Arrays;
import java.util.Optional;

/**
 * this contains different codes to handle the flow for particular school types.
 */
public enum SchoolTypeCode {
  PUBLIC("PUBLIC"),
  INDEPEND("INDEPEND"),
  OFFSHORE("OFFSHORE");

  private final String code;
  SchoolTypeCode(String code) {
    this.code = code;
  }

  public static Optional<SchoolTypeCode> findByValue(String schoolCategoryCode) {
    return Arrays.stream(values()).filter(typeCode -> typeCode.code.equals(schoolCategoryCode)).findFirst();
  }
}
