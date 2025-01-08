package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum CEAndOLEnrolmentAndFteHeader {

    DISTRICT_NUMBER("District Number"),
    SCHOOL_NUMBER("School Number"),
    SCHOOL_NAME("School Name"),
    FACILITY_TYPE("Facility Type"),

    KIND_HT_COUNT("Headcount Kind(H/T) New CE/OL"),
    KIND_HT_REFUGEE_COUNT("Headcount Kind(H/T) Refugees"),
    KIND_HT_ELL_COUNT("Headcount Kind(H/T) ELL"),

    KIND_FT_COUNT("Headcount Kind(F/T) New CE/OL"),
    KIND_FT_REFUGEE_COUNT("Headcount Kind(F/T) Refugees"),
    KIND_FT_ELL_COUNT("Headcount Kind(F/T) ELL"),

    GRADE_01_COUNT("Headcount Grade 1 New CE/OL"),
    GRADE_01_REFUGEE_COUNT("Headcount Grade 1 Refugees"),
    GRADE_01_ELL_COUNT("Headcount Grade 1 ELL"),

    GRADE_02_COUNT("Headcount Grade 2 New CE/OL"),
    GRADE_02_REFUGEE_COUNT("Headcount Grade 2 Refugees"),
    GRADE_02_ELL_COUNT("Headcount Grade 2 ELL"),

    GRADE_03_COUNT("Headcount Grade 3 New CE/OL"),
    GRADE_03_REFUGEE_COUNT("Headcount Grade 3 Refugees"),
    GRADE_03_ELL_COUNT("Headcount Grade 3 ELL"),

    GRADE_04_COUNT("Headcount Grade 4 New CE/OL"),
    GRADE_04_REFUGEE_COUNT("Headcount Grade 4 Refugees"),
    GRADE_04_ELL_COUNT("Headcount Grade 4 ELL"),

    GRADE_05_COUNT("Headcount Grade 5 New CE/OL"),
    GRADE_05_REFUGEE_COUNT("Headcount Grade 5 Refugees"),
    GRADE_05_ELL_COUNT("Headcount Grade 5 ELL"),

    GRADE_06_COUNT("Headcount Grade 6 New CE/OL"),
    GRADE_06_REFUGEE_COUNT("Headcount Grade 6 Refugees"),
    GRADE_06_ELL_COUNT("Headcount Grade 6 ELL"),

    GRADE_07_COUNT("Headcount Grade 7 New CE/OL"),
    GRADE_07_REFUGEE_COUNT("Headcount Grade 7 Refugees"),
    GRADE_07_ELL_COUNT("Headcount Grade 7 ELL"),

    GRADE_08_COUNT("Headcount Grade 8 New CE/OL"),
    GRADE_08_REFUGEE_COUNT("Headcount Grade 8 Refugees"),
    GRADE_08_ELL_COUNT("Headcount Grade 8 ELL"),

    GRADE_09_COUNT("Headcount Grade 9 New CE/OL"),
    GRADE_09_REFUGEE_COUNT("Headcount Grade 9 Refugees"),
    GRADE_09_ELL_COUNT("Headcount Grade 9 ELL"),

    GRADE_10_COUNT("Headcount Grade 10 New CE/OL"),
    GRADE_10_REFUGEE_COUNT("Headcount Grade 10 Refugees"),
    GRADE_10_ELL_COUNT("Headcount Grade 10 ELL"),

    GRADE_11_COUNT("Headcount Grade 11 New CE/OL"),
    GRADE_11_REFUGEE_COUNT("Headcount Grade 11 Refugees"),
    GRADE_11_ELL_COUNT("Headcount Grade 11 ELL"),

    GRADE_12_COUNT("Headcount Grade 12 New CE/OL"),
    GRADE_12_REFUGEE_COUNT("Headcount Grade 12 Refugees"),
    GRADE_12_ELL_COUNT("Headcount Grade 12 ELL"),

    GRADE_EU_COUNT("Headcount Grade EU New CE/OL"),
    GRADE_EU_REFUGEE_COUNT("Headcount Grade EU Refugees"),
    GRADE_EU_ELL_COUNT("Headcount Grade EU ELL"),

    GRADE_SU_COUNT("Headcount Grade SU New CE/OL"),
    GRADE_SU_REFUGEE_COUNT("Headcount Grade SU Refugees"),
    GRADE_SU_ELL_COUNT("Headcount Grade SU ELL"),

    GRAD_ADULT_COUNT("Headcount Grad Adult New CE/OL"),
    NON_GRAD_ADULT_COUNT("Headcount Non-Grad Adult New CE/OL"),

    KIND_HT_FTE_TOTAL("Funded FTE Kind(H/T) New CE/OL"),
    KIND_HT_FTE_REFUGEE_TOTAL("Funded FTE Kind(H/T) Refugees"),

    GRADE_ONE_FTE_TOTAL("Funded FTE Grade 1 New CE/OL"),
    GRADE_ONE_FTE_REFUGEE_TOTAL("Funded FTE Grade 1 Refugees"),

    GRADE_TWO_FTE_TOTAL("Funded FTE Grade 2 New CE/OL"),
    GRADE_TWO_FTE_REFUGEE_TOTAL("Funded FTE Grade 2 Refugees"),

    GRADE_THREE_FTE_TOTAL("Funded FTE Grade 3 New CE/OL"),
    GRADE_THREE_FTE_REFUGEE_TOTAL("Funded FTE Grade 3 Refugees"),

    GRADE_FOUR_FTE_TOTAL("Funded FTE Grade 4 New CE/OL"),
    GRADE_FOUR_FTE_REFUGEE_TOTAL("Funded FTE Grade 4 Refugees"),

    GRADE_FIVE_FTE_TOTAL("Funded FTE Grade 5 New CE/OL"),
    GRADE_FIVE_FTE_REFUGEE_TOTAL("Funded FTE Grade 5 Refugees"),

    GRADE_SIX_FTE_TOTAL("Funded FTE Grade 6 New CE/OL"),
    GRADE_SIX_FTE_REFUGEE_TOTAL("Funded FTE Grade 6 Refugees"),

    GRADE_SEVEN_FTE_TOTAL("Funded FTE Grade 7 New CE/OL"),
    GRADE_SEVEN_FTE_REFUGEE_TOTAL("Funded FTE Grade 7 Refugees"),

    GRADE_EIGHT_FTE_TOTAL("Funded FTE Grade 8 New CE/OL"),
    GRADE_EIGHT_FTE_REFUGEE_TOTAL("Funded FTE Grade 8 Refugees"),

    GRADE_NINE_FTE_TOTAL("Funded FTE Grade 9 New CE/OL"),
    GRADE_NINE_FTE_REFUGEE_TOTAL("Funded FTE Grade 9 Refugees"),

    GRADE_TEN_FTE_TOTAL("Funded FTE Grade 10 New CE/OL"),
    GRADE_TEN_FTE_REFUGEE_TOTAL("Funded FTE Grade 10 Refugees"),

    GRADE_ELEVEN_FTE_TOTAL("Funded FTE Grade 11 New CE/OL"),
    GRADE_ELEVEN_FTE_REFUGEE_TOTAL("Funded FTE Grade 11 Refugees"),

    GRADE_TWELVE_FTE_TOTAL("Funded FTE Grade 12 New CE/OL"),
    GRADE_TWELVE_FTE_REFUGEE_TOTAL("Funded FTE Grade 12 Refugees"),

    EU_FTE_TOTAL("Funded FTE EU New CE/OL"),
    EU_FTE_REFUGEE_TOTAL("Funded FTE EU Refugees"),

    SU_FTE_TOTAL("Funded FTE SU New CE/OL"),
    SU_FTE_REFUGEE_TOTAL("Funded FTE SU Refugees"),

    GRAD_ADULT_FTE_TOTAL("Funded FTE Grad Adult New CE/OL"),
    NON_GRAD_ADULT_FTE_TOTAL("Funded FTE Non-Grad New CE/OL")
    ;

    private final String code;
    CEAndOLEnrolmentAndFteHeader(String code) { this.code = code; }
}
