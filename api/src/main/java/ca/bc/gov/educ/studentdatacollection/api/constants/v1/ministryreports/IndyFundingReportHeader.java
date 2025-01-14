package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum IndyFundingReportHeader {

    DISTRICT_NUMBER("District Number"),
    DISTRICT_NAME("District Name"),
    AUTHORITY_NUMBER("Authority Number"),
    AUTHORITY_NAME("Authority Name"),
    SCHOOL_NUMBER("School Number"),
    SCHOOL_NAME("School Name"),
    FACILITY_TYPE("Facility Type"),

    KIND_HT_FUNDING_GROUP("Kind(H/T) Funding Group"),
    KIND_FT_FUNDING_GROUP("Kind(F/T) Funding Group"),
    GRADE_01_FUNDING_GROUP("Grade 1 Funding Group"),
    GRADE_02_FUNDING_GROUP("Grade 2 Funding Group"),
    GRADE_03_FUNDING_GROUP("Grade 3 Funding Group"),
    GRADE_04_FUNDING_GROUP("Grade 4 Funding Group"),
    GRADE_05_FUNDING_GROUP("Grade 5 Funding Group"),
    GRADE_06_FUNDING_GROUP("Grade 6 Funding Group"),
    GRADE_07_FUNDING_GROUP("Grade 7 Funding Group"),
    GRADE_EU_FUNDING_GROUP("Grade EU Funding Group"),
    GRADE_08_FUNDING_GROUP("Grade 8 Funding Group"),
    GRADE_09_FUNDING_GROUP("Grade 9 Funding Group"),
    GRADE_10_FUNDING_GROUP("Grade 10 Funding Group"),
    GRADE_11_FUNDING_GROUP("Grade 11 Funding Group"),
    GRADE_12_FUNDING_GROUP("Grade 12 Funding Group"),
    GRADE_SU_FUNDING_GROUP("Grade SU Funding Group"),
    GRADE_GA_FUNDING_GROUP("Grade GA Funding Group"),
    GRADE_HS_FUNDING_GROUP("Grade HS Funding Group"),

    TOTAL_HEADCOUNT_NO_ADULTS("Total Headcount No Adults"),
    TOTAL_FTE_NO_ADULTS("Total FTE No Adults"),

    KIND_HT_HEADCOUNT_NO_ADULTS("Headcount Kind(H/T) No Adults"),
    KIND_FT_HEADCOUNT_NO_ADULTS("Headcount Kind(F/T) No Adults"),
    GRADE_01_HEADCOUNT_NO_ADULTS("Headcount Grade 1 No Adults"),
    GRADE_02_HEADCOUNT_NO_ADULTS("Headcount Grade 2 No Adults"),
    GRADE_03_HEADCOUNT_NO_ADULTS("Headcount Grade 3 No Adults"),
    GRADE_04_HEADCOUNT_NO_ADULTS("Headcount Grade 4 No Adults"),
    GRADE_05_HEADCOUNT_NO_ADULTS("Headcount Grade 5 No Adults"),
    GRADE_06_HEADCOUNT_NO_ADULTS("Headcount Grade 6 No Adults"),
    GRADE_07_HEADCOUNT_NO_ADULTS("Headcount Grade 7 No Adults"),
    GRADE_EU_HEADCOUNT_NO_ADULTS("Headcount Grade EU No Adults"),
    GRADE_08_HEADCOUNT_NO_ADULTS("Headcount Grade 8 No Adults"),
    GRADE_09_HEADCOUNT_NO_ADULTS("Headcount Grade 9 No Adults"),
    GRADE_10_HEADCOUNT_NO_ADULTS("Headcount Grade 10 No Adults"),
    GRADE_11_HEADCOUNT_NO_ADULTS("Headcount Grade 11 No Adults"),
    GRADE_12_HEADCOUNT_NO_ADULTS("Headcount Grade 12 No Adults"),
    GRADE_SU_HEADCOUNT_NO_ADULTS("Headcount Grade SU No Adults"),
    GRAD_ADULT_HEADCOUNT_NO_ADULTS("Headcount Grad Adult No Adults"),
    GRADE_HS_HEADCOUNT_NO_ADULTS("Headcount Homeschool No Adults"),

    KIND_HT_FTE_COUNT_NO_ADULTS("FTE Kind(H/T) No Adults"),
    KIND_FT_FTE_COUNT_NO_ADULTS("FTE Kind(F/T) No Adults"),
    GRADE_ONE_FTE_COUNT_NO_ADULTS("FTE Grade 1 No Adults"),
    GRADE_TWO_FTE_COUNT_NO_ADULTS("FTE Grade 2 No Adults"),
    GRADE_THREE_FTE_COUNT_NO_ADULTS("FTE Grade 3 No Adults"),
    GRADE_FOUR_FTE_COUNT_NO_ADULTS("FTE Grade 4 No Adults"),
    GRADE_FIVE_FTE_COUNT_NO_ADULTS("FTE Grade 5 No Adults"),
    GRADE_SIX_FTE_COUNT_NO_ADULTS("FTE Grade 6 No Adults"),
    GRADE_SEVEN_FTE_COUNT_NO_ADULTS("FTE Grade 7 No Adults"),
    EU_FTE_COUNT_NO_ADULTS("FTE EU No Adults"),
    GRADE_EIGHT_FTE_COUNT_NO_ADULTS("FTE Grade 8 No Adults"),
    GRADE_NINE_FTE_COUNT_NO_ADULTS("FTE Grade 9 No Adults"),
    GRADE_TEN_FTE_COUNT_NO_ADULTS("FTE Grade 10 No Adults"),
    GRADE_ELEVEN_FTE_COUNT_NO_ADULTS("FTE Grade 11 No Adults"),
    GRADE_TWELVE_FTE_COUNT_NO_ADULTS("FTE Grade 12 No Adults"),
    SU_FTE_COUNT_NO_ADULTS("FTE SU No Adults"),
    GA_FTE_COUNT_NO_ADULTS("FTE GA No Adults"),

    KIND_HT_HEADCOUNT_ADULTS("Headcount Kind(H/T) Adults"),
    KIND_FT_HEADCOUNT_ADULTS("Headcount Kind(F/T) Adults"),
    GRADE_01_HEADCOUNT_ADULTS("Headcount Grade 1 Adults"),
    GRADE_02_HEADCOUNT_ADULTS("Headcount Grade 2 Adults"),
    GRADE_03_HEADCOUNT_ADULTS("Headcount Grade 3 Adults"),
    GRADE_04_HEADCOUNT_ADULTS("Headcount Grade 4 Adults"),
    GRADE_05_HEADCOUNT_ADULTS("Headcount Grade 5 Adults"),
    GRADE_06_HEADCOUNT_ADULTS("Headcount Grade 6 Adults"),
    GRADE_07_HEADCOUNT_ADULTS("Headcount Grade 7 Adults"),
    GRADE_EU_HEADCOUNT_ADULTS("Headcount Grade EU Adults"),
    GRADE_08_HEADCOUNT_ADULTS("Headcount Grade 8 Adults"),
    GRADE_09_HEADCOUNT_ADULTS("Headcount Grade 9 Adults"),
    GRADE_10_HEADCOUNT_ADULTS("Headcount Grade 10 Adults"),
    GRADE_11_HEADCOUNT_ADULTS("Headcount Grade 11 Adults"),
    GRADE_12_HEADCOUNT_ADULTS("Headcount Grade 12 Adults"),
    GRADE_SU_HEADCOUNT_ADULTS("Headcount Grade SU Adults"),
    GRAD_ADULT_HEADCOUNT_ADULTS("Headcount Grad Adult Adults"),
    GRADE_HS_HEADCOUNT_ADULTS("Headcount Homeschool Adults"),

    KIND_HT_FTE_COUNT_ADULTS("FTE Kind(H/T) Adults"),
    KIND_FT_FTE_COUNT_ADULTS("FTE Kind(F/T) Adults"),
    GRADE_ONE_FTE_COUNT_ADULTS("FTE Grade 1 Adults"),
    GRADE_TWO_FTE_COUNT_ADULTS("FTE Grade 2 Adults"),
    GRADE_THREE_FTE_COUNT_ADULTS("FTE Grade 3 Adults"),
    GRADE_FOUR_FTE_COUNT_ADULTS("FTE Grade 4 Adults"),
    GRADE_FIVE_FTE_COUNT_ADULTS("FTE Grade 5 Adults"),
    GRADE_SIX_FTE_COUNT_ADULTS("FTE Grade 6 Adults"),
    GRADE_SEVEN_FTE_COUNT_ADULTS("FTE Grade 7 Adults"),
    EU_FTE_COUNT_ADULTS("FTE EU Adults"),
    GRADE_EIGHT_FTE_COUNT_ADULTS("FTE Grade 8 Adults"),
    GRADE_NINE_FTE_COUNT_ADULTS("FTE Grade 9 Adults"),
    GRADE_TEN_FTE_COUNT_ADULTS("FTE Grade 10 Adults"),
    GRADE_ELEVEN_FTE_COUNT_ADULTS("FTE Grade 11 Adults"),
    GRADE_TWELVE_FTE_COUNT_ADULTS("FTE Grade 12 Adults"),
    SU_FTE_COUNT_ADULTS("FTE SU Adults"),
    GA_FTE_COUNT_ADULTS("FTE GA Adults");

    private final String code;
    IndyFundingReportHeader(String code) { this.code = code; }

    public static String[] getAllValuesAsStringArray(){
        return Arrays.stream(IndyFundingReportHeader.values()).map(IndyFundingReportHeader::getCode).toArray(String[]::new);
    }
}
