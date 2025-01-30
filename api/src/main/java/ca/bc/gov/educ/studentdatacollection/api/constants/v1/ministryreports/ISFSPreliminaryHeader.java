package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum ISFSPreliminaryHeader {

    REPORT_DATE("REPORT_DATE"),
    DISTNO("DISTNO"),
    SCHLNO("SCHLNO"),
    PRIM_FUND("PRIM_FUND"),
    ELEM_FUND("ELEM_FUND"),
    JUNR_FUND("JUNR_FUND"),
    SECN_FUND("SECN_FUND"),
    DL_13_FUND("DL_13_FUND"),
    DL_14_FUND("DL_14_FUND"),
    SL_1_HC("SL_1_HC"),
    SL_2_HC("SL_2_HC"),
    SL_3_HC("SL_3_HC"),
    SL_SES_HC("SL_SES_HC"),
    ADP_FTE("ADP_FTE"),
    ADE_FTE("ADE_FTE"),
    ADJ_FTE("ADJ_FTE"),
    ADS_FTE("ADS_FTE"),
    DL_AK9_FTE("DL_AK9_FTE"),
    DL_AAC_FTE("DL_AAC_FTE"),
    ES_KH_FTE("ES_KH_FTE"),
    ES_KF_FTE("ES_KF_FTE"),
    ES_PR_FTE("ES_PR_FTE"),
    ES_EL_FTE("ES_EL_FTE"),
    ES_JR_FTE("ES_JR_FTE"),
    ES_SR_FTE("ES_SR_FTE"),
    DL_13_FTE("DL_13_FTE"),
    DL_14_FTE("DL_14_FTE"),
    HS_HC("HS_HC"),
    IS_POSTED("IS_POSTED");

    private final String code;
    ISFSPreliminaryHeader(String code) { this.code = code; }
}
