package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                  | Dependent On |
 *  |-----|----------|-------------------------------------------------------|--------------|
 *  | V05 | ERROR    | Gender must be one of M or F. It cannot be blank.     |  NONE     |
 *
 */
@Component
@Order(20)
public class GenderRule implements ValidationBaseRule {
  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
    return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent();
  }

  @Override
  public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
    final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
    if(StringUtils.isEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getGender()) || (!studentRuleData.getSdcSchoolCollectionStudentEntity().getGender().equals("M") && !studentRuleData.getSdcSchoolCollectionStudentEntity().getGender().equals("F"))) {
      errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.GENDER_CODE, StudentValidationIssueTypeCode.GENDER_INVALID));
    }
    return errors;
  }
}
