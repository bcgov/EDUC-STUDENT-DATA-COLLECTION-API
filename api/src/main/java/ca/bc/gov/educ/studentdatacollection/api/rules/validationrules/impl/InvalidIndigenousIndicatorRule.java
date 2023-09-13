package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V27 | ERROR    | A student's Indigenous Ancestry Indicator must be Y or N.         | NONE         |
 *                     It cannot be blank.
 */
@Component
@Slf4j
@Order(110)
public class InvalidIndigenousIndicatorRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of InvalidIndigenousIndicatorRule-V27: for collectionType {} and sdcSchoolCollectionStudentID :: {}" + studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent();
    }
    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of InvalidIndigenousIndicatorRule-V27, checking for native ancestry indicator::"+studentRuleData.getSdcSchoolCollectionStudentEntity().getNativeAncestryInd());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

        if(StringUtils.isEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getNativeAncestryInd()) || (!student.getNativeAncestryInd().equals("Y") && !student.getNativeAncestryInd().equals("N"))) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.NATIVE_ANCESTRY_IND, StudentValidationIssueTypeCode.NATIVE_IND_INVALID));
        }
        log.debug("InvalidIndigenousIndicatorRule-V27 has errors::" + errors);
        return errors;
    }
}
