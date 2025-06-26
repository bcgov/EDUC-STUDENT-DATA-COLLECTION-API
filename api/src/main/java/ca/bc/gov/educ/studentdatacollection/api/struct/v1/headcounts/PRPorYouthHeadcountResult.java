package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

/**
 * This interface is used as data mapper for PRP or Youth Summary report.
 */
public interface PRPorYouthHeadcountResult extends HeadcountResult{
    String getYouthTotals();
    String getShortPRPTotals();
    String getLongPRPTotals();
    String getYouthPRPTotals();
}
