package ca.bc.gov.educ.studentdatacollection.api.constants;

import lombok.Getter;

/**
 * The enum Pen request batch student validation field code.
 */
public enum SdcSchoolCollectionStudentValidationFieldCode {
  LOCAL_ID("LOCALID"),
  STUDENT_PEN("STUDENT_PEN"),
  LEGAL_FIRST_NAME("LEGAL_FIRST_NAME"),
  LEGAL_MIDDLE_NAMES("LEGAL_MIDDLE_NAMES"),
  LEGAL_LAST_NAME("LEGAL_LAST_NAME"),
  USUAL_FIRST_NAME("USUAL_FIRST_NAME"),
  USUAL_MIDDLE_NAMES("USUAL_MIDDLE_NAMES"),
  USUAL_LAST_NAME("USUAL_LAST_NAME"),
  DOB("DOB"),
  GENDER_CODE("GENDER_CODE"),
  GRADE_CODE("GRADE_CODE"),
  SPECIAL_EDUCATION_CATEGORY_CODE("SPECIAL_EDUCATION_CATEGORY_CODE"),
  SCHOOL_FUNDING_CODE("SCHOOL_FUNDING_CODE"),
  NATIVE_ANCESTRY_IND("NATIVE_ANCESTRY_IND"),
  HOME_LANGUAGE_SPOKEN_CODE("HOME_LANGUAGE_SPOKEN_CODE"),
  OTHER_COURSES("OTHER_COURSES"),
  SUPPORT_BLOCKS("SUPPORT_BLOCKS"),
  ENROLLED_GRADE_CODE("ENROLLED_GRADE_CODE"),
  ENROLLED_PROGRAM_CODE("ENROLLED_PROGRAM_CODE"),
  CAREER_PROGRAM_CODE("CAREER_PROGRAM_CODE"),
  NUMBER_OF_COURSES("NUMBER_OF_COURSES"),
  BAND_CODE("BAND_CODE"),
  POSTAL_CODE("POSTAL_CODE");

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
  SdcSchoolCollectionStudentValidationFieldCode(String code) {
    this.code = code;
  }
}
