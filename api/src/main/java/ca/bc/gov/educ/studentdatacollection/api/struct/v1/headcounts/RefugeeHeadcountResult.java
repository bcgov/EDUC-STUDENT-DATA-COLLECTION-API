package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface RefugeeHeadcountResult extends HeadcountResult {
   String getSchoolCode();
   String getFteTotal();
   String getHeadcount();
}
