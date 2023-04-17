package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;

import java.util.List;

public interface Rule<U, T> {
  boolean shouldExecute(U u);

  List<T> executeValidation(U u);
}
