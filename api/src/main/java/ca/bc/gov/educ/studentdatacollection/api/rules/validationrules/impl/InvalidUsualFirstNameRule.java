package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil.containsInvalidChars;

/**
 *  | ID  | Severity | Rule                                                            | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------|--------------|
 *  | V10 | ERROR    | can only contain Aa-Zz, apostrophes, hyphens, and periods.      | NONE         |
 */
@Component
@Slf4j
@Order(70)
public class InvalidUsualFirstNameRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of InvalidUsualFirstNameRule-V10: for collectionType {} and sdcSchoolCollectionStudentID :: {}" + studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of InvalidUsualFirstNameRule-V10");
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if (containsInvalidChars(studentRuleData.getSdcSchoolCollectionStudentEntity().getUsualFirstName())) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.USUAL_FIRST_NAME, StudentValidationIssueTypeCode.USUAL_FIRST_NAME_CHAR_FIX));
        }
        log.debug("InvalidUsualFirstNameRule-V10 has errors::" + errors);
        return errors;
    }

}

