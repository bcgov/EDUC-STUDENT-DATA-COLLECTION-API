package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import java.util.Arrays;

public enum DuplicateClassLookup {

    ENTRY1(new String[]{FacilityTypeCodes.ALT_PROGS.getCode()},
            SchoolCategoryCodes.getActiveSchoolCategoryCodes(),
            SchoolGradeCodes.getAllSchoolGrades().toArray(new String[0])),

    ENTRY2(FacilityTypeCodes.getFacilityCodesWithoutOLAndCE(),
            new String[]{SchoolCategoryCodes.PUBLIC.getCode()},
            SchoolGradeCodes.getAllSchoolGradesExcludingHS().toArray(new String[0])),

    ENTRY3(new String[]{FacilityTypeCodes.CONT_ED.getCode()},
            SchoolCategoryCodes.getActiveSchoolCategoryCodes(),
            SchoolGradeCodes.getAllSchoolGradesExcludingHS().toArray(new String[0])),

    ENTRY4(FacilityTypeCodes.getFacilityCodesWithoutOLAndCE(),
            new String[]{SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.INDP_FNS.getCode()},
            SchoolGradeCodes.getAllSchoolGradesExcludingHS().toArray(new String[0])),

    ENTRY5(new String[]{FacilityTypeCodes.DIST_LEARN.getCode(), FacilityTypeCodes.DIST_LEARN.getCode()},
            new String[]{SchoolCategoryCodes.PUBLIC.getCode()},
            SchoolGradeCodes.getKToNineGrades().toArray(new String[0])),

    ENTRY6(new String[]{FacilityTypeCodes.DIST_LEARN.getCode(), FacilityTypeCodes.DISTONLINE.getCode()},
            new String[]{SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.INDP_FNS.getCode()},
            SchoolGradeCodes.getKToNineGrades().toArray(new String[0])),

    ENTRY7(FacilityTypeCodes.getFacilityCodesWithoutOLAndCE(),
            new String[]{SchoolCategoryCodes.PUBLIC.getCode()},
            new String[]{SchoolGradeCodes.HOMESCHOOL.getCode()}),

    ENTRY8(new String[]{FacilityTypeCodes.CONT_ED.getCode()},
            SchoolCategoryCodes.getActiveSchoolCategoryCodes(),
            new String[]{SchoolGradeCodes.HOMESCHOOL.getCode()}),

    ENTRY9(FacilityTypeCodes.getFacilityCodesWithoutOLAndCE(),
            new String[]{SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.INDP_FNS.getCode()},
            new String[]{SchoolGradeCodes.HOMESCHOOL.getCode()}),

    ENTRY10(new String[]{FacilityTypeCodes.DIST_LEARN.getCode(), FacilityTypeCodes.DISTONLINE.getCode()},
            new String[]{SchoolCategoryCodes.PUBLIC.getCode()},
            new String[]{SchoolGradeCodes.HOMESCHOOL.getCode()}),

    ENTRY11(new String[]{FacilityTypeCodes.DIST_LEARN.getCode(), FacilityTypeCodes.DISTONLINE.getCode()},
            new String[]{SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.INDP_FNS.getCode()},
            new String[]{SchoolGradeCodes.HOMESCHOOL.getCode()});


    private final String[] facilityCodes;
    private final String[] schoolCategoryCodes;
    private final String[] gradeCodes;

    DuplicateClassLookup(String[] facilityCodes, String[] schoolCategoryCodes, String[] gradeCodes){
        this.facilityCodes = facilityCodes;
        this.schoolCategoryCodes = schoolCategoryCodes;
        this.gradeCodes = gradeCodes;
    }

    public static DuplicateClassLookup getClassNumber(String facilityTypeCode, String schoolCategoryCode, String gradeCode){
        return Arrays.stream(values())
                .filter(e -> Arrays.asList(e.facilityCodes).contains(facilityTypeCode) && Arrays.asList(e.schoolCategoryCodes).contains(schoolCategoryCode) && Arrays.asList(e.gradeCodes).contains(gradeCode))
                .findFirst().orElse(null);
    }
}
