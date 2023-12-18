package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface EnrollmentHeadcountResult extends HeadcountResult {
    String getSchoolAgedHeadcount();
    String getSchoolAgedEligibleForFte();
    String getSchoolAgedFteTotal();
    String getAdultHeadcount();
    String getAdultEligibleForFte();
    String getAdultFteTotal();
    String getTotalHeadcount();
    String getTotalEligibleForFte();
    String getTotalFteTotal();
}
