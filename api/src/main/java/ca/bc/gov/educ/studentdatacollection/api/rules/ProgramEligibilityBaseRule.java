package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;

import java.util.List;
import java.util.Optional;

public interface ProgramEligibilityBaseRule extends Rule<StudentRuleData, ProgramEligibilityIssueCode> {
  default boolean hasNotViolatedBaseRules(List<ProgramEligibilityIssueCode> errors) {
    Optional<ProgramEligibilityIssueCode> baseReason = SdcSchoolCollectionStudentService.getBaseProgramEligibilityFailure(errors);
    return baseReason.isEmpty();
  }
}
