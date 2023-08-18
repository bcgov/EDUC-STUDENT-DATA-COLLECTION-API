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
@Order
public class SpecialEdStudentsMustBeSchoolAgeOrNotGraduated implements ProgramEligibilityBaseRule {

  @Override
  public boolean shouldExecute(SdcStudentSagaData saga,
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors) {
    return hasNotViolatedBaseRules(errors)
    && !errors.contains(SdcSchoolCollectionStudentProgramEligibilityIssueCode.DOES_NOT_NEED_SPECIAL_ED);
  }

  @Override
  public List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData saga) {
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = new ArrayList<>();

    String isGraduated = saga.getSdcSchoolCollectionStudent().getIsGraduated();

    if (StringUtils.isNotEmpty(isGraduated) && isGraduated.equals("true")) {
      errors.add(SdcSchoolCollectionStudentProgramEligibilityIssueCode.IS_GRADUATED);
    }

    return errors;
  }

}
