package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V42 | WARNING    | For students reported in grade 8, 9, 10, 11, 12, SU, or GA , their    | V29          |
 *                     Number of Courses should be less than 15.
 */
@Component
@Order(480)
public class MaximumNumberOfCoursesRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes
            .findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode())
            .isPresent() && isValidationDependencyResolved("V42", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        final String courseCountStr = student.getNumberOfCourses();
        final Double courseCount = TransformUtil.parseNumberOfCourses(courseCountStr, student.getSdcSchoolCollection().getSdcSchoolCollectionID());
        final boolean hasEightPlusGradeCodes = SchoolGradeCodes.get8PlusGrades().contains(student.getEnrolledGradeCode());

        if (StringUtils.isNotEmpty(courseCountStr) && hasEightPlusGradeCodes && courseCount > 15) {
            errors.add(createValidationIssue(
                SdcSchoolCollectionStudentValidationIssueSeverityCode.INFO_WARNING,
                SdcSchoolCollectionStudentValidationFieldCode.NUMBER_OF_COURSES,
                SdcSchoolCollectionStudentValidationIssueTypeCode.NO_OF_COURSE_MAX
            ));
        }

        return errors;
    }
}
