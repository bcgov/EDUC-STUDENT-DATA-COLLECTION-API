package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface ISFSPrelimHeadcountResult extends HeadcountResult{
    String getSpecialEducationLevel1Count();
    String getSpecialEducationLevel2Count();
    String getSpecialEducationLevel3Count();
    String getSpecialEducationLevelOtherCount();

    String getAdultsKto3Fte();
    String getAdultsKto9Fte();
    String getAdults4to7EUFte();
    String getAdults8to10SUFte();
    String getAdults11and12Fte();
    String getAdults10to12Fte();

    String getSchoolAgedKHFte();
    String getSchoolAgedKFFte();
    String getSchoolAged1to3Fte();
    String getSchoolAgedKto9Fte();
    String getSchoolAged4to7EUFte();
    String getSchoolAged8to10SUFte();
    String getSchoolAged11and12Fte();
    String getSchoolAged10to12Fte();

    String getTotalHomeschoolCount();
}
