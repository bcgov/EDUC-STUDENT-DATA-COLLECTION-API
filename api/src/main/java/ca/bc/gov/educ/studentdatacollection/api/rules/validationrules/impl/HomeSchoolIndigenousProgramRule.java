package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V23 | WARNING  | Students registered in homeschool (enrolled grade of HS),             |  V30,V28     |
 *                     must not be reported with any of the following Program Codes:
 *                     29, 33, 36
 *
 */
@Component
@Slf4j
@Order(650)
public class HomeSchoolIndigenousProgramRule implements ValidationBaseRule {

    private final ValidationRulesService validationRulesService;

    public HomeSchoolIndigenousProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of HomeSchoolIndigenousProgramRule-V23: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        log.debug("In shouldExecute of HomeSchoolIndigenousProgramRule-V23: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                isValidationDependencyResolved("V23", validationErrorsMap) &&
                        studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode()) &&
                        StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes()),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return isValidationDependencyResolved("V23", validationErrorsMap) &&
                studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode()) &&
                StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes());
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of HomeSchoolIndigenousProgramRule-V23 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        final List<String> enrolledProgramCodes = validationRulesService.splitString(student.getEnrolledProgramCodes());

        if (EnrolledProgramCodes.getIndigenousProgramCodes().stream().anyMatch(enrolledProgramCodes::contains)) {
            log.debug("HomeSchoolIndigenousProgramRule-V23: Homeschool student cannot have indigenous program codes for sdcSchoolCollectionStudentID:: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.PROGRAM_CODE_HS_IND));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_GRADE_CODE, StudentValidationIssueTypeCode.PROGRAM_CODE_HS_IND));
        }
        return errors;
    }
}
