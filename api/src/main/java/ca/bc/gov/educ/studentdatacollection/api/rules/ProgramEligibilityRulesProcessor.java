package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProgramEligibilityRulesProcessor {
  private final List<ProgramEligibilityBaseRule> rules;

  @Autowired
  public ProgramEligibilityRulesProcessor(final List<ProgramEligibilityBaseRule> rules) {
    this.rules = rules;
  }

  public List<ProgramEligibilityIssueCode> processRules(StudentRuleData studentRuleData) {
    final List<ProgramEligibilityIssueCode> validationErrorsMap = new ArrayList<>();
    rules.forEach(rule -> {
      if(rule.shouldExecute(studentRuleData, validationErrorsMap)) {
        validationErrorsMap.addAll(rule.executeValidation(studentRuleData));
      }
    });
    return validationErrorsMap;
  }

}
