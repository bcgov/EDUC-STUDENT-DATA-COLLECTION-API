package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.helpers.BooleanString;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order
public class IndigenousSupportProgramsRule implements ProgramEligibilityBaseRule {
  private final ValidationRulesService validationRulesService;

  public IndigenousSupportProgramsRule(ValidationRulesService validationRulesService) {
    this.validationRulesService = validationRulesService;
  }

  @Override
  public boolean shouldExecute(SdcStudentSagaData saga, List<ProgramEligibilityIssueCode> errors) {
    return hasNotViolatedBaseRules(errors);
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData saga) {
    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();

    List<String> studentPrograms = validationRulesService.splitString(saga.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());
    String ancestryData = saga.getSdcSchoolCollectionStudent().getNativeAncestryInd();

    if (EnrolledProgramCodes.getIndigenousProgramCodes().stream().noneMatch(studentPrograms::contains)) {
      errors.add(ProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS);
    }else if (BooleanString.areEqual(saga.getSdcSchoolCollectionStudent().getIsSchoolAged(), Boolean.FALSE)) {
      errors.add(ProgramEligibilityIssueCode.INDIGENOUS_ADULT);
    }else if(StringUtils.isEmpty(ancestryData) || ancestryData.equalsIgnoreCase("N")) {
      errors.add(ProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY);
    }

    return errors;
  }

}
