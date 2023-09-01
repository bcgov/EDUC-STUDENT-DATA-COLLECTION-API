package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
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
 *  | V66 | ERROR    | Students reported by a Provincial Online Learning or District     | V67  |
 *                     Online Learning School should be reported with a 0 or blank
 *                     value for Support Blocks.
 */
@Component
@Order(520)
public class SupportBlocksOLRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes
                .findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode())
                .isPresent()
                && !studentRuleData.getCollectionTypeCode().equals(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V66", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        final String supportBlocks = student.getSupportBlocks();

        final String facultyTypeCode = studentRuleData.getSchool().getFacilityTypeCode();
        if ((facultyTypeCode.equals(FacilityTypeCodes.DIST_LEARN.getCode()) || facultyTypeCode.equals(FacilityTypeCodes.DISTONLINE.getCode()))
                && (StringUtils.isNotEmpty(supportBlocks)
                && !supportBlocks.equals("0"))) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.SUPPORT_BLOCKS, StudentValidationIssueTypeCode.SUPPORT_FACILITY_NA));
        }
        return errors;
    }

}
