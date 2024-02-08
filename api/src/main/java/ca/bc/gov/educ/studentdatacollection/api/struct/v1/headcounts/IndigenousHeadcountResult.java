package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface IndigenousHeadcountResult extends HeadcountResult{

    String getIndigenousLanguageTotal();
    String getIndigenousSupportTotal();
    String getOtherProgramTotal();
    String getAllSupportProgramTotal();
}
