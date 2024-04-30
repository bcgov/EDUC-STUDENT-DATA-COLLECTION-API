package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                         | Dependent On |
 *  |-----|----------|------------------------------------------------------------------------------|--------------|
 *  | V77 | WARNING  |  Only school-aged students should be reported with Indigenous Support        |  V04, V30    |
 *                      Programs.
 *
 */
@Component
@Slf4j
@Order(690)
public class SchoolAgedIndigenousSupportRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;

    public SchoolAgedIndigenousSupportRule(ValidationRulesService validationRulesService) {
      this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of AdultIndigenousFundingRule-V77: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        var shouldExecute = isValidationDependencyResolved("V77", validationErrorsMap);

        log.debug("In shouldExecute of AdultIndigenousFundingRule-V77: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                shouldExecute,
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return shouldExecute;
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of AdultIndigenousFundingRule-V77 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        List<String> studentPrograms = validationRulesService.splitEnrolledProgramsString(student.getEnrolledProgramCodes());

        log.debug("AdultIndigenousFundingRule-V77: Invalid age for Indigenous Support Programs for sdcSchoolCollectionStudentID:: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        if (EnrolledProgramCodes.getIndigenousProgramCodes().stream().anyMatch(studentPrograms::contains) && DOBUtil.isAdult(student.getDob())
            || (EnrolledProgramCodes.getIndigenousProgramCodes().stream().anyMatch(studentPrograms::contains) && !DOBUtil.isAdult(student.getDob()) && !DOBUtil.isSchoolAged(student.getDob()))) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.SCHOOL_AGED_INDIGENOUS_SUPPORT));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.DOB, StudentValidationIssueTypeCode.SCHOOL_AGED_INDIGENOUS_SUPPORT));
        }

        return errors;
    }
}
