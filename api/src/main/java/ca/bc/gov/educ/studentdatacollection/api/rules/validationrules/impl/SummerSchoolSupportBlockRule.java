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
 *  | ID  | Severity | Rule                                                        | Dependent On |
 *  |-----|----------|-------------------------------------------------------------|--------------|
 *  | V71 | ERROR    | Student cannot be reported with any Support Blocks.         | V67  |
 *                     Support Block must be reported as a 0 or a blank.
 */
@Component
@Order(500)
public class SummerSchoolSupportBlockRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent() &&
                studentRuleData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.SUMMER.getCode()) &&
                studentRuleData.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V71", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if(StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getSupportBlocks()) && !studentRuleData.getSdcSchoolCollectionStudentEntity().getSupportBlocks().equals("0")) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.SUPPORT_BLOCKS, StudentValidationIssueTypeCode.SUPPORT_BLOCKS_NA));
        }
        return errors;
    }
}
