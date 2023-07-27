package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum ValidationRulesDependencyMatrix {
    ENTRY1("V15", new String[]{"LEGALLASTNAMEBLANK"}),
    ENTRY2("V13", new String[]{"LEGALFIRSTNAMECHARFIX"}),
    ENTRY3("V14", new String[]{"LEGALMIDDLENAMECHARFIX"}),
    ENTRY4("V16", new String[]{"USUALFIRSTNAMECHARFIX"}),
    ENTRY5("V18", new String[]{"USUALLASTNAMECHARFIX"}),
    ENTRY6("V21", new String[]{"PENCHECKDIGITERR"}),
    ENTRY7("V64", new String[]{"MISSINGPOSTALCODE"}),
    ENTRY8("V17", new String[]{"USUALMIDDLENAMECHARFIX"}),
    ENTRY9("V40", new String[]{"BANDCODEINVALID", Constants.FUNDING_CODE_INVALID}),
    ENTRY10("V39", new String[]{"NATIVEINDINVALID", Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID}),
    ENTRY11("V52", new String[]{Constants.FUNDING_CODE_INVALID, Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID, Constants.CAREER_CODE_INVALID}),
    ENTRY12("V30", new String[]{Constants.ENROLLED_CODE_PARSEERR}),
    ENTRY13("V59", new String[]{Constants.CAREER_CODE_INVALID, Constants.ENROLLED_CODE_PARSEERR, "ENROLLEDCODEINVALID"}),
    ENTRY14("V73", new String[]{Constants.INVALID_GRADE_CODE}),
    ENTRY15("V44", new String[]{Constants.INVALID_GRADE_CODE}),
    ENTRY16("V72", new String[]{Constants.INVALID_GRADE_CODE}),
    ENTRY17("V37", new String[]{Constants.DOB_INVALID_FORMAT}),
    ENTRY18("V53", new String[]{Constants.DOB_INVALID_FORMAT}),
    ENTRY19("V54", new String[]{Constants.DOB_INVALID_FORMAT}),
    ENTRY20("V36", new String[]{Constants.DOB_INVALID_FORMAT}),
    ENTRY21("V49", new String[]{Constants.DOB_INVALID_FORMAT}),
    ENTRY22("V45", new String[]{Constants.DOB_INVALID_FORMAT, Constants.INVALID_GRADE_CODE}),
    ENTRY23("V48", new String[]{Constants.DOB_INVALID_FORMAT, Constants.INVALID_GRADE_CODE}),
    ENTRY24("V57", new String[]{Constants.DOB_INVALID_FORMAT, Constants.INVALID_GRADE_CODE}),
    ENTRY25("V70", new String[]{Constants.DOB_INVALID_FORMAT, Constants.INVALID_GRADE_CODE}),
    ENTRY26("V43", new String[]{Constants.DOB_INVALID_FORMAT, Constants.INVALID_GRADE_CODE}),
    ENTRY27("V33", new String[]{Constants.NO_OF_COURSES_INVALID, Constants.DOB_INVALID_FORMAT, Constants.INVALID_GRADE_CODE}),
    ENTRY28("V46", new String[]{Constants.NO_OF_COURSES_INVALID, Constants.DOB_INVALID_FORMAT, Constants.INVALID_GRADE_CODE}),
    ENTRY29("V47", new String[]{Constants.NO_OF_COURSES_INVALID, Constants.DOB_INVALID_FORMAT, Constants.INVALID_GRADE_CODE}),
    ENTRY30("V34", new String[]{Constants.NO_OF_COURSES_INVALID, Constants.DOB_INVALID_FORMAT, Constants.INVALID_GRADE_CODE}),
    ENTRY31("V42", new String[]{Constants.NO_OF_COURSES_INVALID}),
    ENTRY32("V55", new String[]{"SPEDERR"}),
    ENTRY33("V65", new String[]{Constants.NO_OF_COURSES_INVALID, Constants.INVALID_GRADE_CODE, Constants.SUPPORT_BLOCKS_INVALID}),
    ENTRY34("V68", new String[]{Constants.DOB_INVALID_FORMAT, Constants.SUPPORT_BLOCKS_INVALID, Constants.INVALID_GRADE_CODE}),
    ENTRY35("V69", new String[]{Constants.DOB_INVALID_FORMAT, Constants.SUPPORT_BLOCKS_INVALID, Constants.INVALID_GRADE_CODE}),
    ENTRY36("V66", new String[]{Constants.SUPPORT_BLOCKS_INVALID}),
    ENTRY37("V71", new String[]{Constants.SUPPORT_BLOCKS_INVALID}),
    ENTRY38("V50", new String[]{Constants.FUNDING_CODE_INVALID, Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID}),
    ENTRY39("V51", new String[]{Constants.FUNDING_CODE_INVALID, Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID}),
    ENTRY40("V31", new String[]{Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID}),
    ENTRY41("V19", new String[]{Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID}),
    ENTRY42("V56", new String[]{Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID, Constants.CAREER_CODE_INVALID}),
    ENTRY43("V61", new String[]{Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID, Constants.INVALID_GRADE_CODE, "CAREERCODECOUNTERR", Constants.CAREER_CODE_INVALID}),
    ENTRY44("V58", new String[]{Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID, "CAREERCODECOUNTERR", Constants.CAREER_CODE_INVALID}),
    ENTRY45("V24", new String[]{Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID, Constants.CAREER_CODE_INVALID, Constants.INVALID_GRADE_CODE}),
    ENTRY46("V22", new String[]{Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID, Constants.INVALID_GRADE_CODE}),
    ENTRY47("V23", new String[]{Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID, Constants.INVALID_GRADE_CODE}),
    ENTRY48("V38", new String[]{Constants.ENROLLED_CODE_PARSEERR, Constants.ENROLLED_CODE_INVALID, Constants.INVALID_GRADE_CODE}),
    ENTRY49("V25", new String[]{Constants.INVALID_GRADE_CODE, "SPEDERR"}),
    ENTRY50("V09", new String[]{"LEGALLASTNAMECHARFIX"}),
;
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
