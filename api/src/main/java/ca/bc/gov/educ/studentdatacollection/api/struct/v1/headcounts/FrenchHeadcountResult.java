package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface FrenchHeadcountResult extends HeadcountResult {
    String getSchoolAgedCoreFrench();
    String getAdultCoreFrench();
    String getTotalCoreFrench();
    String getSchoolAgedEarlyFrench();
    String getAdultEarlyFrench();
    String getTotalEarlyFrench();
    String getSchoolAgedLateFrench();
    String getAdultLateFrench();
    String getTotalLateFrench();
    String getSchoolAgedTotals();
    String getAdultTotals();
    String getTotalTotals();
}
