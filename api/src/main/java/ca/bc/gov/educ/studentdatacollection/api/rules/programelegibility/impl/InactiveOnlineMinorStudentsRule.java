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
@Order(5)
//todo
//rename to follow convention 'schoolAge'
public class InactiveOnlineMinorStudentsRule implements ProgramEligibilityBaseRule {
  private final FteCalculatorUtils fteCalculatorUtils;

  public InactiveOnlineMinorStudentsRule(
    FteCalculatorUtils fteCalculatorUtils
  ) {
    this.fteCalculatorUtils = fteCalculatorUtils;
  }

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> list) {
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - InactiveOnlineMinorStudentsRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    boolean noCoursesInLastTwoYears = fteCalculatorUtils.noCoursesForSchoolAgedStudentInLastTwoYears(studentRuleData);
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - InactiveOnlineMinorStudentsRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}", noCoursesInLastTwoYears, studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    return noCoursesInLastTwoYears;
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    log.debug("In executeValidation of ProgramEligibilityBaseRule - InactiveOnlineMinorStudentsRule for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    errors.add(ProgramEligibilityIssueCode.INACTIVE_SCHOOL_AGE);
    return errors;
  }

}
