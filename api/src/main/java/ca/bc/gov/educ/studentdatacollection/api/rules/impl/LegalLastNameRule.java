package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil.containsBadValue;
import static ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil.containsInvalidChars;

@Component
public class LegalLastNameRule implements BaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if (StringUtils.isEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalLastName())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_LAST_NAME_BLANK));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_FIRST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_LAST_NAME_BLANK));
        }

        if (containsBadValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalLastName())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_LAST_NAME_BAD_VALUE));
        }

        if (containsInvalidChars(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalLastName())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_LAST_NAME_CHAR_FIX));
        }
        return errors;
    }

}

