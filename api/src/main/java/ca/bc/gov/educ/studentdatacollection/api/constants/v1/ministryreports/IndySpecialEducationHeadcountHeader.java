package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum IndySpecialEducationHeadcountHeader {

    AUTHORITY_NUMBER("Authority Number"),
    AUTHORITY_NAME("Authority Name"),
    MIN_CODE("Mincode"),
    SCHOOL("School Name"),
    LEVEL_1("Level 1 Total"),
    A("A - Physically Dependent (Level 1)"),
    B("B - Deafblind (Level 1)"),
    LEVEL_2("Level 2 Total"),
    C("C - Moderate to Profound Intellectual Disability (Level 2)"),
    D("D - Physical Disability or Chronic Health Impairment (Level 2)"),
    E("E - Visual Impairment (Level 2)"),
    F("F - Deaf or Hard of Hearing (Level 2)"),
    G("G - Autism Spectrum Disorder (Level 2)"),
    LEVEL_3("Level 3 Total"),
    H("H - Intensive Behaviour Interventions or Serious Mental Illness  (Level 3)"),
    LEVEL_OTHER("SES Home School"),
    K("K - Mild Intellectual Disability (Other Level)"),
    P("P - Gifted (Other Level)"),
    Q("Q - Learning Disability (Other Level)"),
    R("R - Moderate Behaviour Support/Mental Illness (Other Level)"),
    TOTAL("Total");

    private final String code;
    IndySpecialEducationHeadcountHeader(String code) { this.code = code; }

}
