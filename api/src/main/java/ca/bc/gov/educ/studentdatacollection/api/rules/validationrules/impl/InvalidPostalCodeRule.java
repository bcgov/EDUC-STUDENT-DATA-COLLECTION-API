package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V64 | WARNING  | If the reporting school is not an Offshore School and a               | V63          |
 *                     postal code is reported, it must be a valid Canadian postal
 *                     code in the format A#A#A#.
 */
@Component
@Order(330)
public class InvalidPostalCodeRule implements ValidationBaseRule {
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent() &&
                isValidationDependencyResolved("V64", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if(!studentRuleData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.OFFSHORE.getCode())) {
            Pattern pattern = Pattern.compile("^[A-Za-z]\\d[A-Za-z]\\d[A-Za-z]\\d$");
            if(!pattern.matcher(studentRuleData.getSdcSchoolCollectionStudentEntity().getPostalCode()).matches()) {
                errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.INFO_WARNING, SdcSchoolCollectionStudentValidationFieldCode.POSTAL_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.INVALID_POSTAL_CODE));
            }
        }
        return errors;
    }

}
