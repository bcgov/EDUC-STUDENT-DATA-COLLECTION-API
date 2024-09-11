package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;
import lombok.Getter;

@Getter
public enum IndyFundingReportHeader {

    DISTRICT_NUMBER("District Number"),
    DISTRICT_NAME("District Name"),
    AUTHORITY_NUMBER("Authority Number"),
    AUTHORITY_NAME("Authority Name"),
    SCHOOL_NUMBER("School Number"),
    SCHOOL_NAME("School Name"),
    FUNDING_GROUP("Funding Group"),

    TOTAL_HEADCOUNT("Total Headcount"),
    TOTAL_FTE("Total FTE"),

    KIND_HT_HEADCOUNT("Headcount Kind(H/T)"),
    GRADE_01_HEADCOUNT("Headcount Grade 1"),
    GRADE_02_HEADCOUNT("Headcount Grade 2"),
    GRADE_03_HEADCOUNT("Headcount Grade 3"),
    GRADE_04_HEADCOUNT("Headcount Grade 4"),
    GRADE_05_HEADCOUNT("Headcount Grade 5"),
    GRADE_06_HEADCOUNT("Headcount Grade 6"),
    GRADE_07_HEADCOUNT("Headcount Grade 7"),
    GRADE_08_HEADCOUNT("Headcount Grade 8"),
    GRADE_09_HEADCOUNT("Headcount Grade 9"),
    GRADE_10_HEADCOUNT("Headcount Grade 10"),
    GRADE_11_HEADCOUNT("Headcount Grade 11"),
    GRADE_12_HEADCOUNT("Headcount Grade 12"),
    GRADE_EU_HEADCOUNT("Headcount Grade EU"),
    GRADE_SU_HEADCOUNT("Headcount Grade SU"),
    GRADE_HS_HEADCOUNT("Headcount Homeschool"),
    GRAD_ADULT_HEADCOUNT("Headcount Grad Adult"),
    NON_GRAD_ADULT_HEADCOUNT("Headcount Non-Grad Adult"),

    KIND_HT_FTE_COUNT("FTE Kind(H/T)"),
    GRADE_ONE_FTE_COUNT("FTE Grade 1"),
    GRADE_TWO_FTE_COUNT("FTE Grade 2"),
    GRADE_THREE_FTE_COUNT("FTE Grade 3"),
    GRADE_FOUR_FTE_COUNT("FTE Grade 4"),
    GRADE_FIVE_FTE_COUNT("FTE Grade 5"),
    GRADE_SIX_FTE_COUNT("FTE Grade 6"),
    GRADE_SEVEN_FTE_COUNT("FTE Grade 7"),
    GRADE_EIGHT_FTE_COUNT("FTE Grade 8"),
    GRADE_NINE_FTE_COUNT("FTE Grade 9"),
    GRADE_TEN_FTE_COUNT("FTE Grade 10"),
    GRADE_ELEVEN_FTE_COUNT("FTE Grade 11"),
    GRADE_TWELVE_FTE_COUNT("FTE Grade 12"),
    EU_FTE_COUNT("FTE EU"),
    SU_FTE_COUNT("FTE SU");

    private final String code;
    IndyFundingReportHeader(String code) { this.code = code; }
}
