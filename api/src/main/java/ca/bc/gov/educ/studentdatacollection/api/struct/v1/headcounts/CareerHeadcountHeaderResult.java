package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface CareerHeadcountHeaderResult {
  String getEligCareerPrep();
  String getReportedCareerPrep();
  String getEligCoopEduc();
  String getReportedCoopEduc();
  String getEligApprentice();
  String getReportedApprentice();
  String getEligTechOrYouth();
  String getReportedTechOrYouth();
  String getAllStudents();
}
