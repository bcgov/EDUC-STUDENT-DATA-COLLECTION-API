package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

public interface EllHeadcountHeaderResult {
  String getEligibleStudents();
  String getReportedStudents();
  String getOneToFiveYears();
  String getSixPlusYears();
  String getAllStudents();
}
