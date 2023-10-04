package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Order(6)
public class TooYoungRule implements ProgramEligibilityBaseRule {

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> list) {
    return true;
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    log.debug("In executeValidation of ProgramEligibilityBaseRule - TooYoungRule for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();

    if(!DOBUtil.is5YearsOldByDec31ThisSchoolYear(studentRuleData.getSdcSchoolCollectionStudentEntity().getDob())) {
      log.debug("In executeValidation of ProgramEligibilityBaseRule - TooYoungRule: DOB - {} for sdcSchoolCollectionStudentID :: {}" , studentRuleData.getSdcSchoolCollectionStudentEntity().getDob(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
      errors.add(ProgramEligibilityIssueCode.TOO_YOUNG);
    }
    return errors;
  }

}
