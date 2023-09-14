package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

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

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V40 | ERROR    | Students reported with a Funding Code of 20                           | V26, V41     |
 *                     (Ordinarily Living on Reserve) or a Band Code, must be reported with
 *                     both a  Funding Code of 20 (Ordinarily Living on Reserve) and a Band Code.
 */
@Component
@Slf4j
@Order(340)
public class BandCodeFundingRule implements ValidationBaseRule {
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of BandCodeFundingRule-V40: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent() &&
                isValidationDependencyResolved("V40", validationErrorsMap);
    }
    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of BandCodeFundingRule-V40 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

        if((StringUtils.isEmpty(student.getBandCode()) && (StringUtils.isNotEmpty(student.getSchoolFundingCode())
                && student.getSchoolFundingCode().equals(SchoolFundingCodes.STATUS_FIRST_NATION.getCode()))) ||
                (StringUtils.isNotEmpty(student.getBandCode()) && (StringUtils.isEmpty(student.getSchoolFundingCode())
                || !student.getSchoolFundingCode().equals(SchoolFundingCodes.STATUS_FIRST_NATION.getCode())))) {
            log.debug("BandCodeFundingRule-V40: Incorrect values of band code {} and funding code {} for sdcSchoolCollectionStudentID:: {}" , student.getBandCode(), student.getSchoolFundingCode(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.BAND_CODE, StudentValidationIssueTypeCode.BAND_CODE_BLANK));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.SCHOOL_FUNDING_CODE, StudentValidationIssueTypeCode.BAND_CODE_BLANK));
        }
        return errors;
    }
}
