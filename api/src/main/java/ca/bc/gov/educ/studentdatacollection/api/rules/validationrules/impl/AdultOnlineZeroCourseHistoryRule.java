package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes.*;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V34 | WARNING  | For adult students, enrolled in grade 10, 11, 12, SU, or GA, reported | V29, V28, V04 |
 *                     by a provincial or district online learning school with Number of
 *                     Courses = 0, must have been reported by the school in the last 2
 *                     collection years with Number of Courses > 0.
 */
@Component
@Slf4j
@Order(290)
public class AdultOnlineZeroCourseHistoryRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;
    public AdultOnlineZeroCourseHistoryRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of ZeroCoursesReportedRule-V34: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        var shouldExecute = !studentRuleData.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V34", validationErrorsMap);

        log.debug("In shouldExecute of ZeroCoursesReportedRule-V34: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                shouldExecute,
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return shouldExecute;
    }


    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of ZeroCoursesReportedRule-V34 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        boolean isAdult = DOBUtil.isAdult(studentRuleData.getSdcSchoolCollectionStudentEntity().getDob());
        String schoolType = studentRuleData.getSchool().getFacilityTypeCode();
        boolean isOnline = Objects.equals(schoolType, String.valueOf(DIST_LEARN)) || Objects.equals(schoolType, String.valueOf(DISTONLINE));

        if(isAdult && isOnline){

            var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

            final String courseCountStr = student.getNumberOfCourses();
            final Double courseCount = TransformUtil.parseNumberOfCourses(courseCountStr, student.getSdcSchoolCollection().getSdcSchoolCollectionID());

            var hasRelevantGrade = SchoolGradeCodes.getAllowedAdultGrades().contains(student.getEnrolledGradeCode());
            boolean hasEnrollmentHistory = validationRulesService.hasEnrollmentHistory(studentRuleData);

            if(courseCount <= 0 && hasRelevantGrade && !hasEnrollmentHistory){
                log.debug("InvalidUsualLastNameRule-V34: Student has no courses reported within last two years for sdcSchoolCollectionStudentID::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                errors.add(createValidationIssue(
                        StudentValidationIssueSeverityCode.INFO_WARNING,
                        StudentValidationFieldCode.NUMBER_OF_COURSES,
                        StudentValidationIssueTypeCode.ADULT_ZERO_COURSE_HISTORY
                ));
            }
        }

        return errors;
    }
}
