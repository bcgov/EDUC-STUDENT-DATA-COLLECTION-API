package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Order
public class YearsInEllRule implements ProgramEligibilityBaseRule {
  private final ValidationRulesService validationRulesService;

  public YearsInEllRule(ValidationRulesService validationRulesService) {
    this.validationRulesService = validationRulesService;
  }

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> errors) {
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - YearsInEllRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    log.debug("In shouldExecute of ProgramEligibilityBaseRule - YearsInEllRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
            hasNotViolatedBaseRules(errors),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    return hasNotViolatedBaseRules(errors);
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    log.debug("In executeValidation of ProgramEligibilityBaseRule - YearsInEllRule for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
    List<String> studentPrograms = validationRulesService.splitEnrolledProgramsString(student.getEnrolledProgramCodes());
    log.debug("In executeValidation of ProgramEligibilityBaseRule - YearsInEllRule: Enrolled Program - {} for sdcSchoolCollectionStudentID :: {}", studentPrograms, studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    if(!studentPrograms.contains(EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode())){
      errors.add(ProgramEligibilityIssueCode.NOT_ENROLLED_ELL);
    }

    //Ensure that PEN Match has been run for the student.
    if (student.getIsGraduated() == null) {
      validationRulesService.updatePenMatchAndGradStatusColumns(student, studentRuleData.getSchool().getMincode());
    }

    var totalYearsInEll = student.getYearsInEll() != null ? student.getYearsInEll(): 0;

    if (errors.isEmpty() && (Boolean.FALSE.equals(student.getIsSchoolAged()) || totalYearsInEll >= 5)) {
      errors.add(ProgramEligibilityIssueCode.YEARS_IN_ELL);
    } else if(errors.isEmpty() && SchoolCategoryCodes.INDEPENDENTS.contains(studentRuleData.getSchool().getSchoolCategoryCode())) {
      errors.add(ProgramEligibilityIssueCode.ELL_INDY_SCHOOL);
    }
    return errors;
  }

}
