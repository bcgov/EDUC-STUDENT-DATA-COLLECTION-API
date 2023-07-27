package ca.bc.gov.educ.studentdatacollection.api.rules.validationrulesimpl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                             | Dependent On |
 *  |-----|----------|--------------------------------------------------|--------------|
 *  | V54 | ERROR    | Student in summer school cannot be an adult.     | V04          |
 */
@Component
@Order(400)
public class AdultStudentInSummerSchoolRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                sdcStudentSagaData.getSchool().getFacilityTypeCode().equalsIgnoreCase(Constants.SUMMER) &&
                sdcStudentSagaData.getCollectionTypeCode().equalsIgnoreCase(Constants.JULY) && isValidationDependencyResolved("V54", validationErrorsMap);
    }
    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if(DOBUtil.isAdult(sdcStudentSagaData.getSdcSchoolCollectionStudent().getDob())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.DOB, SdcSchoolCollectionStudentValidationIssueTypeCode.STUDENT_ADULT_ERR));
        }
        return errors;
    }
}
