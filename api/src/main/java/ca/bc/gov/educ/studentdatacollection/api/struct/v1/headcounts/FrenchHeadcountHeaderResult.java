package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface FrenchHeadcountHeaderResult {
  String getTotalCoreFrench();
  String getReportedCoreFrench();
  String getTotalEarlyFrench();
  String getReportedEarlyFrench();
  String getTotalLateFrench();
  String getReportedLateFrench();
  String getTotalFrancophone();
  String getReportedFrancophone();
  String getAllStudents();
}
