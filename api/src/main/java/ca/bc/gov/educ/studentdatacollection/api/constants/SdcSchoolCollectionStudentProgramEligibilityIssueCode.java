package ca.bc.gov.educ.studentdatacollection.api.constants;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import lombok.Getter;

public enum SdcSchoolCollectionStudentProgramEligibilityIssueCode {
  CRITICIAL_ERROR("CRITICAL",
    "The student data contains critical errors and cannot be eligible for this program."),
  NOT_REPORTED("NOTREPORTD", "The student was not reported with this program."),
  HOMESCHOOL("HOMESCHOOL", "Home schooled students are not eligible for this program."),
  OFFSHORE("OFFSHORE", "Offshore students are not eligible for this program."),
  OUT_OF_PROVINCE("OUTOFPROV", "Students who are out of province are not eligible for this program."),
  INACTIVE_ADULT("INACTADULT", "Adult students must have been reported to have 1 or more courses in the last two"
    + " years to be eligible for this program."),
  INACTIVE_SCHOOL_AGE("INACTMINOR", "School aged students must have been reported to have 1 or more courses in the"
    + " last two years to be eligible for this program."),
  NOT_ENROLLED_FRENCH(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED,
    "The student is not enrolled with French programming."),
  NOT_ENROLLED_CAREER(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED,
    "The student is not enrolled with career programming."),
  NOT_ENROLLED_INDIGENOUS(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED,
    "The student is not enrolled with indigeneous programming."),
  DOES_NOT_NEED_SPECIAL_ED("NOSPECIAL", "The student does not require special education assistance."),
  IS_GRADUATED("GRADUATED", "Student must be school-aged or a non-graduated adult to be eligible for "
    + "Special Education funding."),
  INDIGENOUS_ADULT("ISADULTAGE", "Indigenous students must be school-age to be eligible for indigenous programs"),
  NO_INDIGENOUS_ANCESTRY("NOANCESTRY", "Indigenous students must be school-age to be eligible for indigenous programs")
  ;

  @Getter
  private final String code;

  @Getter
  private final String message;

  private static final String NOT_ENROLLED = "NOENROLLED";

  public static final Optional<SdcSchoolCollectionStudentProgramEligibilityIssueCode> getBaseProgramEligibilityFailure(
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors
  ) {
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> ineligibleCodes = Arrays.asList(
      CRITICIAL_ERROR,
      NOT_REPORTED,
      HOMESCHOOL,
      OFFSHORE,
      OUT_OF_PROVINCE,
      INACTIVE_ADULT,
      INACTIVE_SCHOOL_AGE
    );

    return errors
      .stream()
      .filter(ineligibleCodes::contains)
      .findFirst();
  }

  SdcSchoolCollectionStudentProgramEligibilityIssueCode(String code, String message) {
    this.code = code;
    this.message = message;
  }
}
