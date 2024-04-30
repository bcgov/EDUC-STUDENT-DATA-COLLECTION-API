package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V39 | WARNING  | Student's reported with Indigenous Education Programs and        | V27,V30      |
 *                     Services should be reported with Indigenous Ancestry = Y
 *                     to get funding for the programs
 *
 */
@Component
@Slf4j
@Order(540)
public class IndigenousProgramRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;
    public IndigenousProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of IndigenousProgramRule-V39: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        var shouldExecute = StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes())
                && isValidationDependencyResolved("V39", validationErrorsMap);

        log.debug("In shouldExecute of IndigenousProgramRule-V39: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                shouldExecute,
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return shouldExecute;
    }
    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of IndigenousProgramRule-V39 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = validationRulesService.splitEnrolledProgramsString(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes());

        log.debug("IndigenousProgramRule-V39: Enrolled Program codes {} for sdcSchoolCollectionStudentID:: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        if (EnrolledProgramCodes.getIndigenousProgramCodes().stream().anyMatch(enrolledProgramCodes::contains) && !studentRuleData.getSdcSchoolCollectionStudentEntity().getNativeAncestryInd().equals("Y")) {
            log.debug("IndigenousProgramRule-V39: Invalid native ancestry indicator {} for sdcSchoolCollectionStudentID:: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getNativeAncestryInd(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.PROGRAM_CODE_IND));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.NATIVE_ANCESTRY_IND, StudentValidationIssueTypeCode.PROGRAM_CODE_IND));
        }
        return errors;
    }
}
