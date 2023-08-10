package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
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

  public List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> processRules(SdcStudentSagaData sdcStudentSagaData) {
    final List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> validationErrorsMap = new ArrayList<>();
    rules.forEach(rule -> {
      if(rule.shouldExecute(sdcStudentSagaData, validationErrorsMap)) {
        validationErrorsMap.addAll(rule.executeValidation(sdcStudentSagaData));
      }
    });
    return validationErrorsMap;
  }

}
