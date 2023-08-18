package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibilityimpl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.IndigenousPrograms;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

@Component
@Order(7)
public class IndigenousStudentsMustBeEnrolled implements ProgramEligibilityBaseRule {
  private final ValidationRulesService validationRulesService;

  public IndigenousStudentsMustBeEnrolled(ValidationRulesService validationRulesService) {
    this.validationRulesService = validationRulesService;
  }

  @Override
  public boolean shouldExecute(SdcStudentSagaData saga,
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors) {
    return hasNotViolatedBaseRules(errors);
  }

  @Override
  public List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData saga) {
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = new ArrayList<>();

    List<String> studentPrograms = validationRulesService
      .splitString(saga.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());

    if (IndigenousPrograms.getCodes().stream().noneMatch(studentPrograms::contains)) {
      errors.add(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS);
    }

    return errors;
  }

}
