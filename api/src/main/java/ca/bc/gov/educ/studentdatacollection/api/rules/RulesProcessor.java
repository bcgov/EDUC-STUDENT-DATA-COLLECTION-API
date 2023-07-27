package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
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

  public List<SdcSchoolCollectionStudentValidationIssue> processRules(SdcStudentSagaData sdcStudentSagaData) {
    final List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap = new ArrayList<>();
    rules.forEach(rule -> {
      if(rule.shouldExecute(sdcStudentSagaData, validationErrorsMap)) {
        validationErrorsMap.addAll(rule.executeValidation(sdcStudentSagaData));
      }
    });
    return validationErrorsMap;
  }
}
