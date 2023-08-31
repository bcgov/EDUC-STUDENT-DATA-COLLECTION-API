package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportingRequirementCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V19 | ERROR    | If the student is reported by a CSF school, one of the program     |  V30      |
 *                     codes reported for the student must be Programme francophone.
 *
 *  | V20 | ERROR    | If one of the program codes reported for a students is Programme   |  V30      |
 *                     francophone, the student must be reported by a CSF school.
 *
 */
@Component
@Order(590)
public class CSFProgramRule implements ValidationBaseRule {

    private final ValidationRulesService validationRulesService;

    public CSFProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent()
                 && StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes())
                 && isValidationDependencyResolved("V19", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = validationRulesService.splitString(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes());

        if (enrolledProgramCodes.contains(EnrolledProgramCodes.PROGRAMME_FRANCOPHONE.getCode()) && !studentRuleData.getSchool().getSchoolReportingRequirementCode().equals(SchoolReportingRequirementCodes.CSF.getCode())) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.ENROLLED_WRONG_REPORTING));
        }
        if (studentRuleData.getSchool().getSchoolCategoryCode().equals(SchoolCategoryCodes.PUBLIC.getCode()) && studentRuleData.getSchool().getSchoolReportingRequirementCode().equals(SchoolReportingRequirementCodes.CSF.getCode()) && !enrolledProgramCodes.contains(EnrolledProgramCodes.PROGRAMME_FRANCOPHONE.getCode())) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.ENROLLED_NO_FRANCOPHONE));
        }

        return errors;
    }
}
