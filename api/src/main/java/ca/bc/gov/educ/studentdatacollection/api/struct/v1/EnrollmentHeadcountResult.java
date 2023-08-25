package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import java.math.BigDecimal;

public interface EnrollmentHeadcountResult {
    String getEnrolledGradeCode();
    Long getSchoolAgedHeadcount();
    Long getSchoolAgedEligibleForFte();
    BigDecimal getSchoolAgedFteTotal();
    Long getAdultHeadcount();
    Long getAdultEligibleForFte();
    BigDecimal getAdultFteTotal();
    Long getTotalHeadcount();
    Long getTotalEligibleForFte();
    BigDecimal getTotalFteTotal();
}
