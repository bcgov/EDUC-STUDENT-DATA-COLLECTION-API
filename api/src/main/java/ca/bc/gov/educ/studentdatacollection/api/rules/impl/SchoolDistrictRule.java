package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;

import java.util.ArrayList;
import java.util.List;

public class SchoolDistrictRule extends BaseRule {
  @Override
  public List<SdcSchoolCollectionStudentValidationIssue> validate(final SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity) {
    return new ArrayList<>();
  }
}

