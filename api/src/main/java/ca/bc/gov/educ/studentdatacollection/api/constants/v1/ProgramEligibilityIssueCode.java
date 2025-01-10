package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

public enum ProgramEligibilityIssueCode {
  HOMESCHOOL("HOMESCHOOL", "Homeschool students are not eligible for program funding."),
  OFFSHORE("OFFSHORE", "Offshore students are not eligible program funding."),
  TOO_YOUNG("TOO_YOUNG", "Students that are \"too young\" are not eligible for program funding."),
  OUT_OF_PROVINCE("OUTOFPROV", "Out of Province/International students are not eligible for program funding."),
  INACTIVE_ADULT("INACTADULT", "Students who have not been reported as \"active\" in a new course in the last two years are not eligible for program funding."),
  INACTIVE_SCHOOL_AGE("INACTMINOR", "Students who have not been reported as \"active\" in a new course in the last two years are not eligible for program funding."),
  NOT_ENROLLED_FRENCH("NTENRFRENC", "The student is not enrolled in french programs."),
  NOT_ENROLLED_CAREER("NTENRCAREE", "The student is not enrolled in career programs."),
  ENROLLED_CAREER_INDY_SCHOOL("ENRCARINDY", "Students reported by Independent Schools are not eligible for Career Program funding."),
  NOT_ENROLLED_INDIGENOUS("NTENRINDIG", "The student is not enrolled in indigenous programs."),
  NOT_ENROLLED_SPECIAL_ED("NTENRSPED", "The student was not reported in any inclusive education programs."),
  NON_ELIG_SPECIAL_EDUCATION("NELISPED", "Student must be school-aged or a non-graduated adult reported in a grade other than GA."),
  FEB_ONLINE_WITH_HISTORICAL_FUNDING("FEBSPEDERR", "Student has already been funded in September collection."),
  INDIGENOUS_ADULT("ISADULTAGE", "Student must be school-aged and self-identify as having Indigenous Ancestry to be eligible for funding for Indigenous Support Programs."),
  YEARS_IN_ELL("ELL5ORLESS", "Student must be school-aged and have been reported in ELL for 5 years or less."),
  NOT_ENROLLED_ELL("NTENRELL", "The student is not enrolled in the ELL program."),
  NO_INDIGENOUS_ANCESTRY("NOANCESTRY", "Student must be school-aged and self-identify as having Indigenous Ancestry to be eligible for funding for Indigenous Support Programs."),
  ELL_INDY_SCHOOL("ELLINDYERR", "Students reported by Independent Schools are not eligible for English Language Learning funding."),
  INDIGENOUS_INDY_SCHOOL("INDYERR", "Students reported by Independent Schools are not eligible for Indigenous Support Program funding.");

  @Getter
  private final String code;

  @Getter
  private final String message;

  ProgramEligibilityIssueCode(String code, String message) {
    this.code = code;
    this.message = message;
  }
}
