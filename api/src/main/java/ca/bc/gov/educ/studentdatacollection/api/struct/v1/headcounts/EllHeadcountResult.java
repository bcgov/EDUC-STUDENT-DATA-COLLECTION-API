package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface EllHeadcountResult extends HeadcountResult {
    String getTotalEllStudents();
    String getTotalAdultEllStudents();
    String getTotalSchoolAgedEllStudents();
}
