package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V70 | ERROR    | School-aged students must not be reported in grade "GA".              | V04,V28      |
 */
@Component
@Order(440)
public class GARule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes
            .findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode())
            .isPresent() && isValidationDependencyResolved("V70", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        final String enrolledGradeCode = student.getEnrolledGradeCode();

        final String studentDOB = student.getDob();
        if (DOBUtil.isSchoolAged(studentDOB) && enrolledGradeCode.equals("GA")) {
            errors.add(createValidationIssue(
                StudentValidationIssueSeverityCode.ERROR,
                StudentValidationFieldCode.DOB,
                StudentValidationIssueTypeCode.GA_ERROR
            ));
            errors.add(createValidationIssue(
                StudentValidationIssueSeverityCode.ERROR,
                StudentValidationFieldCode.ENROLLED_GRADE_CODE,
                StudentValidationIssueTypeCode.GA_ERROR
            ));
        }
        return errors;
    }
}
