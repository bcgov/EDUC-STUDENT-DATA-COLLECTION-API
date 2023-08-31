package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
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
@Order(470)
public class SchoolAgedNoOfCoursesRule implements ValidationBaseRule {
    private static final DecimalFormat df = new DecimalFormat("00.00");

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent()
                && isValidationDependencyResolved("V46", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

        if (conditionPassed(studentRuleData) && StringUtils.isNotEmpty(student.getNumberOfCourses()) && Double.parseDouble(df.format(Double.valueOf(student.getNumberOfCourses()))) == 0) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.NUMBER_OF_COURSES, SdcSchoolCollectionStudentValidationIssueTypeCode.SCHOOLAGE_ZERO_COURSES));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_GRADE_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.SCHOOLAGE_ZERO_COURSES));
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
