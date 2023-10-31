package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EnrolledGradeCode;
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
 *  | V28 | ERROR    | A student's enrolled grade code must a valid non-expired              | NONE         |
 *                     grade code. It cannot be blank.
 */
@Component
@Slf4j
@Order(10)
public class InvalidGradeCodeRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;
    public InvalidGradeCodeRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of InvalidGradeCodeRule-V28: for collectionType {} and sdcSchoolCollectionStudentID :: {}", studentRuleData.getCollectionTypeCode(),
                 studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return true;
    }
    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of InvalidGradeCodeRule-V28 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        List<EnrolledGradeCode> activeGradeCodes = validationRulesService.getActiveGradeCodes();

        if(StringUtils.isEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode()) || activeGradeCodes.stream().noneMatch(code -> code.getEnrolledGradeCode().equals(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode()))){
            log.debug("InvalidGradeCodeRule-V28 has: Invalid grade code {}, value does not exist in DB for sdcSchoolCollectionStudentID :: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.ENROLLED_GRADE_CODE, StudentValidationIssueTypeCode.INVALID_GRADE_CODE));
        }

        return errors;
    }

}
