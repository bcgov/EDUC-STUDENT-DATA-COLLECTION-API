package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibilityimpl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

@Component
@Order(7)
public class IndigenousStudentsMustHaveIndigenousAncestry implements ProgramEligibilityBaseRule {

  @Override
  public boolean shouldExecute(SdcStudentSagaData saga,
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors) {
    return hasNotViolatedBaseRules(errors)
    && hasNotViolatedIndigenousRules(errors);
  }

  @Override
  public List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData saga) {
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = new ArrayList<>();
    String ancestryData = saga.getSdcSchoolCollectionStudent().getNativeAncestryInd();

    if (StringUtils.isEmpty(ancestryData) || ancestryData.toUpperCase().equals("N")) {
      errors.add(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY);
    };

    return errors;
  }

}
