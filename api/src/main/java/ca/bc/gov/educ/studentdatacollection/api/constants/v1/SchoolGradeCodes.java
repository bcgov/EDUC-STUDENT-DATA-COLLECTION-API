package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public enum SchoolGradeCodes {
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
    GRADUATED_ADULT("GA"),
    HOMESCHOOL("HS");

    @Getter
    private final String code;
    SchoolGradeCodes(String code) {
        this.code = code;
    }

    public static Optional<SchoolGradeCodes> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }

    /**
     * Get all grades HS, K-9, and EU
     * @return - all grades HS, K-9, and EU
     */
    public static List<String> getDistrictFundingGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(HOMESCHOOL.getCode());
        codes.addAll(getKToNineGrades());
        return codes;
    }

    public static List<String> getKfOneToSevenEuGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(KINDFULL.getCode());
        codes.add(GRADE01.getCode());
        codes.add(GRADE02.getCode());
        codes.add(GRADE03.getCode());
        codes.add(GRADE04.getCode());
        codes.add(GRADE05.getCode());
        codes.add(GRADE06.getCode());
        codes.add(GRADE07.getCode());
        codes.add(ELEMUNGR.getCode());
        return codes;
    }

    public static List<String> getKToSevenEuGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(KINDHALF.getCode());
        codes.add(KINDFULL.getCode());
        codes.add(GRADE01.getCode());
        codes.add(GRADE02.getCode());
        codes.add(GRADE03.getCode());
        codes.add(GRADE04.getCode());
        codes.add(GRADE05.getCode());
        codes.add(GRADE06.getCode());
        codes.add(GRADE07.getCode());
        codes.add(ELEMUNGR.getCode());
        return codes;
    }

    public static List<String> getKToNineGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(KINDHALF.getCode());
        codes.add(KINDFULL.getCode());
        codes.add(GRADE01.getCode());
        codes.add(GRADE02.getCode());
        codes.add(GRADE03.getCode());
        codes.add(GRADE04.getCode());
        codes.add(GRADE05.getCode());
        codes.add(GRADE06.getCode());
        codes.add(GRADE07.getCode());
        codes.add(ELEMUNGR.getCode());
        codes.add(GRADE08.getCode());
        codes.add(GRADE09.getCode());
        return codes;
    }
    public static List<String> get1To7Grades() {
        List<String> codes = new ArrayList<>();
        codes.add(GRADE01.getCode());
        codes.add(GRADE02.getCode());
        codes.add(GRADE03.getCode());
        codes.add(GRADE04.getCode());
        codes.add(GRADE05.getCode());
        codes.add(GRADE06.getCode());
        codes.add(GRADE07.getCode());
        return codes;
    }

    public static List<String> get8To12Grades() {
        List<String> codes = new ArrayList<>();
        codes.add(GRADE08.getCode());
        codes.add(GRADE09.getCode());
        codes.add(GRADE10.getCode());
        codes.add(GRADE11.getCode());
        codes.add(GRADE12.getCode());
        return codes;
    }

    public static List<String> get8PlusGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(GRADE08.getCode());
        codes.add(GRADE09.getCode());
        codes.add(GRADE10.getCode());
        codes.add(GRADE11.getCode());
        codes.add(GRADE12.getCode());
        codes.add(SECONDARY_UNGRADED.getCode());
        codes.add(GRADUATED_ADULT.getCode());
        return codes;
    }

    public static List<String> get8PlusGradesNoGA() {
        List<String> codes = new ArrayList<>();
        codes.add(GRADE08.getCode());
        codes.add(GRADE09.getCode());
        codes.add(GRADE10.getCode());
        codes.add(GRADE11.getCode());
        codes.add(GRADE12.getCode());
        codes.add(SECONDARY_UNGRADED.getCode());
        return codes;
    }

    public static List<String> getHighSchoolGrades() {
        return getGrades10toSU();
    }

    public static List<String> getAllowedAdultGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(GRADE10.getCode());
        codes.add(GRADE11.getCode());
        codes.add(GRADE12.getCode());
        codes.add(SECONDARY_UNGRADED.getCode());
        codes.add(GRADUATED_ADULT.getCode());
        return codes;
    }

    public static List<String> getAllowedAdultGradesNonGraduate() {
        return getGrades10toSU();
    }

    public static List<String> getSummerSchoolGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(GRADE01.getCode());
        codes.add(GRADE02.getCode());
        codes.add(GRADE03.getCode());
        codes.add(GRADE04.getCode());
        codes.add(GRADE05.getCode());
        codes.add(GRADE06.getCode());
        codes.add(GRADE07.getCode());
        codes.add(GRADE08.getCode());
        codes.add(GRADE09.getCode());
        codes.add(GRADE10.getCode());
        codes.add(GRADE11.getCode());
        codes.add(GRADE12.getCode());
        return codes;
    }

    public static List<String> getSupportBlockGrades() {
        return getGrades10toSU();
    }

    public static List<String> getGrades10toSU() {
        List<String> codes = new ArrayList<>();
        codes.add(GRADE10.getCode());
        codes.add(GRADE11.getCode());
        codes.add(GRADE12.getCode());
        codes.add(SECONDARY_UNGRADED.getCode());
        return codes;
    }

    public static List<String> getGrades8and9() {
        List<String> codes = new ArrayList<>();
        codes.add(GRADE08.getCode());
        codes.add(GRADE09.getCode());
        return codes;
    }

    public static List<String> getAllSchoolGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(KINDHALF.getCode());
        codes.add(KINDFULL.getCode());
        codes.add(GRADE01.getCode());
        codes.add(GRADE02.getCode());
        codes.add(GRADE03.getCode());
        codes.add(GRADE04.getCode());
        codes.add(GRADE05.getCode());
        codes.add(GRADE06.getCode());
        codes.add(GRADE07.getCode());
        codes.add(ELEMUNGR.getCode());
        codes.add(GRADE08.getCode());
        codes.add(GRADE09.getCode());
        codes.add(GRADE10.getCode());
        codes.add(GRADE11.getCode());
        codes.add(GRADE12.getCode());
        codes.add(SECONDARY_UNGRADED.getCode());
        codes.add(GRADUATED_ADULT.getCode());
        codes.add(HOMESCHOOL.getCode());
        return codes;
    }

    public static List<String> getAllSchoolGradesExcludingHS(){
        List<String> codes = new ArrayList<>();
        codes.add(KINDHALF.getCode());
        codes.add(KINDFULL.getCode());
        codes.add(GRADE01.getCode());
        codes.add(GRADE02.getCode());
        codes.add(GRADE03.getCode());
        codes.add(GRADE04.getCode());
        codes.add(GRADE05.getCode());
        codes.add(GRADE06.getCode());
        codes.add(GRADE07.getCode());
        codes.add(ELEMUNGR.getCode());
        codes.add(GRADE08.getCode());
        codes.add(GRADE09.getCode());
        codes.add(GRADE10.getCode());
        codes.add(GRADE11.getCode());
        codes.add(GRADE12.getCode());
        codes.add(SECONDARY_UNGRADED.getCode());
        codes.add(GRADUATED_ADULT.getCode());
        return codes;
    }

    public static List<String> getNonIndependentSchoolGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(KINDFULL.getCode());
        codes.add(GRADE01.getCode());
        codes.add(GRADE02.getCode());
        codes.add(GRADE03.getCode());
        codes.add(GRADE04.getCode());
        codes.add(GRADE05.getCode());
        codes.add(GRADE06.getCode());
        codes.add(GRADE07.getCode());
        codes.add(ELEMUNGR.getCode());
        codes.add(GRADE08.getCode());
        codes.add(GRADE09.getCode());
        codes.add(GRADE10.getCode());
        codes.add(GRADE11.getCode());
        codes.add(GRADE12.getCode());
        codes.add(SECONDARY_UNGRADED.getCode());
        codes.add(GRADUATED_ADULT.getCode());
        codes.add(HOMESCHOOL.getCode());
        return codes;
    }
    public static List<String> getNonIndependentKtoSUGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(KINDFULL.getCode());
        codes.add(GRADE01.getCode());
        codes.add(GRADE02.getCode());
        codes.add(GRADE03.getCode());
        codes.add(GRADE04.getCode());
        codes.add(GRADE05.getCode());
        codes.add(GRADE06.getCode());
        codes.add(GRADE07.getCode());
        codes.add(ELEMUNGR.getCode());
        codes.add(GRADE08.getCode());
        codes.add(GRADE09.getCode());
        codes.add(GRADE10.getCode());
        codes.add(GRADE11.getCode());
        codes.add(GRADE12.getCode());
        codes.add(SECONDARY_UNGRADED.getCode());
        return codes;
    }

    public static List<String> getIndependentKtoSUGrades() {
        List<String> codes = getKToNineGrades();
        codes.addAll(getGrades10toSU());
        return codes;
    }

    public static List<String> getNonIndependentKtoGAGrades() {
        List<String> codes = new ArrayList<>(getNonIndependentKtoSUGrades());
        codes.add(GRADUATED_ADULT.getCode());
        return codes;
    }

    public static List<String> getIndependentKtoGAGrades() {
        List<String> codes = new ArrayList<>();
        codes.add(KINDHALF.getCode());
        codes.addAll(getNonIndependentKtoGAGrades());
        return codes;
    }
}
