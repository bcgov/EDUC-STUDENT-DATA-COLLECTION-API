package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolFundingCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.CareerProgramCode;
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
 *  | V52 | WARNING  | Students reported with a Funding Code of 14 cannot have any of the    | V26,V30,V32     |
 *                     Career Enrolled Program Codes: 40, 41, 42, 43 or Career Codes
 *
 */
@Component
@Slf4j
@Order(550)
public class FundingCode14CareerProgramRule implements ValidationBaseRule {

    private final ValidationRulesService validationRulesService;

    public FundingCode14CareerProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of FundingCode14CareerProgramRule-V52: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        var shouldExecute = studentRuleData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.PUBLIC.getCode()) &&
                StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes()) &&
                StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getSchoolFundingCode()) &&
                isValidationDependencyResolved("V52", validationErrorsMap) &&
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSchoolFundingCode().equals(SchoolFundingCodes.OUT_OF_PROVINCE.getCode());

        log.debug("In shouldExecute of FundingCode14CareerProgramRule-V52: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                shouldExecute,
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return shouldExecute;

    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of FundingCode14CareerProgramRule-V52 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = validationRulesService.splitEnrolledProgramsString(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes());
        List<CareerProgramCode> activeCareerPrograms = validationRulesService.getActiveCareerProgramCodes();

        if (EnrolledProgramCodes.getCareerProgramCodes().stream().anyMatch(enrolledProgramCodes::contains) || activeCareerPrograms.stream().anyMatch(programs -> enrolledProgramCodes.contains(programs.getCareerProgramCode()))) {
            log.debug("FundingCode14CareerProgramRule-V52: Invalid Career code and Funding Code 14 for sdcSchoolCollectionStudentID:: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.ENROLLED_CODE_CAREER_ERR));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.SCHOOL_FUNDING_CODE, StudentValidationIssueTypeCode.ENROLLED_CODE_CAREER_ERR));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.CAREER_PROGRAM_CODE, StudentValidationIssueTypeCode.ENROLLED_CODE_CAREER_ERR));
        }

        return errors;
    }

}
