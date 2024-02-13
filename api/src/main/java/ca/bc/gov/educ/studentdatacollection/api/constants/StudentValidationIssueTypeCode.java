package ca.bc.gov.educ.studentdatacollection.api.constants;

import lombok.Getter;

/**
 * The enum Pen request batch student validation issue type code.
 */
public enum StudentValidationIssueTypeCode {

  /**
   * Gender invalid student validation issue type code.
   */
  GENDER_INVALID("GENDERINVALID", "Gender must be M or F"),

  /**
   * Blank local id student validation issue type code.
   */
  LOCALID_BLANK("LOCALIDBLANK", "Local identifier number is blank."),

  DOB_BLANK("DOBBLANK", "Birthdate cannot be blank."),
  DOB_INVALID_FORMAT("DOBINVALIDFORMAT", "Student's birthdate must be a valid calendar date that is not in the future."),
  STUDENT_PEN_BLANK("STUDENTPENBLANK", "Students in Summer School must be reported with a PEN."),
  STUDENT_PEN_DUPLICATE("STUDENTPENDUPLICATE", "PEN reported more than once. Correct the PEN or remove the appropriate student from the submission."),
  LEGAL_LAST_NAME_BLANK("LEGALLASTNAMEBLANK", "Legal surname cannot be blank. If student only has one name, it must be placed in the surname field and the given name field can be left blank."),
  LEGAL_FIRST_NAME_CHAR_FIX("LEGALFIRSTNAMECHARFIX", "Legal given name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  LEGAL_MIDDLE_NAME_CHAR_FIX("LEGALMIDDLENAMECHARFIX", "Legal middle name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  LEGAL_LAST_NAME_CHAR_FIX("LEGALLASTNAMECHARFIX", "Legal surname contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  USUAL_FIRST_NAME_CHAR_FIX("USUALFIRSTNAMECHARFIX", "Usual given name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  USUAL_MIDDLE_NAME_CHAR_FIX("USUALMIDDLENAMECHARFIX", "Usual middle name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  USUAL_LAST_NAME_CHAR_FIX("USUALLASTNAMECHARFIX", "Usual surname contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field."),
  LEGAL_FIRST_NAME_BAD_VALUE("LEGALFIRSTNAMEBADVALUE", "Legal given name contains a questionable value."),
  LEGAL_MIDDLE_NAME_BAD_VALUE("LEGALMIDDLENAMEBADVALUE", "Legal middle name contains a questionable value."),
  LEGAL_LAST_NAME_BAD_VALUE("LEGALLASTNAMEBADVALUE", "Legal surname contains a questionable value."),
  USUAL_FIRST_NAME_BAD_VALUE("USUALFIRSTNAMEBADVALUE", "Usual given name contains a questionable value."),
  USUAL_MIDDLE_NAME_BAD_VALUE("USUALMIDDLENAMEBADVALUE", "Usual middle name contains a questionable value."),
  USUAL_LAST_NAME_BAD_VALUE("USUALLASTNAMEBADVALUE", "Usual surname contains a questionable value."),
  PROGRAM_CODE_HS_LANG("PROGRAMCODEHSLANG", "Home school students will not be funded for any reported language programs."),
  PROGRAM_CODE_HS_IND("PROGRAMCODEHSIND", "Home school students will not be funded for any reported Indigenous support programs."),
  PROGRAM_CODE_HS_SPED("PROGRAMCODEHSSPED", "Home school students will not be funded for any reported special education designations."),
  FUNDING_CODE_INVALID("FUNDINGCODEINVALID","Invalid school funding code."),
  NATIVE_IND_INVALID("NATIVEINDINVALID","Invalid Indigenous ancestry indicator. "),
  ENROLLED_WRONG_REPORTING("ENROLLEDWRONGREPORTING", "Programme francophone may only be reported at a Conseil scolaire francophone school."),
  ENROLLED_NO_FRANCOPHONE("ENROLLEDNOFRANCOPHONE", "Students in a Conseil francophone school must be enrolled in Programme francophone."),
  ENROLLED_CODE_INVALID("ENROLLEDCODEINVALID", "List of enrolled programs includes an invalid code. Select the valid programs for the student."),
  ENROLLED_CODE_COUNT_ERR("ENROLLEDCODECOUNTERR", "Students can have at most one French language code."),
  CAREER_CODE_INVALID("CAREERCODEINVALID", "Invalid career code."),
  ADULT_ZERO_COURSES("ADULTZEROCOURSES", "Adult students must have 0 or more courses."),
  SCHOOLAGE_ZERO_COURSES("SCHOOLAGEZEROCOURSES", "Secondary students must be reported with at least one course. If the student is not enrolled in any courses they should be removed from the submission."),
  AGE_LESS_THAN_FIVE("AGELESSTHANFIVE", "Student is too young for school and is not eligible for funding."),
  CONT_ED_ERR("CONTEDERR", "Student is too young for Continuing Education. Student must be removed from submission or their birthdate adjusted."),
  PROGRAM_CODE_IND("PROGRAMCODEIND", "Student's reported with Indigenous Education Programs and Services must be reported with Indigenous Ancestry to get funding for the programs."),
  ENROLLED_CODE_FRANCOPHONE_ERR("ENROLLEDCODEFRANCOPHONEERR", "Student grade must be 6 or 7 for Late French Immersion."),
  BAND_CODE_BLANK("BANDCODEBLANK", "Student must be reported with both a Band of Residence and as Ordinarily Living on Reserve (funding code 20)."),
  BAND_CODE_INVALID("BANDCODEINVALID", "Invalid Band Code (Band of Residence) reported."),
  NO_OF_COURSE_MAX("NOOFCOURSEMAX", "Student was reported with more than 15 courses."),
  ENROLLED_CODE_FUNDING_ERR("ENROLLEDCODEFUNDINGERR", "Out-of-Province/International students will not be funded any reported language programs."),
  ENROLLED_CODE_IND_ERR("ENROLLEDCODEINDERR", "Out-of-Province/International students will not be funded any reported Indigenous support programs."),
  ENROLLED_CODE_CAREER_ERR("ENROLLEDCODECAREERERR", "Out-of-Province/International students will not be funded any reported career programs."),
  ENROLLED_CODE_SP_ED_ERR("ENROLLEDCODESPEDERR", "Out-of-Province/International students will not be funded any reported special education programs."),
  SUMMER_GRADE_CODE("SUMMERGRADECODE", "Students in summer school must enrolled in grade 1-12."),
  STUDENT_ADULT_ERR("STUDENTADULTERR", "Student cannot be an adult."),
  KH_GRADE_CODE_INVALID("KHGRADECODEINVALID", "Student can only be registered in Kindergarten Halftime if they are in an Independent School."),
  SUPPORT_BLOCKS_NA("SUMMERSUPPORTBLOCKSNA", "Support blocks cannot be reported for summer learners."),
  SUPPORT_BLOCKS_INVALID("SUPPORTBLOCKSINVALID", "Number of Support Blocks must be a value from 0 to 8."),
  SUPPORT_FACILITY_NA("SUPPORTFACILITYNA", "Support blocks are not valid for students in Online Learning schools."),
  MISSING_POSTAL_CODE("MISSINGPOSTALCODE", "Missing postal code."),
  INVALID_POSTAL_CODE("INVALIDPOSTALCODE", "Missing postal code."),
  OTHER_COURSE_INVALID("OTHERCOURSEINVALID", "Number of Other Courses must be blank or a number from 0 to 9."),
  ENROLLED_CODE_PARSE_ERR("ENROLLEDCODEPARSEERR", "List of enrolled program codes could not be parsed. Select the correct program codes for the student."),
  ENROLLED_CODE_DUP_ERR("ENROLLEDCODEDUPERR", "List of enrolled codes contained duplicate codes. Duplicate codes have been removed. Verify that the codes below are the correct codes for the student."),
  INVALID_GRADE_CODE("INVALIDGRADECODE", "Invalid grade code."),
  SPOKEN_LANG_ERR("SPOKENLANGERR", "Primary language spoken in home is invalid."),
  HS_NOT_SCHOOL_AGE("HSNOTSCHOOLAGE", "Students registered in home school must be school-aged. Student must be removed from the submission or have their birthdate adjusted."),
  ADULT_INCORRECT_GRADE("ADULTINCORRECTGRADE", "Adult students cannot be reported in an elementary grade."),
  CAREER_CODE_PROG_ERR("CAREERCODEPROGERR", "Student must be reported with both a Enrolled Career Program and Career Code."),
  CAREER_CODE_COUNT_ERR("CAREERCODECOUNTERR", "Students can only be reported with one Career Program."),
  CAREER_CODE_GRADE_ERR("CAREERCODEGRADEERR", "Student must be enrolled grade 8-12 or SU to be reported in a Career Program."),
  SUPPORT_BLOCKS_NOT_COUNT("SUPPORTBLOCKSNOTCOUNT", "Support blocks will only be counted toward funding if the student is taking less than 8 courses."),
  GA_ERROR("GAERROR", "School-aged students cannot be reported in grade GA."),
  ADULT_SUPPORT_ERR("ADULTSUPPORTERR", "Adult students will not receive funding for support blocks."),
  ADULT_GRADE_ERR("ADULTGRADEERR", "Adult in Online Learning must be in grade 10, 11, 12, SU, or GA."),
  SPED_ERR("SPEDERR", "Reported Special Education Category is not valid."),
  PEN_CHECK_DIGIT_ERR("PENCHECKDIGITERR", "Student's PEN is not valid. Adjust or remove the PEN."),
  NO_OF_COURSES_INVALID("NOOFCOURSESINVALID", "Number of Courses must be a number."),
  ADULT_GRADUATED("ADULTGRADUATED", "Adult graduates must be reported in grade GA."),
  SCHOOL_AGED_GRADUATE_SUMMER("SCHLAGEDGRADSUMMER", "School-aged student has graduated and cannot be reported in Summer School. Remove the student from the submission."),
  SCHOOL_AGED_GRADUATE_SUPPORT_BLOCKS("SCHLAGEDGRADSUPPORT", "Graduated school-aged students will not receive funding for support blocks."),
  GRADUATE_STUDENT_INDEPENDENT("GRADSTUDENTINDEPEND", "Graduated adult students are not eligible for funding."),
  ADULT_ZERO_COURSE_HISTORY("ADULTZEROCOURSEH", "Zero courses reported in last two years."),
  SCHOOL_AGED_ZERO_COURSE_HISTORY("SCHOOLAGEDZEROCOURSEH", "Zero courses reported in last two years."),
  ADULT_NO_INDIGENOUS_SUPPORT("ADULTNOINDIGENOUSSUPPORT", "Only school-aged students will receive funding for Indigenous Support Programs."),
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
  StudentValidationIssueTypeCode(String code, String message) {
    this.code = code;
    this.message = message;
  }
}
