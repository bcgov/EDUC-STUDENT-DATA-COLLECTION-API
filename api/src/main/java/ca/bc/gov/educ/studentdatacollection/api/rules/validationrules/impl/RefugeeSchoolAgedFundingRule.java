package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolFundingCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Order(910)
public class RefugeeSchoolAgedFundingRule implements ValidationBaseRule {
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of RefugeeSchoolAgedFundingRule-V91: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        var shouldExecute = FteCalculatorUtils.getCollectionTypeCode(studentRuleData).equals(CollectionTypeCodes.FEBRUARY.getTypeCode()) &&
                StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getSchoolFundingCode()) &&
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSchoolFundingCode().equals(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode()) &&
                isValidationDependencyResolved("V90", validationErrorsMap);

        log.debug("In shouldExecute of RefugeeSchoolAgedFundingRule-V91: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                shouldExecute,
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return shouldExecute;
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of RefugeeSchoolAgedFundingRule-V91 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        boolean isAdult = studentRuleData.getSdcSchoolCollectionStudentEntity().getIsAdult();

        if (Boolean.TRUE.equals(isAdult)) {
            log.debug("RefugeeSchoolAgedFundingRule-V91: Refugee reported as adult.:: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.SCHOOL_FUNDING_CODE, StudentValidationIssueTypeCode.REFUGEE_IS_ADULT));
        }

        return errors;
    }
}
