package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V46 | ERROR    | School-aged student enrolled in grade 8, 9, 10, 11, 12, SU, or        | V04,V28,V29  |
 *                     GA, not reported by a provincial or district online school must have
 *                     Number of Courses > 0.
 */
@Component
@Slf4j
@Order(470)
public class SchoolAgedNoOfCoursesRule implements ValidationBaseRule {
    private static final DecimalFormat df = new DecimalFormat("00.00");

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of SchoolAgedNoOfCoursesRule-V46: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        log.debug("In shouldExecute of SchoolAgedNoOfCoursesRule-V46: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent() &&
                        isValidationDependencyResolved("V43", validationErrorsMap) &&
                        studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode()),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent()
                && isValidationDependencyResolved("V46", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of SchoolAgedNoOfCoursesRule-V46 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

        if (conditionPassed(studentRuleData) && StringUtils.isNotEmpty(student.getNumberOfCourses()) && Double.parseDouble(df.format(Double.valueOf(student.getNumberOfCourses()))) == 0) {
            log.debug("SchoolAgedNoOfCoursesRule-V46: Home school student is not school-aged, DOB {} for sdcSchoolCollectionStudentID:: {}",student.getDob(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.NUMBER_OF_COURSES, StudentValidationIssueTypeCode.SCHOOLAGE_ZERO_COURSES));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.ENROLLED_GRADE_CODE, StudentValidationIssueTypeCode.SCHOOLAGE_ZERO_COURSES));
        }
        return errors;
    }

    private boolean conditionPassed(StudentRuleData studentRuleData) {
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        return DOBUtil.isSchoolAged(student.getDob()) && SchoolGradeCodes.get8PlusGrades().contains(student.getEnrolledGradeCode()) &&
                (!studentRuleData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DISTONLINE.getCode())
                        && !studentRuleData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DIST_LEARN.getCode()));
    }

}
