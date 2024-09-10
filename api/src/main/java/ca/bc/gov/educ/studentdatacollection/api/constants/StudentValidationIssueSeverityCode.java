package ca.bc.gov.educ.studentdatacollection.api.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * The enum Pen request batch student validation issue severity code.
 */
public enum StudentValidationIssueSeverityCode {
  /**
   * Error student validation issue severity code.
   */
  ERROR("Error"),
  /**
   * Funding Warning student validation issue severity code.
   */
  FUNDING_WARNING("Funding Warning"),
  /**
   * Informational Warning student validation issue severity code.
   */
  INFO_WARNING("Info Warning");


  /**
   * The Code.
   */
  @Getter
  private final String code;

  /**
   * Instantiates a new Pen request batch student validation field code.
   *
   * @param code the code
   */
  StudentValidationIssueSeverityCode(String code) {
    this.code = code;
  }

  public static Optional<StudentValidationIssueSeverityCode> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
  }
}
