package ca.bc.gov.educ.studentdatacollection.api.rules.validationrulesimpl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                  | Dependent On |
 *  |-----|----------|-------------------------------------------------------|--------------|
 *  | V05 | ERROR    | Gender must be one of M or F. It cannot be blank.     |  NONE     |
 *
 */
@Component
@Order(20)
public class GenderRule implements ValidationBaseRule {
  @Override
  public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
    return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent();
  }

  @Override
  public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
    final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
    if(StringUtils.isEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getGender()) || (!sdcStudentSagaData.getSdcSchoolCollectionStudent().getGender().equals("M") && !sdcStudentSagaData.getSdcSchoolCollectionStudent().getGender().equals("F"))) {
      errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.GENDER_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.GENDER_INVALID));
    }
    return errors;
  }
}
