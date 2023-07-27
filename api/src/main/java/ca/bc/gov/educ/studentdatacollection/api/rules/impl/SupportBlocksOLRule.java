package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V66 | ERROR    | Students reported by a Provincial Online Learning or District     | V67  |
 *                     Online Learning School should be reported with a 0 or blank
 *                     value for Support Blocks.
 */
@Component
@Order(520)
public class SupportBlocksOLRule implements BaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes
                .findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode())
                .isPresent()
                && !sdcStudentSagaData.getCollectionTypeCode().equals(Constants.JULY)
                && isValidationDependencyResolved("V66", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final SdcSchoolCollectionStudent student = sdcStudentSagaData.getSdcSchoolCollectionStudent();
        final String supportBlocks = student.getSupportBlocks();

        final String facultyTypeCode = sdcStudentSagaData.getSchool().getFacilityTypeCode();
        if ((facultyTypeCode.equals(Constants.PROV_ONLINE) || facultyTypeCode.equals(Constants.DISTRICT_ONLINE))
                && (StringUtils.isNotEmpty(supportBlocks)
                && !supportBlocks.equals("0"))) {
            errors.add(createValidationIssue(
                    SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR,
                    SdcSchoolCollectionStudentValidationFieldCode.SUPPORT_BLOCKS,
                    SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_FACILITY_NA
            ));
        }
        return errors;
    }

}
