package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;

import java.util.Map;

@FunctionalInterface
public interface Rule {

  /**
   * This method will be implemented by each child class for specific rule.
   * @param sdcSchoolStudentEntity the object to be validated.
   * @return the List of Errors Map, the map
   */
  Map<String, String> validate(SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity);
}
