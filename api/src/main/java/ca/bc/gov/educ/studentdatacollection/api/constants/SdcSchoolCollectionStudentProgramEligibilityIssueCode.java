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
  NOT_ENROLLED_FRENCH("NOENROLLED", "The student is not enrolled in French programming.")
  ;

  @Getter
  private final String code;

  @Getter
  private final String message;

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
