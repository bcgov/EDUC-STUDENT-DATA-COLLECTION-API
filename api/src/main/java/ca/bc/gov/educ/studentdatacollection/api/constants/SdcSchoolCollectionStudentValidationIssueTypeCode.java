package ca.bc.gov.educ.studentdatacollection.api.constants;

import lombok.Getter;

/**
 * The enum Pen request batch student validation issue type code.
 */
public enum SdcSchoolCollectionStudentValidationIssueTypeCode {

  /**
   * Gender invalid student validation issue type code.
   */
  GENDER_INVALID("GENDERINVALID", "Gender must be M or F"),

  /**
   * Blank local id student validation issue type code.
   */
  LOCALID_BLANK("LOCALIDBLANK", "Local identifier number is blank."),
  DOB_INVALID_FORMAT("DOBINVALIDFORMAT", "Student's birthdate must be a valid calendar date that is not in the future."),
  STUDENT_PEN_BLANK("STUDENTPENBLANK", "Students in Summer School must be reported with a PEN."),
  LEGAL_LAST_NAME_BLANK("LEGALLASTNAMEBLANK", "Legal surname cannot be blank. If student only has one name, it must be placed in the surname field and the given name field can be left blank."),
  LEGAL_FIRST_NAME_CHAR_FIX("LEGALFIRSTNAMECHARFIX", "Legal given name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  LEGAL_MIDDLE_NAME_CHAR_FIX("LEGALMIDDLENAMECHARFIX", "Legal middle name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  LEGAL_LAST_NAME_CHAR_FIX("LEGALLASTNAMECHARFIX", "Legal surname contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  USUAL_FIRST_NAME_CHAR_FIX("USUALFIRSTNAMECHARFIX", "Usual given name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  USUAL_MIDDLE_NAME_CHAR_FIX("USUALMIDDLENAMECHARFIX", "Usual middle name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  USUAL_LAST_NAME_CHAR_FIX("USUALLASTNAMECHARFIX", "Usual surname contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  LEGAL_FIRST_NAME_BAD_VALUE("LEGALFIRSTNAMEBADVALUE", "Legal given name contains a questionable value."),
  LEGAL_MIDDLE_NAME_BAD_VALUE("LEGALMIDDLENAMEBADVALUE", "Legal middle name contains a questionable value."),
  LEGAL_LAST_NAME_BAD_VALUE("LEGALLASTNAMEBADVALUE", "Legal surname name contains a questionable value."),
  USUAL_FIRST_NAME_BAD_VALUE("USUALFIRSTNAMEBADVALUE", "Usual given name contains a questionable value."),
  USUAL_MIDDLE_NAME_BAD_VALUE("USUALMIDDLENAMEBADVALUE", "Usual middle name contains a questionable value."),
  USUAL_LAST_NAME_BAD_VALUE("USUALLASTNAMEBADVALUE", "Usual surname contains a questionable value."),
  PROGRAM_CODE_HS_LANG("PROGRAMCODEHSLANG", "Home school students will not be funded for any reported language programs."),
  PROGRAM_CODE_HS_IND("PROGRAMCODEHSIND", "Home school students will not be funded for any reported Indigenous support programs."),
  PROGRAM_CODE_HS_CAREER("PROGRAMCODEHSCAREER", "Home school students will not be funded for any reported career programs."),
  PROGRAM_CODE_HS_SPED("PROGRAMCODEHSSPED", "Home school students will not be funded for any reported special education designations."),
  FUNDING_CODE_INVALID("FUNDINGCODEINVALID","Invalid school funding code."),
  NATIVE_IND_INVALID("NATIVEINDINVALID","Invalid Indigenous ancestry indicator. "),
  ENROLLED_WRONG_REPORTING("ENROLLEDWRONGREPORTING", "Programme francophone may only be reported at a Conseil scolaire francophone school."),
  ENROLLED_NO_FRANCOPHONE("ENROLLEDNOFRANCOPHONE", "Students in a Conseil francophone school must be enrolled in Programme francophone."),
  ENROLLED_CODE_INVALID("ENROLLEDCODEINVALID", "List of enrolled programs includes an invalid code. Select the valid programs for the student."),
  ENROLLED_CODE_COUNT_ERR("ENROLLEDCODECOUNTERR", "Students can have at most one French language code."),
  CAREER_CODE_INVALID("CAREERCODEINVALID", "Invalid career code.")




  ;

  /**
   * The Code.
   */
  @Getter
  private final String code;

  /**
   * Validation message
   */
  @Getter
  private final String message;

  /**
   * Instantiates a new Pen request batch student validation issue type code.
   *
   * @param code the code
   */
  SdcSchoolCollectionStudentValidationIssueTypeCode(String code, String message) {
    this.code = code;
    this.message = message;
  }
}
