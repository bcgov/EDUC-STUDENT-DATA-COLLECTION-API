package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Order(7)
public class ZeroCoursesSchoolAgeRule implements ProgramEligibilityBaseRule {

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> errors) {
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - ZeroCoursesSchoolAgeRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    log.debug("In shouldExecute of ProgramEligibilityBaseRule - ZeroCoursesSchoolAgeRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
            hasNotViolatedBaseRules(errors),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    return hasNotViolatedBaseRules(errors);
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
    log.debug("In executeValidation of ProgramEligibilityBaseRule - ZeroCoursesSchoolAgeRule for sdcSchoolCollectionStudentID :: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    boolean isSchoolAged = student.getIsSchoolAged();
    boolean hasZeroCourses = Integer.parseInt(student.getNumberOfCourses()) <= 0;
    boolean notAttendingOL = !FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(studentRuleData.getSchool().getFacilityTypeCode());
    boolean inGrades8PlusMinusGA = SchoolGradeCodes.get8PlusGradesNoGA().contains(student.getEnrolledGradeCode());

    if(isSchoolAged && hasZeroCourses && notAttendingOL && inGrades8PlusMinusGA){
      errors.add(ProgramEligibilityIssueCode.ZERO_COURSES_SCHOOL_AGE);
    }

    return errors;
  }

}
