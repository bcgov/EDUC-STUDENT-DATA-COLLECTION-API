package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

public enum ProgramEligibilityIssueCode {
  HOMESCHOOL("HOMESCHOOL", "Homeschool students are not eligible for program funding.", ProgramEligibilityTypeCode.BASE),
  OFFSHORE("OFFSHORE", "Offshore students are not eligible program funding.", ProgramEligibilityTypeCode.BASE),
  TOO_YOUNG("TOO_YOUNG", "Students that are \"too young\" are not eligible for program funding.", ProgramEligibilityTypeCode.BASE),
  OUT_OF_PROVINCE("OUTOFPROV", "Out of Province/International students are not eligible for program funding.", ProgramEligibilityTypeCode.BASE),
  INACTIVE_ADULT("INACTADULT", "Students who have not been reported as \"active\" in a new course in the last two years are not eligible for program funding.", ProgramEligibilityTypeCode.BASE),
  INACTIVE_SCHOOL_AGE("INACTMINOR", "Students who have not been reported as \"active\" in a new course in the last two years are not eligible for program funding.", ProgramEligibilityTypeCode.BASE),
  ZERO_COURSES_ADULT("ZEROCRSADU", "Adult students must be reported with at least one course.", ProgramEligibilityTypeCode.BASE),
  ZERO_COURSES_SCHOOL_AGE("ZEROCRSAGE", "Secondary students must be reported with at least one course.", ProgramEligibilityTypeCode.BASE),
  NOT_ENROLLED_FRENCH("NTENRFRENC", "The student is not enrolled in french programs.", ProgramEligibilityTypeCode.FRENCH),
  NOT_ENROLLED_CAREER("NTENRCAREE", "The student is not enrolled in career programs.", ProgramEligibilityTypeCode.CAREER_PROGRAMS),
  ENROLLED_CAREER_INDY_SCHOOL("ENRCARINDY", "Students reported by Independent Schools are not eligible for Career Program funding.", ProgramEligibilityTypeCode.CAREER_PROGRAMS),
  NOT_ENROLLED_INDIGENOUS("NTENRINDIG", "The student is not enrolled in indigenous programs.", ProgramEligibilityTypeCode.IND_SUPPORT),
  NOT_ENROLLED_SPECIAL_ED("NTENRSPED", "The student was not reported in any inclusive education programs.", ProgramEligibilityTypeCode.SPED),
  NON_ELIG_SPECIAL_EDUCATION("NELISPED", "Student must be school-aged or a non-graduated adult reported in a grade other than GA.", ProgramEligibilityTypeCode.SPED),
  FEB_ONLINE_WITH_HISTORICAL_FUNDING("FEBSPEDERR", "Student has no new courses and is not continuing their educational program for this school year.", ProgramEligibilityTypeCode.SPED),
  INDP_FIRST_NATION_SPED("FUND20SPED", "Students reported by Independent School with Funding code 20 are not eligible for Inclusive Education funding.", ProgramEligibilityTypeCode.SPED),
  INDIGENOUS_ADULT("ISADULTAGE", "Student must be school-aged and self-identify as having Indigenous Ancestry to be eligible for funding for Indigenous Support Programs.", ProgramEligibilityTypeCode.IND_SUPPORT),
  YEARS_IN_ELL("ELL5ORLESS", "Student must be school-aged and have been reported in ELL for 5 years or less.", ProgramEligibilityTypeCode.ELL),
  NOT_ENROLLED_ELL("NTENRELL", "The student is not enrolled in the ELL program.", ProgramEligibilityTypeCode.ELL),
  NO_INDIGENOUS_ANCESTRY("NOANCESTRY", "Student must be school-aged and self-identify as having Indigenous Ancestry to be eligible for funding for Indigenous Support Programs.", ProgramEligibilityTypeCode.IND_SUPPORT),
  ELL_INDY_SCHOOL("ELLINDYERR", "Students reported by Independent Schools are not eligible for English Language Learning funding.", ProgramEligibilityTypeCode.ELL),
  INDIGENOUS_INDY_SCHOOL("INDYERR", "Students reported by Independent Schools are not eligible for Indigenous Support Program funding.", ProgramEligibilityTypeCode.IND_SUPPORT),
  X_ENROLL("XENROLL", "8/9 Cross-enrollment students are not eligible for program funding.", ProgramEligibilityTypeCode.BASE),
  SUMMER_SCHOOL_FRENCH("SSFRENCH", "Summer School students are not eligible for French Program funding.", ProgramEligibilityTypeCode.FRENCH),
  SUMMER_SCHOOL_CAREER("SSCAREER", "Summer School students are not eligible for Career Program funding.", ProgramEligibilityTypeCode.CAREER_PROGRAMS),
  ;

  @Getter
  private final String code;

  @Getter
  private final String message;

  @Getter final ProgramEligibilityTypeCode programEligibilityTypeCode;

  ProgramEligibilityIssueCode(String code, String message, ProgramEligibilityTypeCode programEligibilityTypeCode) {
    this.code = code;
    this.message = message;
    this.programEligibilityTypeCode = programEligibilityTypeCode;
  }
}
