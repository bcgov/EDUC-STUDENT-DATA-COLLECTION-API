package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum InclusiveEducationVarianceHeader {

    DISTRICT_NUMBER("District Number"),
    DISTRICT_NAME("District Name"),
    SEPT_A("September - Level 1 - A - Physically Dependent"),
    SEPT_B("September - Level 1 - B - Deafblind"),
    SEPT_C("September - Level 2 - C - Moderate to Profound Intellectual Disability"),
    SEPT_D("September - Level 2 - D - Physical Disability or Chronic Health Impairment"),
    SEPT_E("September - Level 2 - E - Visual Impairment"),
    SEPT_F("September - Level 2 - F - Deaf or Hard of Hearing"),
    SEPT_G("September - Level 2 - G - Autism Spectrum Disorder"),
    SEPT_H("September - Level 3 - H - Intensive Behaviour Interventions or Serious Mental Illness"),
    SEPT_K("September - Other - K - Mild Intellectual Disability"),
    SEPT_P("September - Other - P - Gifted"),
    SEPT_Q("September - Other - Q - Learning Disability"),
    SEPT_R("September - Other - R - Moderate Behaviour Support/Mental Illness"),
    FEB_A("February - Level 1 - A - Physically Dependent"),
    FEB_B("February - Level 1 - B - Deafblind"),
    FEB_C("February - Level 2 - C - Moderate to Profound Intellectual Disability"),
    FEB_D("February - Level 2 - D - Physical Disability or Chronic Health Impairment"),
    FEB_E("February - Level 2 - E - Visual Impairment"),
    FEB_F("February - Level 2 - F - Deaf or Hard of Hearing"),
    FEB_G("February - Level 2 - G - Autism Spectrum Disorder"),
    FEB_H("February - Level 3 - H - Intensive Behaviour Interventions or Serious Mental Illness"),
    FEB_K("February - Other - K - Mild Intellectual Disability"),
    FEB_P("February - Other - P - Gifted"),
    FEB_Q("February - Other - Q - Learning Disability"),
    FEB_R("February - Other - R - Moderate Behaviour Support/Mental Illness"),
    VARIANCE_A("Variance - Level 1 - A - Physically Dependent"),
    VARIANCE_B("Variance - Level 1 - B - Deafblind"),
    VARIANCE_C("Variance - Level 2 - C - Moderate to Profound Intellectual Disability"),
    VARIANCE_D("Variance - Level 2 - D - Physical Disability or Chronic Health Impairment"),
    VARIANCE_E("Variance - Level 2 - E - Visual Impairment"),
    VARIANCE_F("Variance - Level 2 - F - Deaf or Hard of Hearing"),
    VARIANCE_G("Variance - Level 2 - G - Autism Spectrum Disorder"),
    VARIANCE_H("Variance - Level 3 - H - Intensive Behaviour Interventions or Serious Mental Illness"),
    VARIANCE_K("Variance - Other - K - Mild Intellectual Disability"),
    VARIANCE_P("Variance - Other - P - Gifted"),
    VARIANCE_Q("Variance - Other - Q - Learning Disability"),
    VARIANCE_R("Variance - Other - R - Moderate Behaviour Support/Mental Illness");

    private final String code;
    InclusiveEducationVarianceHeader(String code) { this.code = code; }
}
