package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;

import java.util.ArrayList;
import java.util.List;

public class GenderRule extends BaseRule {
  @Override
  public List<SdcSchoolCollectionStudentValidationIssue> validate(final SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity) {
    final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
    if(!sdcSchoolStudentEntity.getGender().equals("M")){
      errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.GENDER_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.GENDER_ERR));
    }
    return errors;
  }
}
