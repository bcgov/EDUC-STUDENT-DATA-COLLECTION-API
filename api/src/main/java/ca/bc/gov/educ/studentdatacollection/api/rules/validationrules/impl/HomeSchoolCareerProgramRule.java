package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.CareerProgramCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V24 | WARNING  | Students registered in homeschool (enrolled grade of HS),             |  V30,V28,V32 |
 *                     must not be reported with any of the following Enrolled Program Codes:
 *                     40, 41, 42, 43 and Career CodeS.
 *
 */
@Component
@Order(630)
public class HomeSchoolCareerProgramRule implements ValidationBaseRule {

    private final ValidationRulesService validationRulesService;

    public HomeSchoolCareerProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent() &&
                isValidationDependencyResolved("V24", validationErrorsMap) &&
                studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode()) &&
                StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes());
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        List<CareerProgramCode> activeCareerCodes = validationRulesService.getActiveCareerProgramCodes();
        final List<String> enrolledProgramCodes = validationRulesService.splitString(student.getEnrolledProgramCodes());

        if (EnrolledProgramCodes.getCareerProgramCodes().stream().anyMatch(enrolledProgramCodes::contains) || (StringUtils.isNotEmpty(student.getCareerProgramCode()) && activeCareerCodes.stream().anyMatch(careerCode -> student.getCareerProgramCode().contains(careerCode.getCareerProgramCode())))) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.PROGRAM_CODE_HS_CAREER));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_GRADE_CODE, StudentValidationIssueTypeCode.PROGRAM_CODE_HS_CAREER));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.CAREER_PROGRAM_CODE, StudentValidationIssueTypeCode.PROGRAM_CODE_HS_CAREER));
        }

        return errors;
    }
}
