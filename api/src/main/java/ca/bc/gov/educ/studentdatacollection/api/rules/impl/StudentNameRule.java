package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.BadNameValues;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class StudentNameRule implements BaseRule {
    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        Pattern pattern = Pattern.compile("[^a-z0-9]+", Pattern.CASE_INSENSITIVE);

        //LEGAL LAST NAME
        if (StringUtils.isEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalLastName())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_LAST_NAME_BLANK));
        }

        if (containsBadValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalLastName())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_LAST_NAME_BAD_VALUE));
        }

        if (containsInvalidChars(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalLastName(), pattern)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_LAST_NAME_CHAR_FIX));
        }

        //LEGAL FIRST NAME
        if (containsInvalidChars(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalFirstName(), pattern)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_FIRST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_FIRST_NAME_CHAR_FIX));
        }
        if (containsBadValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalFirstName())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_FIRST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_FIRST_NAME_BAD_VALUE));
        }

        //LEGAL MIDDLE NAME
        if (containsInvalidChars(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalMiddleNames(), pattern)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_MIDDLE_NAMES, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_MIDDLE_NAME_CHAR_FIX));
        }
        if (containsBadValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalMiddleNames())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_MIDDLE_NAMES, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_MIDDLE_NAME_BAD_VALUE));
        }

        //USUAL FIRST NAME
        if (containsInvalidChars(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualFirstName(), pattern)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.USUAL_FIRST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_FIRST_NAME_CHAR_FIX));
        }
        if (containsBadValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualFirstName())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.USUAL_FIRST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_FIRST_NAME_BAD_VALUE));
        }

        //USUAL MIDDLE NAME
        if (containsInvalidChars(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualMiddleNames(), pattern)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.USUAL_MIDDLE_NAMES, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_MIDDLE_NAME_CHAR_FIX));
        }
        if (containsBadValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualMiddleNames())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.USUAL_MIDDLE_NAMES, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_MIDDLE_NAME_BAD_VALUE));
        }

        //USUAL LAST NAME
        if (containsInvalidChars(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualLastName(), pattern)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.USUAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_LAST_NAME_CHAR_FIX));
        }
        if (containsBadValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualLastName())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.USUAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_LAST_NAME_BAD_VALUE));
        }

        return errors;
    }

    private boolean containsBadValue(String name) {
        return StringUtils.isNotEmpty(name) && BadNameValues.findByValue(name).isPresent();
    }

    private boolean containsInvalidChars(String name, Pattern pattern) {
        return StringUtils.isNotEmpty(name) && pattern.matcher(name).find();
    }
}

