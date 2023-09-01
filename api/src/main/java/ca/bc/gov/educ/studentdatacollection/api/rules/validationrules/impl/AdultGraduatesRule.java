package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.PenMatchAndGradStatusService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V48 | ERROR    | Adult graduates must be reported in grade GA.                         | V04,V28,V29  |
 */
@Component
@Order(700)
public class AdultGraduatesRule implements ValidationBaseRule {

    private final PenMatchAndGradStatusService penMatchAndGradStatusService;

    public AdultGraduatesRule(PenMatchAndGradStatusService penMatchAndGradStatusService) {
        this.penMatchAndGradStatusService = penMatchAndGradStatusService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent()
                && !studentRuleData.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V48", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

        if(student.getIsGraduated() == null){
            penMatchAndGradStatusService.updatePenMatchAndGradStatusColumns(student, studentRuleData.getSchool().getMincode());
        }

        if (student.getIsAdult() && student.getIsGraduated() && !student.getEnrolledGradeCode().equalsIgnoreCase(SchoolGradeCodes.GRADUATED_ADULT.getCode())) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.DOB, StudentValidationIssueTypeCode.ADULT_GRADUATED));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.ENROLLED_GRADE_CODE, StudentValidationIssueTypeCode.ADULT_GRADUATED));
        }
        return errors;
    }

}
