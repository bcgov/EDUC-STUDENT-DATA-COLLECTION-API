package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.val;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RulesProcessor {
  private final List<Rule> rules;

  public RulesProcessor(final List<Rule> rules) {
    this.rules = rules;
  }

  public List<SdcSchoolCollectionStudentValidationIssue> processRules(final SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity) {
    final List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap = new ArrayList<>();
    this.rules.forEach(rule -> {
      val valErrors = rule.validate(sdcSchoolStudentEntity);
      if (!valErrors.isEmpty()) {
        validationErrorsMap.addAll(valErrors);
      }
    });
    return validationErrorsMap;
  }
}
