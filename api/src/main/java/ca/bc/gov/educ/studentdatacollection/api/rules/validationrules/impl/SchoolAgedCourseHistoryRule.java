package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V47 | WARNING  | For school-aged student enrolled in grade 8, 9, 10, 11, 12, SU, or    | V28, V04, V29 |
 *                     GA, reported by a provincial or district online school with Number of
 *                     Courses = 0, must have been reported by the school in the last 2 years
 *                     with Number of Courses > 0.
 */
@Component
@Slf4j
@Order(300)
public class SchoolAgedCourseHistoryRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;
    public SchoolAgedCourseHistoryRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of ZeroCoursesReportedRule-V47: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        log.debug("In shouldExecute of ZeroCoursesReportedRule-V47: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                isValidationDependencyResolved("V47", validationErrorsMap),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return isValidationDependencyResolved("V47", validationErrorsMap);
    }


    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of ZeroCoursesReportedRule-V47 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        boolean isAdult = DOBUtil.isAdult(studentRuleData.getSdcSchoolCollectionStudentEntity().getDob());

        if(!isAdult){

            var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

            final String courseCountStr = student.getNumberOfCourses();
            final Double courseCount = TransformUtil.parseNumberOfCourses(courseCountStr, student.getSdcSchoolCollection().getSdcSchoolCollectionID());

            var hasRelevantGrade = SchoolGradeCodes.get8PlusGrades().contains(student.getEnrolledGradeCode());
            boolean hasNoCoursesInLastTwoYears = validationRulesService.hasNoEnrollmentHistory(studentRuleData);

            if(courseCount <= 0 && hasRelevantGrade && hasNoCoursesInLastTwoYears){
                log.debug("InvalidUsualLastNameRule-V47: Student has no courses reported within last two years for sdcSchoolCollectionStudentID::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                errors.add(createValidationIssue(
                        StudentValidationIssueSeverityCode.INFO_WARNING,
                        StudentValidationFieldCode.NUMBER_OF_COURSES,
                        StudentValidationIssueTypeCode.SCHOOL_AGED_ZERO_COURSE_HISTORY
                ));
            }
        }


        return errors;
    }
}