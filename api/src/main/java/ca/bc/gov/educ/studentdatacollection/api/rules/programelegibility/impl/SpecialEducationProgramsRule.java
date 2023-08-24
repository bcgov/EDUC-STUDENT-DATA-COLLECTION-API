package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(6)
public class SpecialEducationProgramsRule implements ProgramEligibilityBaseRule {
  private final ValidationRulesService validationRulesService;

  public SpecialEducationProgramsRule(ValidationRulesService validationRulesService) {
    this.validationRulesService = validationRulesService;
  }

  @Override
  public boolean shouldExecute(SdcStudentSagaData saga, List<ProgramEligibilityIssueCode> errors) {
    return hasNotViolatedBaseRules(errors);
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData saga) {
    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();

    List<String> activeSpecialEdPrograms = validationRulesService.getActiveSpecialEducationCategoryCodes().stream().map(e -> e.getSpecialEducationCategoryCode()).toList();

    String isGraduated = saga.getSdcSchoolCollectionStudent().getIsGraduated();
    boolean isGraduatedVal = StringUtils.isNotEmpty(isGraduated) && isGraduated.equals("true");
    String isSchoolAged = saga.getSdcSchoolCollectionStudent().getIsSchoolAged();
    boolean isSchoolAgedVal = StringUtils.isNotEmpty(isSchoolAged) && isSchoolAged.equals("true");
    String isAdult = saga.getSdcSchoolCollectionStudent().getIsAdult();
    boolean isAdultVal = StringUtils.isNotEmpty(isAdult) && isAdult.equals("true");
    boolean isNonGraduatedAdult = !isGraduatedVal && isAdultVal;

    if (StringUtils.isEmpty(saga.getSdcSchoolCollectionStudent().getSpecialEducationCategoryCode()) || !activeSpecialEdPrograms.contains(saga.getSdcSchoolCollectionStudent().getSpecialEducationCategoryCode())) {
      errors.add(ProgramEligibilityIssueCode.NOT_ENROLLED_SPECIAL_ED);
    }else if (!isSchoolAgedVal && !isNonGraduatedAdult) {
      errors.add(ProgramEligibilityIssueCode.NON_ELIG_SPECIAL_EDUCATION);
    }

    return errors;
  }

}
