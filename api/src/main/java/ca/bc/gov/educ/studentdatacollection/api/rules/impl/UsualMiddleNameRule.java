package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil.containsBadValue;

@Component
public class UsualMiddleNameRule implements BaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if (containsBadValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getUsualMiddleNames())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.USUAL_MIDDLE_NAMES, SdcSchoolCollectionStudentValidationIssueTypeCode.USUAL_MIDDLE_NAME_BAD_VALUE));
        }

        return errors;
    }

}

