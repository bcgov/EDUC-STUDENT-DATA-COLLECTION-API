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
    FUNDING_GROUP("Facility Type"),

    TOTAL_HEADCOUNT("Total Headcount"),
    TOTAL_FTE("Total FTE"),

    KIND_HT_HEADCOUNT("Headcount Kind(H/T)"),
    KIND_FT_HEADCOUNT("Headcount Kind(F/T)"),
    GRADE_01_HEADCOUNT("Headcount Grade 1"),
    GRADE_02_HEADCOUNT("Headcount Grade 2"),
    GRADE_03_HEADCOUNT("Headcount Grade 3"),
    GRADE_04_HEADCOUNT("Headcount Grade 4"),
    GRADE_05_HEADCOUNT("Headcount Grade 5"),
    GRADE_06_HEADCOUNT("Headcount Grade 6"),
    GRADE_07_HEADCOUNT("Headcount Grade 7"),
    GRADE_EU_HEADCOUNT("Headcount Grade EU"),
    GRADE_08_HEADCOUNT("Headcount Grade 8"),
    GRADE_09_HEADCOUNT("Headcount Grade 9"),
    GRADE_10_HEADCOUNT("Headcount Grade 10"),
    GRADE_11_HEADCOUNT("Headcount Grade 11"),
    GRADE_12_HEADCOUNT("Headcount Grade 12"),
    GRADE_SU_HEADCOUNT("Headcount Grade SU"),
    GRAD_ADULT_HEADCOUNT("Headcount Grad Adult"),
    GRADE_HS_HEADCOUNT("Headcount Homeschool"),

    KIND_HT_FTE_COUNT("FTE Kind(H/T)"),
    KIND_FT_FTE_COUNT("FTE Kind(F/T)"),
    GRADE_ONE_FTE_COUNT("FTE Grade 1"),
    GRADE_TWO_FTE_COUNT("FTE Grade 2"),
    GRADE_THREE_FTE_COUNT("FTE Grade 3"),
    GRADE_FOUR_FTE_COUNT("FTE Grade 4"),
    GRADE_FIVE_FTE_COUNT("FTE Grade 5"),
    GRADE_SIX_FTE_COUNT("FTE Grade 6"),
    GRADE_SEVEN_FTE_COUNT("FTE Grade 7"),
    EU_FTE_COUNT("FTE EU"),
    GRADE_EIGHT_FTE_COUNT("FTE Grade 8"),
    GRADE_NINE_FTE_COUNT("FTE Grade 9"),
    GRADE_TEN_FTE_COUNT("FTE Grade 10"),
    GRADE_ELEVEN_FTE_COUNT("FTE Grade 11"),
    GRADE_TWELVE_FTE_COUNT("FTE Grade 12"),
    SU_FTE_COUNT("FTE SU"),
    GA_FTE_COUNT("FTE GA"),

    KIND_HT_FUNDING_GROUP("Kind(H/T) Funding Group #"),
    KIND_FT_FUNDING_GROUP("Kind(F/T) Funding Group #"),
    GRADE_01_FUNDING_GROUP("Grade 1 Funding Group #"),
    GRADE_02_FUNDING_GROUP("Grade 2 Funding Group #"),
    GRADE_03_FUNDING_GROUP("Grade 3 Funding Group #"),
    GRADE_04_FUNDING_GROUP("Grade 4 Funding Group #"),
    GRADE_05_FUNDING_GROUP("Grade 5 Funding Group #"),
    GRADE_06_FUNDING_GROUP("Grade 6 Funding Group #"),
    GRADE_07_FUNDING_GROUP("Grade 7 Funding Group #"),
    GRADE_EU_FUNDING_GROUP("Grade EU Funding Group #"),
    GRADE_08_FUNDING_GROUP("Grade 8 Funding Group #"),
    GRADE_09_FUNDING_GROUP("Grade 9 Funding Group #"),
    GRADE_10_FUNDING_GROUP("Grade 10 Funding Group #"),
    GRADE_11_FUNDING_GROUP("Grade 11 Funding Group #"),
    GRADE_12_FUNDING_GROUP("Grade 12 Funding Group #"),
    GRADE_SU_FUNDING_GROUP("Grade SU Funding Group #"),
    GRADE_GA_FUNDING_GROUP("Grade GA Funding Group #"),
    GRADE_HS_FUNDING_GROUP("Grade HS Funding Group #");

    private final String code;
    IndyFundingReportHeader(String code) { this.code = code; }
}
