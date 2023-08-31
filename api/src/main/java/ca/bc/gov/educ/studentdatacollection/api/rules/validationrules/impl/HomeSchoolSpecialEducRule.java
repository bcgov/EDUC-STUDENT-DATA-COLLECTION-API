package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
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
 *  | V25 | WARNING  | Students registered in homeschool (enrolled grade of HS),             |  V60,V28     |
 *                     should not be reported with any special education designations.
 *
 */
@Component
@Order(670)
public class HomeSchoolSpecialEducRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent() &&
                isValidationDependencyResolved("V25", validationErrorsMap) &&
                studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode());
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

        if (StringUtils.isNotEmpty(student.getSpecialEducationCategoryCode())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.FUNDING_WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_GRADE_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.PROGRAM_CODE_HS_SPED));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.FUNDING_WARNING, SdcSchoolCollectionStudentValidationFieldCode.SPECIAL_EDUCATION_CATEGORY_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.PROGRAM_CODE_HS_SPED));
        }
        return errors;
    }
}
