package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import static ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode.*;
import lombok.Getter;


import java.util.Arrays;
import java.util.Optional;

public enum ValidationRulesDependencyMatrix {
    ENTRY1("V15", new String[]{LEGAL_LAST_NAME_BLANK.getCode()}),
    ENTRY2("V13", new String[]{LEGAL_FIRST_NAME_CHAR_FIX.getCode()}),
    ENTRY3("V14", new String[]{LEGAL_MIDDLE_NAME_CHAR_FIX.getCode()}),
    ENTRY4("V16", new String[]{USUAL_FIRST_NAME_CHAR_FIX.getCode()}),
    ENTRY5("V18", new String[]{USUAL_LAST_NAME_CHAR_FIX.getCode()}),
    ENTRY6("V21", new String[]{PEN_CHECK_DIGIT_ERR.getCode()}),
    ENTRY7("V64", new String[]{MISSING_POSTAL_CODE.getCode()}),
    ENTRY8("V17", new String[]{USUAL_MIDDLE_NAME_CHAR_FIX.getCode()}),
    ENTRY9("V40", new String[]{BAND_CODE_INVALID.getCode(), FUNDING_CODE_INVALID.getCode()}),
    ENTRY10("V39", new String[]{NATIVE_IND_INVALID.getCode(), ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode()}),
    ENTRY11("V52", new String[]{FUNDING_CODE_INVALID.getCode(), ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode(), CAREER_CODE_INVALID.getCode()}),
    ENTRY12("V30", new String[]{ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_DUP_ERR.getCode()}),
    ENTRY13("V59", new String[]{CAREER_CODE_INVALID.getCode(), ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode()}),
    ENTRY14("V73", new String[]{INVALID_GRADE_CODE.getCode()}),
    ENTRY15("V44", new String[]{INVALID_GRADE_CODE.getCode()}),
    ENTRY16("V72", new String[]{INVALID_GRADE_CODE.getCode()}),
    ENTRY17("V37", new String[]{DOB_INVALID_FORMAT.getCode()}),
    ENTRY18("V53", new String[]{DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode(), GENDER_INVALID.getCode(), LEGAL_LAST_NAME_BLANK.getCode(), LEGAL_FIRST_NAME_CHAR_FIX.getCode(), LEGAL_MIDDLE_NAME_CHAR_FIX.getCode(), LEGAL_LAST_NAME_CHAR_FIX.getCode(), USUAL_FIRST_NAME_CHAR_FIX.getCode(), USUAL_MIDDLE_NAME_CHAR_FIX.getCode(), USUAL_LAST_NAME_CHAR_FIX.getCode()}),
    ENTRY19("V54", new String[]{DOB_INVALID_FORMAT.getCode()}),
    ENTRY20("V36", new String[]{DOB_INVALID_FORMAT.getCode()}),
    ENTRY21("V49", new String[]{DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode(), GENDER_INVALID.getCode(), LEGAL_LAST_NAME_BLANK.getCode(), LEGAL_FIRST_NAME_CHAR_FIX.getCode(), LEGAL_MIDDLE_NAME_CHAR_FIX.getCode(), LEGAL_LAST_NAME_CHAR_FIX.getCode(), USUAL_FIRST_NAME_CHAR_FIX.getCode(), USUAL_MIDDLE_NAME_CHAR_FIX.getCode(), USUAL_LAST_NAME_CHAR_FIX.getCode()}),
    ENTRY22("V45", new String[]{DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY23("V48", new String[]{DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode(), GENDER_INVALID.getCode(), LEGAL_LAST_NAME_BLANK.getCode(), LEGAL_FIRST_NAME_CHAR_FIX.getCode(), LEGAL_MIDDLE_NAME_CHAR_FIX.getCode(), LEGAL_LAST_NAME_CHAR_FIX.getCode(), USUAL_FIRST_NAME_CHAR_FIX.getCode(), USUAL_MIDDLE_NAME_CHAR_FIX.getCode(), USUAL_LAST_NAME_CHAR_FIX.getCode()}),
    ENTRY24("V57", new String[]{DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY25("V70", new String[]{DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY26("V43", new String[]{DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY27("V33", new String[]{NO_OF_COURSES_INVALID.getCode(), DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY28("V46", new String[]{NO_OF_COURSES_INVALID.getCode(), DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY29("V47", new String[]{NO_OF_COURSES_INVALID.getCode(), DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY30("V34", new String[]{NO_OF_COURSES_INVALID.getCode(), DOB_INVALID_FORMAT.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY31("V42", new String[]{NO_OF_COURSES_INVALID.getCode()}),
    ENTRY33("V65", new String[]{NO_OF_COURSES_INVALID.getCode(), INVALID_GRADE_CODE.getCode(), SUPPORT_BLOCKS_INVALID.getCode()}),
    ENTRY34("V68", new String[]{DOB_INVALID_FORMAT.getCode(), SUPPORT_BLOCKS_INVALID.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY35("V69", new String[]{DOB_INVALID_FORMAT.getCode(), SUPPORT_BLOCKS_INVALID.getCode(), INVALID_GRADE_CODE.getCode(), GENDER_INVALID.getCode(), LEGAL_LAST_NAME_BLANK.getCode(), LEGAL_FIRST_NAME_CHAR_FIX.getCode(), LEGAL_MIDDLE_NAME_CHAR_FIX.getCode(), LEGAL_LAST_NAME_CHAR_FIX.getCode(), USUAL_FIRST_NAME_CHAR_FIX.getCode(), USUAL_MIDDLE_NAME_CHAR_FIX.getCode(), USUAL_LAST_NAME_CHAR_FIX.getCode()}),
    ENTRY36("V66", new String[]{SUPPORT_BLOCKS_INVALID.getCode()}),
    ENTRY37("V71", new String[]{SUPPORT_BLOCKS_INVALID.getCode()}),
    ENTRY38("V50", new String[]{FUNDING_CODE_INVALID.getCode(), ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode()}),
    ENTRY39("V51", new String[]{FUNDING_CODE_INVALID.getCode(), ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode()}),
    ENTRY40("V31", new String[]{ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode()}),
    ENTRY41("V19", new String[]{ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode()}),
    ENTRY43("V61", new String[]{ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode(), INVALID_GRADE_CODE.getCode(), CAREER_CODE_COUNT_ERR.getCode(), CAREER_CODE_INVALID.getCode()}),
    ENTRY44("V58", new String[]{ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode(), CAREER_CODE_COUNT_ERR.getCode(), CAREER_CODE_INVALID.getCode()}),
    ENTRY46("V22", new String[]{ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY47("V23", new String[]{ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY48("V38", new String[]{ENROLLED_CODE_PARSE_ERR.getCode(), ENROLLED_CODE_INVALID.getCode(), INVALID_GRADE_CODE.getCode()}),
    ENTRY49("V25", new String[]{INVALID_GRADE_CODE.getCode(), SPED_ERR.getCode()}),
    ENTRY50("V09", new String[]{LEGAL_LAST_NAME_BLANK.getCode()}),
    ENTRY51("V75", new String[]{ENROLLED_CODE_PARSE_ERR.getCode()}),
    ENTRY52("V34", new String[]{NO_OF_COURSES_INVALID.getCode(), INVALID_GRADE_CODE.getCode(), DOB_INVALID_FORMAT.getCode()}),
    ENTRY53("V47", new String[]{NO_OF_COURSES_INVALID.getCode(), INVALID_GRADE_CODE.getCode(), DOB_INVALID_FORMAT.getCode()}),
    ENTRY54("V76", new String[]{SPED_ERR.getCode()});


    @Getter
    private final String ruleID;
    @Getter
    private final String[] baseRuleErrorCode;
    ValidationRulesDependencyMatrix(String ruleID, String[] baseRuleErrorCode) {
        this.ruleID = ruleID;
        this.baseRuleErrorCode = baseRuleErrorCode;
    }

    public static Optional<ValidationRulesDependencyMatrix> findByValue(String ruleID) {
        return Arrays.stream(values()).filter(code -> code.ruleID.equalsIgnoreCase(ruleID)).findFirst();
    }
}
