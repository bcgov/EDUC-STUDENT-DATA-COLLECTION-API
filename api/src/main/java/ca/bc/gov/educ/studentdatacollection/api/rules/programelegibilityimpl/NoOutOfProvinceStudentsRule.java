package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibilityimpl;

import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolFundingCodes;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

@Component
@Order(3)
public class NoOutOfProvinceStudentsRule implements ProgramEligibilityBaseRule {

  @Override
  public boolean shouldExecute(SdcStudentSagaData saga, List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> list) {
    String fundingCode = saga.getSdcSchoolCollectionStudent().getSchoolFundingCode();
    return StringUtils.isNotEmpty(fundingCode) && StringUtils.equals(fundingCode, SchoolFundingCodes.OUT_OF_PROVINCE.getCode());
  }

  @Override
  public List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData u) {
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = new ArrayList<>();
    errors.add(SdcSchoolCollectionStudentProgramEligibilityIssueCode.OUT_OF_PROVINCE);
    return errors;
  }

}
