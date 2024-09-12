package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Order(4)
public class InactiveOnlineAdultStudentsRule implements ProgramEligibilityBaseRule {
  private final FteCalculatorUtils fteCalculatorUtils;

  public InactiveOnlineAdultStudentsRule(
    FteCalculatorUtils fteCalculatorUtils
  ) {
    this.fteCalculatorUtils = fteCalculatorUtils;
  }

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> list) {
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - InactiveOnlineAdultStudentsRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    boolean noCoursesInLastTwoYears = fteCalculatorUtils.noCoursesForAdultStudentInLastTwoYears(studentRuleData);
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - InactiveOnlineAdultStudentsRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}", noCoursesInLastTwoYears, studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    return noCoursesInLastTwoYears;
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    log.debug("In executeValidation of ProgramEligibilityBaseRule - InactiveOnlineAdultStudentsRule for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    errors.add(ProgramEligibilityIssueCode.INACTIVE_ADULT);
    return errors;
  }

}
