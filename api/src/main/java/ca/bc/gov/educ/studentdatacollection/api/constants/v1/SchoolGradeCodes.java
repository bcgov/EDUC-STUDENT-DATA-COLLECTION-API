package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum SchoolGradeCodes {
    HOMESCHOOL("HS"),
    KINDHALF("KH"),
    KINDFULL("KF"),
    GRADE01("01"),
    GRADE02("02"),
    GRADE03("03"),
    GRADE04("04"),
    GRADE05("05"),
    GRADE06("06"),
    GRADE07("07"),
    ELEMUNGR("EU"),
    GRADE08("08"),
    GRADE09("09"),
    GRADE10("10"),
    GRADE11("11"),
    GRADE12("12"),
    SECONDARY_UNGRADED("SU"),
    GRADUATED_ADULT("GA");

    @Getter
    private final String code;
    SchoolGradeCodes(String code) {
        this.code = code;
    }

    /**
     * Get all grades HS, K-9, and EU
     * @return - all grades HS, K-9, and EU
     */
    public static List<String> getDistrictFundingGrades() {
        List<String> districtFundingGrades = new ArrayList<>();
        districtFundingGrades.add(HOMESCHOOL.getCode());
        districtFundingGrades.addAll(getKToNineGrades());
        return districtFundingGrades;
    }

    public static List<String> getKfOneToSevenEuGrades() {
        List<String> districtFundingGrades = new ArrayList<>();
        districtFundingGrades.add(KINDFULL.getCode());
        districtFundingGrades.add(GRADE01.getCode());
        districtFundingGrades.add(GRADE02.getCode());
        districtFundingGrades.add(GRADE03.getCode());
        districtFundingGrades.add(GRADE04.getCode());
        districtFundingGrades.add(GRADE05.getCode());
        districtFundingGrades.add(GRADE06.getCode());
        districtFundingGrades.add(GRADE07.getCode());
        districtFundingGrades.add(ELEMUNGR.getCode());
        return districtFundingGrades;
    }

    public static List<String> getKToNineGrades() {
        List<String> districtFundingGrades = new ArrayList<>();
        districtFundingGrades.add(KINDHALF.getCode());
        districtFundingGrades.add(KINDFULL.getCode());
        districtFundingGrades.add(GRADE01.getCode());
        districtFundingGrades.add(GRADE02.getCode());
        districtFundingGrades.add(GRADE03.getCode());
        districtFundingGrades.add(GRADE04.getCode());
        districtFundingGrades.add(GRADE05.getCode());
        districtFundingGrades.add(GRADE06.getCode());
        districtFundingGrades.add(GRADE07.getCode());
        districtFundingGrades.add(ELEMUNGR.getCode());
        districtFundingGrades.add(GRADE08.getCode());
        districtFundingGrades.add(GRADE09.getCode());
        return districtFundingGrades;
    }
    public static List<String> get1To7Grades() {
        List<String> districtFundingGrades = new ArrayList<>();
        districtFundingGrades.add(GRADE01.getCode());
        districtFundingGrades.add(GRADE02.getCode());
        districtFundingGrades.add(GRADE03.getCode());
        districtFundingGrades.add(GRADE04.getCode());
        districtFundingGrades.add(GRADE05.getCode());
        districtFundingGrades.add(GRADE06.getCode());
        districtFundingGrades.add(GRADE07.getCode());
        return districtFundingGrades;
    }

    public static List<String> get9To12Grades() {
        List<String> districtFundingGrades = new ArrayList<>();
        districtFundingGrades.add(GRADE08.getCode());
        districtFundingGrades.add(GRADE09.getCode());
        districtFundingGrades.add(GRADE10.getCode());
        districtFundingGrades.add(GRADE11.getCode());
        districtFundingGrades.add(GRADE12.getCode());
        return districtFundingGrades;
    }

    public static List<String> getHighSchoolGrades() {
        List<String> districtFundingGrades = new ArrayList<>();
        districtFundingGrades.add(GRADE10.getCode());
        districtFundingGrades.add(GRADE11.getCode());
        districtFundingGrades.add(GRADE12.getCode());
        districtFundingGrades.add(SECONDARY_UNGRADED.getCode());
        return districtFundingGrades;
    }
}
