package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface IndigenousHeadcountResult extends HeadcountResult{

    String getIndigenousLanguageTotal();
    String getIndigenousLanguageWithAncestry();
    String getIndigenousLanguageWithoutAncestry();
    String getIndigenousSupportTotal();
    String getIndigenousSupportWithAncestry();
    String getIndigenousSupportWithoutAncestry();
    String getOtherProgramTotal();
    String getOtherProgramWithAncestry();
    String getOtherProgramWithoutAncestry();
    String getAllSupportProgamTotal();
    String getAllSupportProgamWithAncestry();
    String getAllSupportProgamWithoutAncestry();
}
