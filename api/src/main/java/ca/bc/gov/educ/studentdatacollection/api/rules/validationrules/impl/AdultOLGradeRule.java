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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V57 | ERROR    | For adult students reported by a provincial or district online        | V04,V28      |
 *                     learning school, their reported grade must be  10, 11, 12, SU, or GA.
 */
@Component
@Order(430)
public class AdultOLGradeRule implements ValidationBaseRule {


    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent() &&
                !studentRuleData.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V57", validationErrorsMap)
                && DOBUtil.isAdult(studentRuleData.getSdcSchoolCollectionStudentEntity().getDob());
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if(onlineConditionPassed(studentRuleData)) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.ENROLLED_GRADE_CODE, StudentValidationIssueTypeCode.ADULT_GRADE_ERR));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.DOB, StudentValidationIssueTypeCode.ADULT_GRADE_ERR));
        }
        return errors;
    }

    private boolean onlineConditionPassed(StudentRuleData studentRuleData) {
        return  (studentRuleData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DISTONLINE.getCode()) || studentRuleData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DIST_LEARN.getCode())) &&
                !SchoolGradeCodes.getAllowedAdultGrades().contains(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode());
    }
}
