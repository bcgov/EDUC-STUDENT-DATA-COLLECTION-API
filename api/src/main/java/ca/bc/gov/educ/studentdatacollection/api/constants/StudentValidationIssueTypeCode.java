package ca.bc.gov.educ.studentdatacollection.api.constants;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode.*;

/**
 * The enum Pen request batch student validation issue type code.
 */
public enum StudentValidationIssueTypeCode {

  /**
   * Gender invalid student validation issue type code.
   */
  GENDER_INVALID("GENDERINVALID", "Gender must be M or F", ERROR),

  /**
   * Blank local id student validation issue type code.
   */
  LOCALID_BLANK("LOCALIDBLANK", "Local identifier number is blank.", INFO_WARNING),

  DOB_BLANK("DOBBLANK", "Birthdate cannot be blank.", ERROR),
  DOB_INVALID_FORMAT("DOBINVALIDFORMAT", "Student's birthdate must be a valid calendar date that is not in the future.", ERROR),
  STUDENT_PEN_BLANK("STUDENTPENBLANK", "Students must be reported with a PEN.", ERROR),
  STUDENT_PEN_DUPLICATE("STUDENTPENDUPLICATE", "PEN reported more than once. Correct the PEN or remove the appropriate student from the submission.", ERROR),
  LEGAL_LAST_NAME_BLANK("LEGALLASTNAMEBLANK", "Legal surname cannot be blank. If student only has one name, it must be placed in the surname field and the given name field can be left blank.", ERROR),
  LEGAL_FIRST_NAME_CHAR_FIX("LEGALFIRSTNAMECHARFIX", "Legal given name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field.", ERROR),
  LEGAL_MIDDLE_NAME_CHAR_FIX("LEGALMIDDLENAMECHARFIX", "Legal middle name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field.", ERROR),
  LEGAL_LAST_NAME_CHAR_FIX("LEGALLASTNAMECHARFIX", "Legal surname contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field.", ERROR),
  USUAL_FIRST_NAME_CHAR_FIX("USUALFIRSTNAMECHARFIX", "Usual given name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field.", ERROR),
  USUAL_MIDDLE_NAME_CHAR_FIX("USUALMIDDLENAMECHARFIX", "Usual middle name contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field.", ERROR),
  USUAL_LAST_NAME_CHAR_FIX("USUALLASTNAMECHARFIX", "Usual surname contains characters that are not yet supported by legacy systems. Please remove or replace any special characters in this field.", ERROR),
  LEGAL_FIRST_NAME_BAD_VALUE("LEGALFIRSTNAMEBADVALUE", "Legal given name contains a questionable value.", INFO_WARNING),
  LEGAL_MIDDLE_NAME_BAD_VALUE("LEGALMIDDLENAMEBADVALUE", "Legal middle name contains a questionable value.", INFO_WARNING),
  LEGAL_LAST_NAME_BAD_VALUE("LEGALLASTNAMEBADVALUE", "Legal surname contains a questionable value.", INFO_WARNING),
  USUAL_FIRST_NAME_BAD_VALUE("USUALFIRSTNAMEBADVALUE", "Usual given name contains a questionable value.", INFO_WARNING),
  USUAL_MIDDLE_NAME_BAD_VALUE("USUALMIDDLENAMEBADVALUE", "Usual middle name contains a questionable value.", INFO_WARNING),
  USUAL_LAST_NAME_BAD_VALUE("USUALLASTNAMEBADVALUE", "Usual surname contains a questionable value.", INFO_WARNING),
  PROGRAM_CODE_HS_LANG("PROGRAMCODEHSLANG", "Home school students will not be funded for any reported language programs.", FUNDING_WARNING),
  PROGRAM_CODE_HS_IND("PROGRAMCODEHSIND", "Home school students will not be funded for any reported Indigenous support programs.", FUNDING_WARNING),
  PROGRAM_CODE_HS_SPED("PROGRAMCODEHSSPED", "Home school students will not be funded for any reported inclusive education designations.", FUNDING_WARNING),
  FUNDING_CODE_INVALID("FUNDINGCODEINVALID","Invalid school funding code.", ERROR),
  NATIVE_IND_INVALID("NATIVEINDINVALID","Invalid Indigenous ancestry indicator. ", ERROR),
  ENROLLED_WRONG_REPORTING("ENROLLEDWRONGREPORTING", "Programme francophone may only be reported at a Conseil scolaire francophone school.", ERROR),
  ENROLLED_NO_FRANCOPHONE("ENROLLEDNOFRANCOPHONE", "Students in a Conseil francophone school must be enrolled in Programme francophone.", ERROR),
  ENROLLED_CODE_INVALID("ENROLLEDCODEINVALID", "List of enrolled programs includes an invalid code. Select the valid programs for the student.", ERROR),
  ENROLLED_CODE_COUNT_ERR("ENROLLEDCODECOUNTERR", "Students can have at most one French language code.", ERROR),
  CAREER_CODE_INVALID("CAREERCODEINVALID", "Invalid career code.", ERROR),
  ADULT_ZERO_COURSES("ADULTZEROCOURSES", "Adult students must have more than 0 courses.", ERROR),
  SCHOOLAGE_ZERO_COURSES("SCHOOLAGEZEROCOURSES", "Secondary students must be reported with at least one course. If the student is not enrolled in any courses they should be removed from the submission.", ERROR),
  AGE_LESS_THAN_FIVE("AGELESSTHANFIVE", "Student is too young for school and is not eligible for funding.", FUNDING_WARNING),
  CONT_ED_ERR("CONTEDERR", "Student is too young for Continuing Education. Student must be removed from submission or their birthdate adjusted.", ERROR),
  PROGRAM_CODE_IND("PROGRAMCODEIND", "Only students who self identify as having Indigenous Ancestry are eligible for program funding in Indigenous Education Programs and Services.", FUNDING_WARNING),
  ENROLLED_CODE_FRANCOPHONE_ERR("ENROLLEDCODEFRANCOPHONEERR", "Student grade must be 6 or 7 for Late French Immersion.", ERROR),
  BAND_CODE_BLANK("BANDCODEBLANK", "Student must be reported with both a Band of Residence and as Ordinarily Living on Reserve (funding code 20).", ERROR),
  BAND_CODE_INVALID("BANDCODEINVALID", "Invalid Band Code (Band of Residence) reported.", ERROR),
  NO_OF_COURSE_MAX("NOOFCOURSEMAX", "Student was reported with more than 15 courses.", INFO_WARNING),
  INVALID_GRADE_SCHOOL_FUNDING_GROUP("INVALIDGRADESCHOOLFUNDINGGROUP", "Student grade does not fall within the grade range for which the school has been approved.", INFO_WARNING),
  ENROLLED_CODE_FUNDING_ERR("ENROLLEDCODEFUNDINGERR", "Out-of-Province/International students will not be funded any reported language programs.", FUNDING_WARNING),
  ENROLLED_CODE_IND_ERR("ENROLLEDCODEINDERR", "Out-of-Province/International students will not be funded any reported Indigenous support programs.", FUNDING_WARNING),
  ENROLLED_CODE_CAREER_ERR("ENROLLEDCODECAREERERR", "Out-of-Province/International students will not be funded any reported career programs.", FUNDING_WARNING),
  ENROLLED_CODE_SP_ED_ERR("ENROLLEDCODESPEDERR", "Out-of-Province/International students will not be funded any reported inclusive education programs.", FUNDING_WARNING),
  SUMMER_GRADE_CODE("SUMMERGRADECODE", "Students in summer school must enrolled in grade 1-12.", ERROR),
  STUDENT_ADULT_ERR("STUDENTADULTERR", "Adult students cannot be reported in Summer School.", ERROR),
  KH_GRADE_CODE_INVALID("KHGRADECODEINVALID", "Student can only be registered in Kindergarten Halftime if they are in an Independent School.", ERROR),
  SUPPORT_BLOCKS_NA("SUMMERSUPPORTBLOCKSNA", "Support blocks cannot be reported for summer learners.", ERROR),
  SUPPORT_BLOCKS_INVALID("SUPPORTBLOCKSINVALID", "Number of Support Blocks must be a value from 0 to 8.", ERROR),
  SUPPORT_FACILITY_NA("SUPPORTFACILITYNA", "Support blocks are not valid for students in Online Learning schools.", ERROR),
  MISSING_POSTAL_CODE("MISSINGPOSTALCODE", "Missing postal code.", INFO_WARNING),
  INVALID_POSTAL_CODE("INVALIDPOSTALCODE", "Invalid postal code.", INFO_WARNING),
  OTHER_COURSE_INVALID("OTHERCOURSEINVALID", "Number of Other Courses must be blank or a number from 0 to 9.", ERROR),
  ENROLLED_CODE_PARSE_ERR("ENROLLEDCODEPARSEERR", "List of enrolled program codes could not be parsed. Select the correct program codes for the student.", ERROR),
  ENROLLED_CODE_DUP_ERR("ENROLLEDCODEDUPERR", "List of enrolled codes contained duplicate codes. Duplicate codes have been removed. Verify that the codes below are the correct codes for the student.", ERROR),
  INVALID_GRADE_CODE("INVALIDGRADECODE", "Invalid grade code.", ERROR),
  SPOKEN_LANG_ERR("SPOKENLANGERR", "Primary language spoken in home is invalid.", ERROR),
  HS_NOT_SCHOOL_AGE("HSNOTSCHOOLAGE", "Students registered in home school must be school-aged. Student must be removed from the submission or have their birthdate adjusted.", ERROR),
  ADULT_INCORRECT_GRADE("ADULTINCORRECTGRADE", "Adult students cannot be reported in an elementary grade.", ERROR),
  CAREER_CODE_PROG_ERR("CAREERCODEPROGERR", "Student must be reported with both a Enrolled Career Program and Career Code.", ERROR),
  CAREER_CODE_COUNT_ERR("CAREERCODECOUNTERR", "Students can only be reported with one Career Program.", ERROR),
  CAREER_CODE_GRADE_ERR("CAREERCODEGRADEERR", "Student must be enrolled in grade 8-12, SU or GA to be reported in a Career Program.", ERROR),
  SUPPORT_BLOCKS_NOT_COUNT("SUPPORTBLOCKSNOTCOUNT", "Support blocks will only be counted toward funding if the student is taking less than 8 courses.", FUNDING_WARNING),
  GA_ERROR("GAERROR", "School-aged students cannot be reported in grade GA.", ERROR),
  ADULT_SUPPORT_ERR("ADULTSUPPORTERR", "Adult students will not receive funding for support blocks.", FUNDING_WARNING),
  ADULT_GRADE_ERR("ADULTGRADEERR", "Adult in Online Learning must be in grade 10, 11, 12, SU, or GA.", ERROR),
  SPED_ERR("SPEDERR", "Reported Inclusive Education Category is not valid.", ERROR),
  PEN_CHECK_DIGIT_ERR("PENCHECKDIGITERR", "Student's PEN is not valid. Adjust or remove the PEN.", ERROR),
  NO_OF_COURSES_INVALID("NOOFCOURSESINVALID", "Number of Courses must be a number.", ERROR),
  ADULT_GRADUATED("ADULTGRADUATED", "Adult graduates must be reported in grade GA.", ERROR),
  SCHOOL_AGED_GRADUATE_SUMMER("SCHLAGEDGRADSUMMER", "School-aged student has graduated and cannot be reported in Summer School. Remove the student from the submission.", ERROR),
  SCHOOL_AGED_GRADUATE_SUPPORT_BLOCKS("SCHLAGEDGRADSUPPORT", "Graduated school-aged students will not receive funding for support blocks.", FUNDING_WARNING),
  GRADUATE_STUDENT_INDEPENDENT("GRADSTUDENTINDEPEND", "Graduated adult students are not eligible for funding.", FUNDING_WARNING),
  ADULT_ZERO_COURSE_HISTORY("ADULTZEROCOURSEH", "Zero courses reported for an adult student and no course activity for the past two years.", FUNDING_WARNING),
  SCHOOL_AGED_ZERO_COURSE_HISTORY("SCHOOLAGEDZEROCOURSEH", "Student has zero courses reported and no course activity for the past two years. This student will not receive program funding.", FUNDING_WARNING),
  SCHOOL_AGED_INDIGENOUS_SUPPORT("SCHOOLAGEDINDIGENOUSSUPPORT", "Only school-aged students will receive funding for Indigenous Support Programs.", FUNDING_WARNING),
  SCHOOL_AGED_ELL("SCHOOLAGEDELL", "Only school-aged students will receive funding for English Language Learning.", FUNDING_WARNING),
  SCHOOL_AGED_SPED("SCHOOLAGEDSPED", "Only school-aged students or non-graduated adults will receive funding for Inclusive Education.", FUNDING_WARNING),
  REFUGEE_IN_PREV_COL("REFUGEEINPREVCOL", "School-aged students reported in the previous collection are not eligible for newcomer refugee funding.", FUNDING_WARNING),
  REFUGEE_IS_ADULT("REFUGEEISADULT", "Adults are not eligible for February newcomer refugee funding.", FUNDING_WARNING),
  SUMMER_PUBLIC_SCHOOL_GRADE_ERROR("SUMMERPUBLICSCHOOLGRADEERROR","8/9 cross enrollment students must be in grades 8 or 9", ERROR),
  SUMMER_STUDENT_REPORTED_IN_DISTRICT_ERROR("SUMMERSTUDENTREPORTEDINDISTRICTERROR","The student has already been reported by the district during the current school year.", ERROR),
  SUMMER_STUDENT_REPORTED_NOT_IN_DISTRICT_ERROR("SUMMERSTUDENTREPORTEDNOTINDISTRICTERROR","Student was not reported in Grade 8 or 9 in this school year and cannot be reported in 8/9 cross enrollment in this collection.", ERROR),
  SUMMER_ADULT_STUDENT_ERROR("SUMMERADULTSTUDENTERROR","Adult students cannot be reported in 8/9 cross enrollment", ERROR),
  SUMMER_FUNDING_CODE_ERROR("SUMMERFUNDINGCODEERROR","Out of Province/International students cannot be reported in Summer collection", ERROR),
  SUMMER_PRE_PRIMARY_ERROR("SUMMERPREPRIMARYERROR","Pre-primary or early childhood education students cannot be reported in 8/9 cross enrollment", ERROR),
  SUMMER_ENROLLED_PROGRAM_ERROR("SUMMERENROLLEDPROGRAMERROR","Students enrolled in the grade 8/9 cross-enrollment collection are not eligible for funding for inclusive education or additional programs.", FUNDING_WARNING),
  SUMMER_FRENCH_CAREER_PROGRAM_ERROR("SUMMERFRENCHCAREERPROGRAM_ERROR","Students in summer school are not eligible for additional funding for Career or French Programs", FUNDING_WARNING),
  ;

  private static final Map<String, StudentValidationIssueTypeCode> CODE_MAP = new HashMap<>();

  static {
    for (StudentValidationIssueTypeCode type : values()) {
      CODE_MAP.put(type.getCode(), type);
    }
  }

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

  @Getter
  private final StudentValidationIssueSeverityCode severityCode;

  /**
   * Instantiates a new Pen request batch student validation issue type code.
   *
   * @param code the code
   */
  StudentValidationIssueTypeCode(String code, String message, StudentValidationIssueSeverityCode severityCode) {
    this.code = code;
    this.message = message;
    this.severityCode = severityCode;
  }
  public static StudentValidationIssueTypeCode findByValue(String value) {
    return CODE_MAP.get(value);
  }
}
