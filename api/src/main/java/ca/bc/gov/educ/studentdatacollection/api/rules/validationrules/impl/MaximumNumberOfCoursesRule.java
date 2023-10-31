package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                    | Dependent On |
 *  |-----|----------|-------------------------------------------------------------------------|--------------|
 *  | V42 | WARNING    | For students reported in grade 8, 9, 10, 11, 12, SU, or GA , their    | V29          |
 *                     Number of Courses should be less than 15.
 */
@Component
@Slf4j
@Order(480)
public class MaximumNumberOfCoursesRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of MaximumNumberOfCoursesRule-V42: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        log.debug("In shouldExecute of MaximumNumberOfCoursesRule-V42: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                isValidationDependencyResolved("V42", validationErrorsMap),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return isValidationDependencyResolved("V42", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of MaximumNumberOfCoursesRule-V42 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        final String courseCountStr = student.getNumberOfCourses();
        final Double courseCount = TransformUtil.parseNumberOfCourses(courseCountStr, student.getSdcSchoolCollection().getSdcSchoolCollectionID());
        final boolean hasEightPlusGradeCodes = SchoolGradeCodes.get8PlusGrades().contains(student.getEnrolledGradeCode());

        if (StringUtils.isNotEmpty(courseCountStr) && hasEightPlusGradeCodes && courseCount > 15) {
            log.debug("MaximumNumberOfCoursesRule-V42: No of courses {} and grade code {} for sdcSchoolCollectionStudentID:: {}",student.getNumberOfCourses(), student.getEnrolledGradeCode(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

            errors.add(createValidationIssue(
                StudentValidationIssueSeverityCode.INFO_WARNING,
                StudentValidationFieldCode.NUMBER_OF_COURSES,
                StudentValidationIssueTypeCode.NO_OF_COURSE_MAX
            ));
        }

        return errors;
    }
}
