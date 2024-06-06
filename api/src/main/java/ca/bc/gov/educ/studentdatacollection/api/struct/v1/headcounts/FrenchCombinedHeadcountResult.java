package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface FrenchCombinedHeadcountResult extends HeadcountResult {
    String getSchoolAgedCoreFrench();
    String getAdultCoreFrench();
    String getTotalCoreFrench();
    String getSchoolAgedEarlyFrench();
    String getAdultEarlyFrench();
    String getTotalEarlyFrench();
    String getSchoolAgedLateFrench();
    String getAdultLateFrench();
    String getTotalLateFrench();
    String getSchoolAgedFrancophone();
    String getAdultFrancophone();
    String getTotalFrancophone();
    String getSchoolAgedTotals();
    String getAdultTotals();
    String getTotalTotals();
}
