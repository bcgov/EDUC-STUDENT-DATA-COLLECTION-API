package ca.bc.gov.educ.studentdatacollection.api.constants;

import lombok.Getter;

/**
 * The enum Batch file constants.
 *
 * @author OM
 */
public enum SdcBatchFileConstants {

  TRANSACTION_CODE("transactionCode"),

  LOCAL_STUDENT_ID("localStudentID"),

  PEN("pen"),

  LEGAL_SURNAME("legalSurname"),

  LEGAL_GIVEN_NAME("legalGivenName"),

  LEGAL_MIDDLE_NAME("legalMiddleName"),

  USUAL_SURNAME("usualSurname"),

  USUAL_GIVEN_NAME("usualGivenName"),

  USUAL_MIDDLE_NAME("usualMiddleName"),

  BIRTH_DATE("birthDate"),

  GENDER("gender"),

  SPECIAL_EDUCATION_CATEGORY("specialEducationCategory"),

  UNUSED_BLOCK1("unusedBlock1"),

  SCHOOL_FUNDING_CODE("schoolFundingCode"),

  NATIVE_ANCESTRY_INDICATOR("nativeAncestryIndicator"),

  HOME_SPOKEN_LANGUAGE_CODE("homeSpokenLanguageCode"),

  UNUSED_BLOCK2("unusedBlock2"),

  OTHER_COURSES("otherCourses"),

  SUPPORT_BLOCKS("supportBlocks"),

  ENROLLED_GRADE_CODE("enrolledGradeCode"),

  ENROLLED_PROGRAM_CODES("enrolledProgramCodes"),

  CAREER_PROGRAM_CODE("careerProgramCode"),

  NUMBER_OF_COURSES("numberOfCourses"),

  BAND_CODE("bandCode"),

  POSTAL_CODE("postalCode"),
  /**
   * Student count batch file constants.
   */
  STUDENT_COUNT("studentCount"),
  /**
   * Vendor name batch file constants.
   */
  VENDOR_NAME("vendorName"),
  /**
   * Product name batch file constants.
   */
  PRODUCT_NAME("productName"),
  /**
   * Product id batch file constants.
   */
  PRODUCT_ID("productID"),
  /**
   * Header batch file constants.
   */
  HEADER("header"),
  /**
   * Trailer batch file constants.
   */
  TRAILER("trailer"),
  /**
   * School name batch file constants.
   */
  SCHOOL_NAME("schoolName"),
  /**
   * Request date batch file constants.
   */
  REQUEST_DATE("requestDate"),
  /**
   * Email batch file constants.
   */
  EMAIL("emailID"),
  /**
   * Fax number batch file constants.
   */
  FAX_NUMBER("faxNumber"),
  /**
   * Contact name batch file constants.
   */
  CONTACT_NAME("contactName"),
  /**
   * Office number batch file constants.
   */
  OFFICE_NUMBER("officeNumber"),
  /**
   * Min code batch file constants.
   */
  MIN_CODE("mincode");

  /**
   * The Name.
   */
  @Getter
  private final String name;

  /**
   * Instantiates a new Batch file constants.
   *
   * @param name the name
   */
  SdcBatchFileConstants(String name) {
    this.name = name;
  }
}
