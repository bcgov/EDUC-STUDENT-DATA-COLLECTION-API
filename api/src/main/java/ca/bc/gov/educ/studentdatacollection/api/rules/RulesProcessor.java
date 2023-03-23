package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolStudentEntity;
import lombok.val;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RulesProcessor {
  private final List<Rule> rules;

  public RulesProcessor(final List<Rule> rules) {
    this.rules = rules;
  }

  public Map<String, String> processRules(final SdcSchoolStudentEntity sdcSchoolStudentEntity) {
    final Map<String, String> validationErrorsMap = new LinkedHashMap<>();
    this.rules.forEach(rule -> {
      val valErrors = rule.validate(sdcSchoolStudentEntity);
      if (!valErrors.isEmpty()) {
        validationErrorsMap.putAll(valErrors);
      }
    });
    return validationErrorsMap;
  }
}
