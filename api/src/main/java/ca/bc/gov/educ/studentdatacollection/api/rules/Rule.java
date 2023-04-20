package ca.bc.gov.educ.studentdatacollection.api.rules;

import java.util.List;

public interface Rule<U, T> {
  boolean shouldExecute(U u);

  List<T> executeValidation(U u);
}
