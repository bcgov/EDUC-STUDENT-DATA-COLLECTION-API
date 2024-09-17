package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum OffshoreSchoolEnrolmentHeadcountHeader {

    SCHOOL("School Name"),
    KIND_FT("KF"),
    GRADE_01("01"),
    GRADE_02("02"),
    GRADE_03("03"),
    GRADE_04("04"),
    GRADE_05("05"),
    GRADE_06("06"),
    GRADE_07("07"),
    GRADE_EU("EU"),
    GRADE_08("08"),
    GRADE_09("09"),
    GRADE_10("10"),
    GRADE_11("11"),
    GRADE_12("12"),
    GRADE_SU("SU"),
    GRADE_GA("GA"),
    GRADE_HS("HS"),
    TOTAL("Total");

    private final String code;
    OffshoreSchoolEnrolmentHeadcountHeader(String code) { this.code = code; }
}
