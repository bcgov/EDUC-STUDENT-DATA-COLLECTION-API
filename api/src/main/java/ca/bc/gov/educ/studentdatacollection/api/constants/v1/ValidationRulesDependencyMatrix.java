package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum ValidationRulesDependencyMatrix {
    ENTRY1("V15", new String[]{RulesConstants.LEGAL_LAST_NAME_BLANK}),
    ENTRY2("V13", new String[]{RulesConstants.LEGAL_FIRST_NAME_CHAR_FIX}),
    ENTRY3("V14", new String[]{RulesConstants.LEGAL_MIDDLE_NAME_CHAR_FIX}),
    ENTRY4("V16", new String[]{RulesConstants.USUAL_FIRST_NAME_CHAR_FIX}),
    ENTRY5("V18", new String[]{RulesConstants.USUAL_LAST_NAME_CHAR_FIX}),
    ENTRY6("V21", new String[]{"PENCHECKDIGITERR"}),
    ENTRY7("V64", new String[]{"MISSINGPOSTALCODE"}),
    ENTRY8("V17", new String[]{RulesConstants.USUAL_MIDDLE_NAME_CHAR_FIX}),
    ENTRY9("V40", new String[]{"BANDCODEINVALID", RulesConstants.FUNDING_CODE_INVALID}),
    ENTRY10("V39", new String[]{"NATIVEINDINVALID", RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID}),
    ENTRY11("V52", new String[]{RulesConstants.FUNDING_CODE_INVALID, RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID, RulesConstants.CAREER_CODE_INVALID}),
    ENTRY12("V30", new String[]{RulesConstants.ENROLLED_CODE_PARSEERR}),
    ENTRY13("V59", new String[]{RulesConstants.CAREER_CODE_INVALID, RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID}),
    ENTRY14("V73", new String[]{RulesConstants.INVALID_GRADE_CODE}),
    ENTRY15("V44", new String[]{RulesConstants.INVALID_GRADE_CODE}),
    ENTRY16("V72", new String[]{RulesConstants.INVALID_GRADE_CODE}),
    ENTRY17("V37", new String[]{RulesConstants.DOB_INVALID_FORMAT}),
    ENTRY18("V53", new String[]{RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE, RulesConstants.GENDER_INVALID, RulesConstants.LEGAL_LAST_NAME_BLANK, RulesConstants.LEGAL_FIRST_NAME_CHAR_FIX, RulesConstants.LEGAL_MIDDLE_NAME_CHAR_FIX, RulesConstants.LEGAL_LAST_NAME_CHAR_FIX, RulesConstants.USUAL_FIRST_NAME_CHAR_FIX, RulesConstants.USUAL_MIDDLE_NAME_CHAR_FIX, RulesConstants.USUAL_LAST_NAME_CHAR_FIX}),
    ENTRY19("V54", new String[]{RulesConstants.DOB_INVALID_FORMAT}),
    ENTRY20("V36", new String[]{RulesConstants.DOB_INVALID_FORMAT}),
    ENTRY21("V49", new String[]{RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE, RulesConstants.GENDER_INVALID, RulesConstants.LEGAL_LAST_NAME_BLANK, RulesConstants.LEGAL_FIRST_NAME_CHAR_FIX, RulesConstants.LEGAL_MIDDLE_NAME_CHAR_FIX, RulesConstants.LEGAL_LAST_NAME_CHAR_FIX, RulesConstants.USUAL_FIRST_NAME_CHAR_FIX, RulesConstants.USUAL_MIDDLE_NAME_CHAR_FIX, RulesConstants.USUAL_LAST_NAME_CHAR_FIX}),
    ENTRY22("V45", new String[]{RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY23("V48", new String[]{RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE, RulesConstants.GENDER_INVALID, RulesConstants.LEGAL_LAST_NAME_BLANK, RulesConstants.LEGAL_FIRST_NAME_CHAR_FIX, RulesConstants.LEGAL_MIDDLE_NAME_CHAR_FIX, RulesConstants.LEGAL_LAST_NAME_CHAR_FIX, RulesConstants.USUAL_FIRST_NAME_CHAR_FIX, RulesConstants.USUAL_MIDDLE_NAME_CHAR_FIX, RulesConstants.USUAL_LAST_NAME_CHAR_FIX}),
    ENTRY24("V57", new String[]{RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY25("V70", new String[]{RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY26("V43", new String[]{RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY27("V33", new String[]{RulesConstants.NO_OF_COURSES_INVALID, RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY28("V46", new String[]{RulesConstants.NO_OF_COURSES_INVALID, RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY29("V47", new String[]{RulesConstants.NO_OF_COURSES_INVALID, RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY30("V34", new String[]{RulesConstants.NO_OF_COURSES_INVALID, RulesConstants.DOB_INVALID_FORMAT, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY31("V42", new String[]{RulesConstants.NO_OF_COURSES_INVALID}),
    ENTRY32("V55", new String[]{"SPEDERR"}),
    ENTRY33("V65", new String[]{RulesConstants.NO_OF_COURSES_INVALID, RulesConstants.INVALID_GRADE_CODE, RulesConstants.SUPPORT_BLOCKS_INVALID}),
    ENTRY34("V68", new String[]{RulesConstants.DOB_INVALID_FORMAT, RulesConstants.SUPPORT_BLOCKS_INVALID, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY35("V69", new String[]{RulesConstants.DOB_INVALID_FORMAT, RulesConstants.SUPPORT_BLOCKS_INVALID, RulesConstants.INVALID_GRADE_CODE, RulesConstants.GENDER_INVALID, RulesConstants.LEGAL_LAST_NAME_BLANK, RulesConstants.LEGAL_FIRST_NAME_CHAR_FIX, RulesConstants.LEGAL_MIDDLE_NAME_CHAR_FIX, RulesConstants.LEGAL_LAST_NAME_CHAR_FIX, RulesConstants.USUAL_FIRST_NAME_CHAR_FIX, RulesConstants.USUAL_MIDDLE_NAME_CHAR_FIX, RulesConstants.USUAL_LAST_NAME_CHAR_FIX}),
    ENTRY36("V66", new String[]{RulesConstants.SUPPORT_BLOCKS_INVALID}),
    ENTRY37("V71", new String[]{RulesConstants.SUPPORT_BLOCKS_INVALID}),
    ENTRY38("V50", new String[]{RulesConstants.FUNDING_CODE_INVALID, RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID}),
    ENTRY39("V51", new String[]{RulesConstants.FUNDING_CODE_INVALID, RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID}),
    ENTRY40("V31", new String[]{RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID}),
    ENTRY41("V19", new String[]{RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID}),
    ENTRY42("V56", new String[]{RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID, RulesConstants.CAREER_CODE_INVALID}),
    ENTRY43("V61", new String[]{RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID, RulesConstants.INVALID_GRADE_CODE, "CAREERCODECOUNTERR", RulesConstants.CAREER_CODE_INVALID}),
    ENTRY44("V58", new String[]{RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID, "CAREERCODECOUNTERR", RulesConstants.CAREER_CODE_INVALID}),
    ENTRY45("V24", new String[]{RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID, RulesConstants.CAREER_CODE_INVALID, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY46("V22", new String[]{RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY47("V23", new String[]{RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY48("V38", new String[]{RulesConstants.ENROLLED_CODE_PARSEERR, RulesConstants.ENROLLED_CODE_INVALID, RulesConstants.INVALID_GRADE_CODE}),
    ENTRY49("V25", new String[]{RulesConstants.INVALID_GRADE_CODE, "SPEDERR"}),
    ENTRY50("V09", new String[]{RulesConstants.LEGAL_LAST_NAME_BLANK}),
    ENTRY51("V34", new String[]{RulesConstants.NO_OF_COURSES_INVALID, RulesConstants.INVALID_GRADE_CODE, RulesConstants.DOB_INVALID_FORMAT}),
    ENTRY52("V47", new String[]{RulesConstants.NO_OF_COURSES_INVALID, RulesConstants.INVALID_GRADE_CODE, RulesConstants.DOB_INVALID_FORMAT});


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

    private static class RulesConstants {
        public static final String FUNDING_CODE_INVALID="FUNDINGCODEINVALID";
        public static final String ENROLLED_CODE_INVALID="ENROLLEDCODEINVALID";
        public static final String ENROLLED_CODE_PARSEERR="ENROLLEDCODEPARSEERR";
        public static final String CAREER_CODE_INVALID="CAREERCODEINVALID";
        public static final String INVALID_GRADE_CODE="INVALIDGRADECODE";
        public static final String DOB_INVALID_FORMAT="DOBINVALIDFORMAT";
        public static final String NO_OF_COURSES_INVALID="NOOFCOURSESINVALID";
        public static final String SUPPORT_BLOCKS_INVALID="SUPPORTBLOCKSINVALID";
        public static final String GENDER_INVALID="GENDERINVALID";
        public static final String LEGAL_LAST_NAME_BLANK = "LEGALLASTNAMEBLANK";
        public static final String LEGAL_FIRST_NAME_CHAR_FIX = "LEGALFIRSTNAMECHARFIX";
        public static final String LEGAL_MIDDLE_NAME_CHAR_FIX = "LEGALMIDDLENAMECHARFIX";
        public static final String LEGAL_LAST_NAME_CHAR_FIX = "LEGALLASTNAMECHARFIX";
        public static final String USUAL_FIRST_NAME_CHAR_FIX = "USUALFIRSTNAMECHARFIX";
        public static final String  USUAL_MIDDLE_NAME_CHAR_FIX = "USUALMIDDLENAMECHARFIX";
        public static final String USUAL_LAST_NAME_CHAR_FIX = "USUALLASTNAMECHARFIX";
    }
}
