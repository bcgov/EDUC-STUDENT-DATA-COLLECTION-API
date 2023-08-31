package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V58 | ERROR    | Students reported with a valid non-expired Career Program (enrolled program)   |  V30,V59 |
 *                     or Career Program Type, must be reported with both a Career
 *                     Program and Career Code.
 *
 */
@Component
@Order(620)
public class CareerProgramCodeRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;
    public CareerProgramCodeRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent()
                && StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes())
                && isValidationDependencyResolved("V58", validationErrorsMap);
    }
    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = validationRulesService.splitString(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes());

        if((StringUtils.isEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getCareerProgramCode()) && EnrolledProgramCodes.getCareerProgramCodes().stream().anyMatch(enrolledProgramCodes::contains))
                || (StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getCareerProgramCode()) && EnrolledProgramCodes.getCareerProgramCodes().stream().noneMatch(enrolledProgramCodes::contains))) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.CAREER_PROGRAM_CODE, StudentValidationIssueTypeCode.CAREER_CODE_PROG_ERR));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.CAREER_CODE_PROG_ERR));
        }

        return errors;
    }
}
