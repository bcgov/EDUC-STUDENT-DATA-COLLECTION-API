package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Order(6)
public class SpecialEducationProgramsRule implements ProgramEligibilityBaseRule {
  private final ValidationRulesService validationRulesService;

  public SpecialEducationProgramsRule(ValidationRulesService validationRulesService) {
    this.validationRulesService = validationRulesService;
  }

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> errors) {
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - SpecialEducationProgramsRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    log.debug("In shouldExecute of ProgramEligibilityBaseRule - SpecialEducationProgramsRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
            hasNotViolatedBaseRules(errors),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    return hasNotViolatedBaseRules(errors);
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    log.debug("In executeValidation of ProgramEligibilityBaseRule - SpecialEducationProgramsRule for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

    List<String> activeSpecialEdPrograms = validationRulesService.getActiveSpecialEducationCategoryCodes().stream().map(e -> e.getSpecialEducationCategoryCode()).toList();

    Boolean isGraduated = student.getIsGraduated();
    Boolean isSchoolAged = student.getIsSchoolAged();
    Boolean isAdult = student.getIsAdult();
    boolean isNonGraduatedAdult = Boolean.FALSE.equals(isGraduated) && Boolean.TRUE.equals(isAdult);

    if (StringUtils.isEmpty(student.getSpecialEducationCategoryCode()) || !activeSpecialEdPrograms.contains(student.getSpecialEducationCategoryCode())) {
      log.debug("ProgramEligibilityBaseRule - SpecialEducationProgramsRule: Sped code value - {} for sdcSchoolCollectionStudentID :: {}", student.getSpecialEducationCategoryCode(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
      errors.add(ProgramEligibilityIssueCode.NOT_ENROLLED_SPECIAL_ED);
    }else if (Boolean.FALSE.equals(isSchoolAged) && !isNonGraduatedAdult) {
      log.debug("ProgramEligibilityBaseRule - SpecialEducationProgramsRule: Is school aged - {}, Is non graduated adult - {}, for sdcSchoolCollectionStudentID :: {}", student.getIsSchoolAged(), student.getIsGraduated(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
      errors.add(ProgramEligibilityIssueCode.NON_ELIG_SPECIAL_EDUCATION);
    }

    return errors;
  }

}
