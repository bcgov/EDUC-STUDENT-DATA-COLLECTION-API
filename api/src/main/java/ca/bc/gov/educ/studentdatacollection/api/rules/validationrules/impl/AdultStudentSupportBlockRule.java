package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V68 | WARNING  | Adult students in grade 10, 11, 12, or SU , should not be reported    | V04,V28,V67  |
 *                      with support blocks.
 */
@Component
@Order(510)
public class AdultStudentSupportBlockRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent() &&
                !studentRuleData.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V68", validationErrorsMap)
                && DOBUtil.isAdult(studentRuleData.getSdcSchoolCollectionStudentEntity().getDob());
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if(SchoolGradeCodes.getAllowedAdultGradesNonGraduate().contains(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode())
                && StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getSupportBlocks())) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.SUPPORT_BLOCKS, StudentValidationIssueTypeCode.ADULT_SUPPORT_ERR));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.DOB, StudentValidationIssueTypeCode.ADULT_SUPPORT_ERR));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_GRADE_CODE, StudentValidationIssueTypeCode.ADULT_SUPPORT_ERR));
        }

        return errors;
    }
}
