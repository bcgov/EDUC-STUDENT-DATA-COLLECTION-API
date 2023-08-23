package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                                       | Dependent On |
 *  |-----|----------|--------------------------------------------------------------------------------------------|--------------|
 *  | V63 | WARNING  | If the reporting school is not an Offshore School, then postal code should be reported.    | NONE         |
 */
@Component
@Order(230)
public class EmptyPostalCodeRule implements ValidationBaseRule {
    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if(!sdcStudentSagaData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.OFFSHORE.getCode()) &&
                StringUtils.isEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getPostalCode())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.INFO_WARNING, SdcSchoolCollectionStudentValidationFieldCode.POSTAL_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.MISSING_POSTAL_CODE));
        }
        return errors;
    }

}
