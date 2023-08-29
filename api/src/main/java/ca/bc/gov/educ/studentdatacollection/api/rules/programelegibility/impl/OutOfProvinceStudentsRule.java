package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolFundingCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(3)
public class OutOfProvinceStudentsRule implements ProgramEligibilityBaseRule {

  @Override
  public boolean shouldExecute(SdcStudentSagaData saga, List<ProgramEligibilityIssueCode> list) {
    String fundingCode = saga.getSdcSchoolCollectionStudent().getSchoolFundingCode();
    return StringUtils.isNotEmpty(fundingCode) && StringUtils.equals(fundingCode, SchoolFundingCodes.OUT_OF_PROVINCE.getCode());
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData u) {
    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    errors.add(ProgramEligibilityIssueCode.OUT_OF_PROVINCE);
    return errors;
  }

}
