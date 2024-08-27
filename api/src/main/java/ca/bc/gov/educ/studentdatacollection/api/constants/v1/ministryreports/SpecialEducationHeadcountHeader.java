package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum SpecialEducationHeadcountHeader {

    SCHOOL("School Name"),
    A("A - Physically Dependent"),
    B("B - Deafblind"),
    C("C - Moderate to Profound Intellectual Disability"),
    D("D - Physical Disability or Chronic Health Impairment"),
    E("E - Visual Impairment"),
    F("F - Deaf or Hard of Hearing"),
    G("G - Autism Spectrum Disorder"),
    H("H - Intensive Behaviour Interventions or Serious Mental Illness"),
    K("K - Mild Intellectual Disability"),
    P("P - Gifted"),
    Q("Q - Learning Disability"),
    R("R - Moderate Behaviour Support/Mental Illness"),
    TOTAL("Total");

    private final String code;
    SpecialEducationHeadcountHeader(String code) { this.code = code; }
}
