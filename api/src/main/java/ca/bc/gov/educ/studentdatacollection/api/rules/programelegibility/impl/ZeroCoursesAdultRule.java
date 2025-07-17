package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Order(7)
public class ZeroCoursesAdultRule implements ProgramEligibilityBaseRule {

  private static final DecimalFormat df = new DecimalFormat("00.00");
  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> errors) {
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - ZeroCoursesAdultRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    var hasNotViolatedBaseRules = hasNotViolatedBaseRules(errors);

    log.debug("In shouldExecute of ProgramEligibilityBaseRule - ZeroCoursesAdultRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
            hasNotViolatedBaseRules,
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    return hasNotViolatedBaseRules;
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
    log.debug("In executeValidation of ProgramEligibilityBaseRule - ZeroCoursesAdultRule for sdcSchoolCollectionStudentID :: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    boolean isAdult = DOBUtil.isAdult(studentRuleData.getSdcSchoolCollectionStudentEntity().getDob());
    boolean hasZeroCourses = StringUtils.isEmpty(student.getNumberOfCourses()) || Double.parseDouble(df.format(Double.valueOf(student.getNumberOfCourses()))) == 0;
    // todo
    // we want to check OL in september
    // for OL in february we want to skip the base rule - then we need a new non eligibility rule for all but sped in february for OL
    boolean notAttendingOL = !FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(studentRuleData.getSchool().getFacilityTypeCode());
    boolean inGrades8Plus = SchoolGradeCodes.get8PlusGrades().contains(student.getEnrolledGradeCode());

    if(isAdult && hasZeroCourses && notAttendingOL && inGrades8Plus){
      errors.add(ProgramEligibilityIssueCode.ZERO_COURSES_ADULT);
    }

    return errors;
  }

}
