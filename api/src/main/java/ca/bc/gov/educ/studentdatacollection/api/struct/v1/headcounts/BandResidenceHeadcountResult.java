package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface BandResidenceHeadcountResult extends HeadcountResult{

    String getBandCode();
    String getFteTotal();
    String getHeadcount();

}