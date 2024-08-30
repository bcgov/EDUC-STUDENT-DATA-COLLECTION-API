package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface EllHeadcountHeaderResult {
  String getEligibleStudents();
  String getIneligibleStudents();
  String getReportedStudents();
  String getOneToFiveYears();
  String getSixPlusYears();
  String getAllStudents();
}
