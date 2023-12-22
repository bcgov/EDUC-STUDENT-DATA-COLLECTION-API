package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface CsfFrenchHeadcountResult extends HeadcountResult {
    String getSchoolAgedFrancophone();
    String getAdultFrancophone();
    String getTotalFrancophone();
}
