package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GradeCodeRule extends BaseRule {
  @Override
  public List<SdcSchoolCollectionStudentValidationIssue> validate(final SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity) {
    return new ArrayList<>();
  }
}
