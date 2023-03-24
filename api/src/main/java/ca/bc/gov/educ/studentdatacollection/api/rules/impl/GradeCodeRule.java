package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class GradeCodeRule extends BaseRule {
  @Override
  public Map<String, String> validate(final SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity) {
    return new LinkedHashMap<>();
  }
}
