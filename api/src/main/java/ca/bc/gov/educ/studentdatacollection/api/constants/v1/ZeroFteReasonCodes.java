package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

/**
 * The enum that describes the reason a fte calculation resulted in a 0
 */
public enum ZeroFteReasonCodes {

  TOO_YOUNG("TOOYOUNG", "The student is too young."),
  OUT_OF_PROVINCE("OUTOFPROV", "Out-of-Province/International Students are not eligible for funding."),
  OFFSHORE("OFFSHORE", "Offshore students do not receive funding."),
  INACTIVE("INACTIVE", "The student has not been reported as \"active\" in a new course in the last two years."),
  NOMINAL_ROLL_ELIGIBLE("NOMROLL", "The student is Nominal Roll eligible and is federally funded."),
  IND_AUTH_DUPLICATE_FUNDING("AUTHDUP", "The authority has already received funding for the student this year."),
  GRADUATED_ADULT_IND_AUTH("INDYADULT", "The student is graduated adult reported by an independent school."),
  DISTRICT_DUPLICATE_FUNDING("DISTDUP", "The district has already received funding for the student this year."),
  NO_ONLINE_LEARNING("NOONLINE", "None of student's educational program was delivered through online learning this year."),
  NOT_REPORTED("NOREPORT", "Student was not reported in Grade 8 or 9 outside of district this school year."),
  ZERO_COURSES("ZERO_COURSES", "The student was reported with zero courses and in a secondary grade or adult grade level."),
  ;

  @Getter
  private final String code;
  @Getter
  private final String message;

  ZeroFteReasonCodes(String code, String message) {
    this.code = code;
    this.message = message;
  }
}
