package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RulesProcessor {
  private final List<ValidationBaseRule> rules;

  @Autowired
  public RulesProcessor(final List<ValidationBaseRule> rules) {
    this.rules = rules;
  }

  public List<SdcSchoolCollectionStudentValidationIssue> processRules(StudentRuleData ruleStudent) {
    final List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap = new ArrayList<>();
    rules.forEach(rule -> {
      if(rule.shouldExecute(ruleStudent, validationErrorsMap)) {
        validationErrorsMap.addAll(rule.executeValidation(ruleStudent));
      }
    });
    return validationErrorsMap;
  }
}
