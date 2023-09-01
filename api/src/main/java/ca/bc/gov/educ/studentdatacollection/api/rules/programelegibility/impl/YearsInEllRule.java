package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
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
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> errors) {
    return hasNotViolatedBaseRules(errors);
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
    List<String> studentPrograms = validationRulesService.splitString(student.getEnrolledProgramCodes());

    if(!studentPrograms.contains(EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode())){
      errors.add(ProgramEligibilityIssueCode.NOT_ENROLLED_ELL);
    }

    var totalYearsInEll = 0;
    if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() != null) {
      var yearsInEllEntityOptional = validationRulesService.getStudentYearsInEll(student.getAssignedStudentId().toString());

      if (yearsInEllEntityOptional.isPresent()) {
        totalYearsInEll = yearsInEllEntityOptional.get().getYearsInEll();
      }
    }

    if (errors.isEmpty() && (Boolean.FALSE.equals(student.getIsSchoolAged()) || totalYearsInEll >= 5)) {
      errors.add(ProgramEligibilityIssueCode.YEARS_IN_ELL);
    }
    return errors;
  }

}
