package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ValidationRulesDependencyMatrix;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface ValidationBaseRule extends Rule<SdcStudentSagaData, SdcSchoolCollectionStudentValidationIssue> {
  default SdcSchoolCollectionStudentValidationIssue createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode severityCode, SdcSchoolCollectionStudentValidationFieldCode fieldCode, SdcSchoolCollectionStudentValidationIssueTypeCode typeCode){
    SdcSchoolCollectionStudentValidationIssue sdcSchoolCollectionStudentValidationIssue = new SdcSchoolCollectionStudentValidationIssue();
    sdcSchoolCollectionStudentValidationIssue.setValidationIssueSeverityCode(severityCode.toString());
    sdcSchoolCollectionStudentValidationIssue.setValidationIssueCode(typeCode.getCode());
    sdcSchoolCollectionStudentValidationIssue.setValidationIssueFieldCode(fieldCode.getCode());
    return sdcSchoolCollectionStudentValidationIssue;
  }

  default boolean isValidationDependencyResolved(String fieldName, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
    Optional<ValidationRulesDependencyMatrix> errorCodesToCheck = ValidationRulesDependencyMatrix.findByValue(fieldName);
    if(errorCodesToCheck.isPresent()) {
      String[] errorCodes = errorCodesToCheck.get().getBaseRuleErrorCode();
      return validationErrorsMap.stream().noneMatch(val -> Arrays.stream(errorCodes).anyMatch(val.getValidationIssueCode()::contains));
    }
    return false;
  }
}
