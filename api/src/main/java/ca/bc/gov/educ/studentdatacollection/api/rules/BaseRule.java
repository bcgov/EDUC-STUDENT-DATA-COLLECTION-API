package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;

public abstract class BaseRule implements Rule{
  public SdcSchoolCollectionStudentValidationIssue createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode severityCode, SdcSchoolCollectionStudentValidationFieldCode fieldCode, SdcSchoolCollectionStudentValidationIssueTypeCode typeCode){
    SdcSchoolCollectionStudentValidationIssue sdcSchoolCollectionStudentValidationIssue = new SdcSchoolCollectionStudentValidationIssue();
    sdcSchoolCollectionStudentValidationIssue.setValidationIssueSeverityCode(severityCode.toString());
    sdcSchoolCollectionStudentValidationIssue.setValidationIssueCode(typeCode.getCode());
    sdcSchoolCollectionStudentValidationIssue.setValidationIssueFieldCode(fieldCode.getCode());
    return sdcSchoolCollectionStudentValidationIssue;
  }
}
