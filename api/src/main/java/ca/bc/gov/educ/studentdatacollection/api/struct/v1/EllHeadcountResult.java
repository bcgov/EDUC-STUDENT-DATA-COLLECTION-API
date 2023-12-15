package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

public interface EllHeadcountResult {
    String getEnrolledGradeCode();
    Long getSchoolAgedOneThroughFive();
    Long getSchoolAgedSixPlus();
    Long getSchoolAgedTotals();
    Long getAdultOneThroughFive();
    Long getAdultSixPlus();
    Long getAdultTotals();
    Long getAllOneThroughFive();
    Long getAllSixPlus();
    Long getTotalEllStudents();
}
