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
import java.util.regex.Matcher;
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
        Pattern pattern = Pattern.compile("[^a-z]", Pattern.CASE_INSENSITIVE);

        //LEGAL LAST NAME
        if (StringUtils.isEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalLastName())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_LAST_NAME_BLANK));
        } else {
            Matcher lastNameMatcher = pattern.matcher(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalLastName());
            if (lastNameMatcher.find()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_LAST_NAME_CHAR_FIX));
            }
            if(BadNameValues.findByValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalLastName()).isPresent()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_LAST_NAME_BAD_VALUE));
            }
        }

    //LEGAL FIRST NAME
        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalFirstName())) {
            Matcher firstNameMatcher = pattern.matcher(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalFirstName());
            if (firstNameMatcher.find()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_FIRST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_FIRST_NAME_CHAR_FIX));
            }
            if(BadNameValues.findByValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalFirstName()).isPresent()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_FIRST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_FIRST_NAME_BAD_VALUE));
            }
        }

        //LEGAL MIDDLE NAME
        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalMiddleNames())) {
            Matcher middleNameMatcher = pattern.matcher(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalMiddleNames());
            if (middleNameMatcher.find()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_MIDDLE_NAMES, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_MIDDLE_NAME_CHAR_FIX));
            }
            if(BadNameValues.findByValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getLegalMiddleNames()).isPresent()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.LEGAL_MIDDLE_NAMES, SdcSchoolCollectionStudentValidationIssueTypeCode.LEGAL_MIDDLE_NAME_BAD_VALUE));
            }
        }

        //USUAL FIRST NAME
        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualFirstName())) {
            Matcher middleNameMatcher = pattern.matcher(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualFirstName());
            if (middleNameMatcher.find()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.USUAL_FIRST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_FIRST_NAME_CHAR_FIX));
            }
            if(BadNameValues.findByValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualFirstName()).isPresent()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.USUAL_FIRST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_FIRST_NAME_BAD_VALUE));
            }
        }

        //USUAL MIDDLE NAME
        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualMiddleNames())) {
            Matcher middleNameMatcher = pattern.matcher(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualMiddleNames());
            if (middleNameMatcher.find()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.USUAL_MIDDLE_NAMES, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_MIDDLE_NAME_CHAR_FIX));
            }
            if(BadNameValues.findByValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualMiddleNames()).isPresent()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.USUAL_MIDDLE_NAMES, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_MIDDLE_NAME_BAD_VALUE));
            }
        }

        //USUAL LAST NAME
        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualLastName())) {
            Matcher middleNameMatcher = pattern.matcher(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualLastName());
            if (middleNameMatcher.find()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.USUAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_LAST_NAME_CHAR_FIX));
            }
            if(BadNameValues.findByValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualLastName()).isPresent()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.USUAL_LAST_NAME, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_LAST_NAME_BAD_VALUE));
            }
        }

        return errors;
    }
}

