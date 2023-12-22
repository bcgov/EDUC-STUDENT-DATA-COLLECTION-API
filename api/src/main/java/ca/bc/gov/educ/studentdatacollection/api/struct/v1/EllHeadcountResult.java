package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResult;

public interface EllHeadcountResult extends HeadcountResult {
    String getSchoolAgedOneThroughFive();
    String getSchoolAgedSixPlus();
    String getSchoolAgedTotals();
    String getAdultOneThroughFive();
    String getAdultSixPlus();
    String getAdultTotals();
    String getAllOneThroughFive();
    String getAllSixPlus();
    String getTotalEllStudents();
}
