package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface RefugeeHeadcountHeaderResult extends HeadcountResult {
    String getEligibleStudents();
    String getReportedStudents();
}
