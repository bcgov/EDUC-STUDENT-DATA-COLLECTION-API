package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface RefugeeHeadcountResult extends HeadcountResult {
   String getFteTotal();
   String getHeadcount();
   String getEll();
}
