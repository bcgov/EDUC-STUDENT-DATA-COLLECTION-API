package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order
public class FrenchStudentsMustBeEnrolledRule implements ProgramEligibilityBaseRule {
  private final ValidationRulesService validationRulesService;

  public FrenchStudentsMustBeEnrolledRule(ValidationRulesService validationRulesService) {
    this.validationRulesService = validationRulesService;
  }

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> errors) {
    return hasNotViolatedBaseRules(errors);
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();

    List<String> studentPrograms = validationRulesService.splitString(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes());

    if (EnrolledProgramCodes.getFrenchProgramCodes().stream().noneMatch(studentPrograms::contains)) {
      errors.add(ProgramEligibilityIssueCode.NOT_ENROLLED_FRENCH);
    }

    return errors;
  }

}
