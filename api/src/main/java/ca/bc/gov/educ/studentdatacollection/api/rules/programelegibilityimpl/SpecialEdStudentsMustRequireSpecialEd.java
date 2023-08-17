package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibilityimpl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

@Component
@Order(6)
public class SpecialEdStudentsMustRequireSpecialEd implements ProgramEligibilityBaseRule {
  private final ValidationRulesService validationRulesService;

  public SpecialEdStudentsMustRequireSpecialEd(ValidationRulesService validationRulesService) {
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

    List<String> activeSpecialEdPrograms = validationRulesService
      .getActiveSpecialEducationCategoryCodes()
      .stream().map(e -> e.getSpecialEducationCategoryCode()).toList();

    if (!activeSpecialEdPrograms.contains(saga.getSdcSchoolCollectionStudent().getSpecialEducationCategoryCode())) {
      errors.add(SdcSchoolCollectionStudentProgramEligibilityIssueCode.DOES_NOT_NEED_SPECIAL_ED);
    }

    return errors;
  }

}
