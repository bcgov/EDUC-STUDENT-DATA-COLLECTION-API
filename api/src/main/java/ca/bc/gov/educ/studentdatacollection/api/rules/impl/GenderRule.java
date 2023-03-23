package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;

import java.util.LinkedHashMap;
import java.util.Map;

public class GenderRule extends BaseRule {
  @Override
  public Map<String, String> validate(final SdcSchoolStudentEntity sdcSchoolStudentEntity) {
    return new LinkedHashMap<>();
  }
}
