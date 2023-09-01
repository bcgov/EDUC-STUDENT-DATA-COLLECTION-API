package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(6)
public class TooYoungRule implements ProgramEligibilityBaseRule {

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> list) {
    return true;
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();

    if(!DOBUtil.is5YearsOldByDec31ThisSchoolYear(studentRuleData.getSdcSchoolCollectionStudentEntity().getDob())) {
      errors.add(ProgramEligibilityIssueCode.TOO_YOUNG);
    }
    return errors;
  }

}
