package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibilityimpl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
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

    boolean isGraduated = saga.getSdcSchoolCollectionStudent().getIsGraduated();
    if (isGraduated) {
      errors.add(SdcSchoolCollectionStudentProgramEligibilityIssueCode.IS_GRADUATED);
    }

    return errors;
  }

}
