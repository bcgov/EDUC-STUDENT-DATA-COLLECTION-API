package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
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
 *  | V74 | ERROR    | List of enrolled program codes must:                                  | NONE         |
 *                     be of an even length: 0, 2, 4, 6, 8, ... 16
 *                     not contain any spaces
 *                     contain only numeric values: 0-9
 */
@Component
@Slf4j
@Order(190)
public class EnrolledProgramParseRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of EnrolledProgramParseRule-V74: for collectionType {} and sdcSchoolCollectionStudentID :: {}", FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes());
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of EnrolledProgramParseRule-V74 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        if (!StringUtils.isNumeric(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes()) || studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes().length() % 2 != 0 || studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes().contains(" ")) {
            log.debug("EnrolledProgramParseRule-V74: Invalid enrolled program code {} for sdcSchoolCollectionStudentID:: {}" , studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.ENROLLED_CODE_PARSE_ERR));
        }
        return errors;
    }
}
