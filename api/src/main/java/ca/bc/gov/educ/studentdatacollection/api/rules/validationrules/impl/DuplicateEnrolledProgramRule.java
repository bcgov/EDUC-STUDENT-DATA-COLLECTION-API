package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V75 | ERROR    | List of enrolled programs codes must                                  | V74         |
 *                     not include duplicate program (e.g. 404005)
 */
@Component
@Slf4j
@Order(345)
public class DuplicateEnrolledProgramRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;
    public DuplicateEnrolledProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of DuplicateEnrolledProgramRule-V75: for collectionType {} and sdcSchoolCollectionStudentID :: {}", studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes())
                && isValidationDependencyResolved("V75", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of DuplicateEnrolledProgramRule-V75 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        List<String> enrolledProgramCodesList = validationRulesService.splitString(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes());
        Set<String> enrolledProgramCodesSet = new HashSet<>(enrolledProgramCodesList);
        if (enrolledProgramCodesList.size() > enrolledProgramCodesSet.size()) {
            log.debug("DuplicateEnrolledProgramRule-V75: Duplicate enrolled program code found {} for sdcSchoolCollectionStudentID:: {}" , studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.ENROLLED_CODE_DUP_ERR));
        }
        return errors;
    }
}
