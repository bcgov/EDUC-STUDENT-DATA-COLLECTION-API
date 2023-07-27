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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
/**
 *  | ID  | Severity | Rule                                                        | Dependent On |
 *  |-----|----------|-------------------------------------------------------------|--------------|
 *  | V71 | ERROR    | Student cannot be reported with any Support Blocks.         | V67  |
 *                     Support Block must be reported as a 0 or a blank.
 */
@Component
@Order(500)
public class SummerSchoolSupportBlockRule implements BaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                sdcStudentSagaData.getSchool().getFacilityTypeCode().equalsIgnoreCase(Constants.SUMMER) &&
                sdcStudentSagaData.getCollectionTypeCode().equalsIgnoreCase(Constants.JULY)
                && isValidationDependencyResolved("V71", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSupportBlocks()) && !sdcStudentSagaData.getSdcSchoolCollectionStudent().getSupportBlocks().equals("0")) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.SUPPORT_BLOCKS, SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_BLOCKS_NA));
        }
        return errors;
    }
}
