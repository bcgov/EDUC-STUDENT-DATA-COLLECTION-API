package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcStudentFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;

import java.util.LinkedHashMap;
import java.util.Map;

public class GenderRule extends BaseRule {
  @Override
  public Map<String, String> validate(final SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity) {
    final Map<String, String> errorsMap = new LinkedHashMap<>();
    if(!sdcSchoolStudentEntity.getGender().equals("M")){
      errorsMap.put(SdcStudentFieldCode.GENDER_CODE.getCode(), String.format("Gender code %s is not recognized.", sdcSchoolStudentEntity.getGender()));
    }
    return errorsMap;
  }
}
