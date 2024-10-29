package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Order(20)
public class GenderRule implements ValidationBaseRule {

  private final CodeTableService codeTableService;

  public GenderRule(CodeTableService codeTableService) {
      this.codeTableService = codeTableService;
  }

    @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
    log.debug("In shouldExecute of GenderRule-V05: for collectionType {} and sdcSchoolCollectionStudentID :: {}", FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    return true;
  }

  @Override
  public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
    log.debug("In executeValidation of GenderRule-V05 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

    var studentGender = studentRuleData.getSdcSchoolCollectionStudentEntity().getGender();
    var codeTableValues = codeTableService.getAllGenderCodes();

    if(StringUtils.isEmpty(studentGender) || codeTableValues.stream().noneMatch(genderCodeEntity -> genderCodeEntity.getGenderCode().equalsIgnoreCase(studentGender))) {
      log.debug("GenderRule-V05: Invalid Gender value {} for sdcSchoolCollectionStudentID:: {}" , studentRuleData.getSdcSchoolCollectionStudentEntity().getGender(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
      errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.GENDER_CODE, StudentValidationIssueTypeCode.GENDER_INVALID));
    }

    return errors;
  }
}
