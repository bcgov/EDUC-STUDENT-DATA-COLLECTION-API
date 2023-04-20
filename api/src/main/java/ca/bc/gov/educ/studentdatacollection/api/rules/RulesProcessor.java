package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RulesProcessor {
  private final List<BaseRule> rules;

  @Autowired
  public RulesProcessor(final List<BaseRule> rules) {
    this.rules = rules;
  }

  public List<SdcSchoolCollectionStudentValidationIssue> processRules(SdcStudentSagaData sdcStudentSagaData) {
    final List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap = new ArrayList<>();
    rules.stream().filter(rule-> rule.shouldExecute(sdcStudentSagaData)).forEach(rule -> validationErrorsMap.addAll(rule.executeValidation(sdcStudentSagaData)));
    return validationErrorsMap;
  }
}
