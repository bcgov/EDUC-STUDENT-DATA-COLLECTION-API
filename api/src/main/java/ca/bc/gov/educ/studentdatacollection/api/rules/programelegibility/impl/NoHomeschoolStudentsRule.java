package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

@Component
@Order(1)
public class NoHomeschoolStudentsRule implements ProgramEligibilityBaseRule {

  @Override
  public boolean shouldExecute(SdcStudentSagaData saga, List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> list) {
    return saga.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode());
  }

  @Override
  public List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData saga) {
    List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors = new ArrayList<>();
    errors.add(SdcSchoolCollectionStudentProgramEligibilityIssueCode.HOMESCHOOL);
    return errors;
  }

}
