package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

public interface FrenchHeadcountResult {
    String getEnrolledGradeCode();
    Long getSchoolAgedCoreFrench();
    Long getAdultCoreFrench();
    Long getTotalCoreFrench();
    Long getSchoolAgedEarlyFrench();
    Long getAdultEarlyFrench();
    Long getTotalEarlyFrench();
    Long getSchoolAgedLateFrench();
    Long getAdultLateFrench();
    Long getTotalLateFrench();
    Long getSchoolAgedFrancophone();
    Long getAdultFrancophone();
    Long getTotalFrancophone();
    Long getSchoolAgedTotals();
    Long getAdultTotals();
    Long getTotalTotals();
}
