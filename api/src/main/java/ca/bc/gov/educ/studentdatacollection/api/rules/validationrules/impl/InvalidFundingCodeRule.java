package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolFundingCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V26 | ERROR    | A student's School Funding Code must be set to one of the following:  | NONE         |
 *                     Blank
 *                     14 - Out of province/international student)
 *                     20 - Ordinarily Living on a Reserve
 *                     16 - Newcomer Refugee
 */
@Component
@Order(100)
public class InvalidFundingCodeRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;

    public InvalidFundingCodeRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        List<SchoolFundingCode> activeFundingCodes = validationRulesService.getActiveFundingCodes();
        if(StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getSchoolFundingCode()) && activeFundingCodes.stream().noneMatch(code -> code.getSchoolFundingCode().equals(studentRuleData.getSdcSchoolCollectionStudentEntity().getSchoolFundingCode()))) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.SCHOOL_FUNDING_CODE, StudentValidationIssueTypeCode.FUNDING_CODE_INVALID));
        }
        return errors;
    }
}
