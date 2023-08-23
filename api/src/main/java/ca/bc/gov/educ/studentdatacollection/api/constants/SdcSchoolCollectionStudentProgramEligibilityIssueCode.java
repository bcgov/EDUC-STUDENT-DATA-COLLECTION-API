package ca.bc.gov.educ.studentdatacollection.api.constants;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public enum SdcSchoolCollectionStudentProgramEligibilityIssueCode {
  HOMESCHOOL("HOMESCHOOL", "Home schooled students are not eligible for this program."),
  OFFSHORE("OFFSHORE", "Offshore students are not eligible for this program."),
  OUT_OF_PROVINCE("OUTOFPROV", "Students who are out of province are not eligible for this program."),
  INACTIVE_ADULT("INACTADULT", "Adult students must have been reported to have 1 or more courses in the last two years to be eligible for this program."),
  INACTIVE_SCHOOL_AGE("INACTMINOR", "School aged students must have been reported to have 1 or more courses in the last two years to be eligible for this program."),
  NOT_ENROLLED_FRENCH("NTENRFRENC", "The student is not enrolled in french programs."),
  NOT_ENROLLED_CAREER("NTENRCAREE", "The student is not enrolled in career programs."),
  NOT_ENROLLED_INDIGENOUS("NTENRINDIG", "The student is not enrolled in indigenous programs."),
  NOT_ENROLLED_SPECIAL_ED("NOSPECIAL", "The student was not reported in any special education programs."),
  NON_ELIG_SPECIAL_EDUCATION("NELISPED", "Student must be school-aged or a non-graduated adult to be eligible for Special Education funding."),
  INDIGENOUS_ADULT("ISADULTAGE", "Indigenous students must be school-age to be eligible for indigenous programs."),
  YEARS_IN_ELL("ELL5ORLESS", "Student must be school-aged and have been reported in ELL for 5 years or less."),
  NOT_ENROLLED_ELL("NTENRELL", "The student is not enrolled in the ELL program."),
  NO_INDIGENOUS_ANCESTRY("NOANCESTRY", "Indigenous students must be school-age to be eligible for indigenous programs.");

  @Getter
  private final String code;

  @Getter
  private final String message;

  public static final Map<SdcSchoolCollectionStudentProgramEligibilityIssueCode, Consumer<String>> getEligibilityErrorHandlers(SdcSchoolCollectionStudentEntity student) {
    return Map.of(
      NOT_ENROLLED_FRENCH, student::setFrenchProgramNonEligReasonCode,
      NOT_ENROLLED_CAREER, student::setCareerProgramNonEligReasonCode,
      NOT_ENROLLED_INDIGENOUS, student::setIndigenousSupportProgramNonEligReasonCode,
      NOT_ENROLLED_SPECIAL_ED, student::setSpecialEducationNonEligReasonCode,
      NON_ELIG_SPECIAL_EDUCATION, student::setSpecialEducationNonEligReasonCode,
      INDIGENOUS_ADULT, student::setIndigenousSupportProgramNonEligReasonCode,
      NO_INDIGENOUS_ANCESTRY, student::setIndigenousSupportProgramNonEligReasonCode
    );
  }

  public static final Optional<SdcSchoolCollectionStudentProgramEligibilityIssueCode> getBaseProgramEligibilityFailure(
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors
  ) {
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> ineligibleCodes = Arrays.asList(
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
