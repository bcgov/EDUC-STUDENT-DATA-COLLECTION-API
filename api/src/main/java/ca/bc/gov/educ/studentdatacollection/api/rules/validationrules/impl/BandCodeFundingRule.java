package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolFundingCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
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
@Order(340)
public class BandCodeFundingRule implements ValidationBaseRule {
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent() &&
                isValidationDependencyResolved("V40", validationErrorsMap);
    }
    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

        if((StringUtils.isEmpty(student.getBandCode()) && (StringUtils.isNotEmpty(student.getSchoolFundingCode())
                && student.getSchoolFundingCode().equals(SchoolFundingCodes.STATUS_FIRST_NATION.getCode()))) ||
                (StringUtils.isNotEmpty(student.getBandCode()) && (StringUtils.isEmpty(student.getSchoolFundingCode())
                || !student.getSchoolFundingCode().equals(SchoolFundingCodes.STATUS_FIRST_NATION.getCode())))) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.BAND_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.BAND_CODE_BLANK));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.SCHOOL_FUNDING_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.BAND_CODE_BLANK));
        }
        return errors;
    }
}
