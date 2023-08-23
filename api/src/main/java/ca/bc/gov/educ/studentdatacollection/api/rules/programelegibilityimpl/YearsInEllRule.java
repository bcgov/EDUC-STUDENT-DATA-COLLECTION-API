package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibilityimpl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.helpers.BooleanString;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import io.micrometer.common.util.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order
public class YearsInEllRule implements ProgramEligibilityBaseRule {
  private final ValidationRulesService validationRulesService;

  public YearsInEllRule(ValidationRulesService validationRulesService) {
    this.validationRulesService = validationRulesService;
  }

  @Override
  public boolean shouldExecute(SdcStudentSagaData saga, List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors) {
    return hasNotViolatedBaseRules(errors);
  }

  @Override
  public List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData saga) {
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = new ArrayList<>();
    var student = saga.getSdcSchoolCollectionStudent();
    List<String> studentPrograms = validationRulesService.splitString(student.getEnrolledProgramCodes());

    if(!studentPrograms.contains(EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode())){
      errors.add(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_ELL);
    }

    var totalYearsInEll = 0;
    if(StringUtils.isNotEmpty(saga.getSdcSchoolCollectionStudent().getAssignedStudentId())) {
      var yearsInEllEntityOptional = validationRulesService.getStudentYearsInEll(student.getAssignedStudentId());

      if (yearsInEllEntityOptional.isPresent()) {
        totalYearsInEll = yearsInEllEntityOptional.get().getYearsInEll();
      }
    }

    if (errors.isEmpty() && (BooleanString.areEqual(saga.getSdcSchoolCollectionStudent().getIsSchoolAged(), Boolean.FALSE) || totalYearsInEll >= 5)) {
      errors.add(SdcSchoolCollectionStudentProgramEligibilityIssueCode.YEARS_IN_ELL);
    }
    return errors;
  }

}
