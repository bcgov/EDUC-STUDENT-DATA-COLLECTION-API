package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;

import java.util.LinkedHashMap;
import java.util.Map;

public class SchoolDistrictRule extends BaseRule {
  @Override
  public Map<String, String> validate(final SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity) {
    return new LinkedHashMap<>();
  }
}

