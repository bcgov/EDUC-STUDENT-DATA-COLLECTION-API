package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

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
@Order(1)
public class HomeschoolStudentsRule implements ProgramEligibilityBaseRule {

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> list) {
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - HomeschoolStudentsRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    log.debug("In shouldExecute of ProgramEligibilityBaseRule - HomeschoolStudentsRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
            studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode()),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    return studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode());
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    log.debug("In executeValidation of ProgramEligibilityBaseRule - HomeschoolStudentsRule for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    errors.add(ProgramEligibilityIssueCode.HOMESCHOOL);
    return errors;
  }

}
