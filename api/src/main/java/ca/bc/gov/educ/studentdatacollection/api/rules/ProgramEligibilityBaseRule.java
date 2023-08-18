package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

import java.util.List;
import java.util.Optional;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentProgramEligibilityIssueCode;

public interface ProgramEligibilityBaseRule
extends Rule<SdcStudentSagaData, SdcSchoolCollectionStudentProgramEligibilityIssueCode> {
  default boolean hasNotViolatedBaseRules(List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors) {
    Optional<SdcSchoolCollectionStudentProgramEligibilityIssueCode> baseReason =
      SdcSchoolCollectionStudentProgramEligibilityIssueCode.getBaseProgramEligibilityFailure(errors);
    return baseReason.isEmpty();
  }

  default boolean hasNotViolatedIndigenousRules(List<SdcSchoolCollectionStudentProgramEligibilityIssueCode> errors) {
    return errors.stream().noneMatch(e ->
      e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NOT_ENROLLED_INDIGENOUS)
      || e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.INDIGENOUS_ADULT)
      || e.equals(SdcSchoolCollectionStudentProgramEligibilityIssueCode.NO_INDIGENOUS_ANCESTRY)
    );
  }
}
