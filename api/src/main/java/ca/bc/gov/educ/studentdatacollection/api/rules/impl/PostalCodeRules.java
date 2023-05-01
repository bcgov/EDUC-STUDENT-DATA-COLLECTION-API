package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PostalCodeRules implements BaseRule {
    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if(!sdcStudentSagaData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(Constants.OFFSHORE) &&
                StringUtils.isEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getPostalCode())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.POSTAL_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.MISSING_POSTAL_CODE));
        }

        if(!sdcStudentSagaData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(Constants.OFFSHORE) &&
                StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getPostalCode())) {
            Pattern pattern = Pattern.compile("^[A-Za-z]\\d[A-Za-z]\\d[A-Za-z]\\d$");
            if(!pattern.matcher(sdcStudentSagaData.getSdcSchoolCollectionStudent().getPostalCode()).matches()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.POSTAL_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.INVALID_POSTAL_CODE));
            }
        }
        return errors;
    }

}
