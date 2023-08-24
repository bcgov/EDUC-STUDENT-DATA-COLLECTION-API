package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
public class NoHomeschoolStudentsRule implements ProgramEligibilityBaseRule {

  @Override
  public boolean shouldExecute(SdcStudentSagaData saga, List<ProgramEligibilityIssueCode> list) {
    return saga.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode());
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData saga) {
    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    errors.add(ProgramEligibilityIssueCode.HOMESCHOOL);
    return errors;
  }

}
