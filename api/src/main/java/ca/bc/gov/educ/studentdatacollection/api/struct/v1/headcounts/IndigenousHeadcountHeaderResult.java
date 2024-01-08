package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface IndigenousHeadcountHeaderResult{

    String getEligIndigenousLanguage();
    String getReportedIndigenousLanguage();
    String getEligIndigenousSupport();
    String getReportedIndigenousSupport();
    String getEligOtherProgram();
    String getReportedOtherProgram();
    String getStudentsWithIndigenousAncestry();
    String getStudentsWithFundingCode20();
    String getAllStudents();
}
