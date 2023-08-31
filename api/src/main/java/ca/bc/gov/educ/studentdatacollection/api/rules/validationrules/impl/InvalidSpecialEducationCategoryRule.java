package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SpecialEducationCategoryCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V60 | ERROR    | For students reported with a Special Education Category,              | NONE         |
 *                     the category must be a valid, non-expired SPED Category.
 */
@Component
@Order(160)
public class InvalidSpecialEducationCategoryRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;
    public InvalidSpecialEducationCategoryRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent();
    }
    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        List<SpecialEducationCategoryCode> activeSpecialEducationCategoryCode = validationRulesService.getActiveSpecialEducationCategoryCodes();

        if(StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getSpecialEducationCategoryCode())
                && activeSpecialEducationCategoryCode.stream().noneMatch(program -> program.getSpecialEducationCategoryCode().equals(studentRuleData.getSdcSchoolCollectionStudentEntity().getSpecialEducationCategoryCode()))) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.SPECIAL_EDUCATION_CATEGORY_CODE, StudentValidationIssueTypeCode.SPED_ERR));
        }

        return errors;
    }

}
