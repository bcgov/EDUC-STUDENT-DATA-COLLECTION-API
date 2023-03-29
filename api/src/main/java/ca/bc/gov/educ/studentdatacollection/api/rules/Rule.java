package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;

import java.util.List;

@FunctionalInterface
public interface Rule {

  List<SdcSchoolCollectionStudentValidationIssue> validate(SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity);
}
