package ca.bc.gov.educ.studentdatacollection.api.rules.validationrulesimpl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
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
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent()
                 && StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes())
                 && isValidationDependencyResolved("V19", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = validationRulesService.splitString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());

        if (enrolledProgramCodes.contains(EnrolledProgramCodes.PROGRAMME_FRANCOPHONE.getCode()) && !sdcStudentSagaData.getSchool().getSchoolReportingRequirementCode().equals(SchoolReportingRequirementCodes.CSF.getCode())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.ENROLLED_WRONG_REPORTING));
        }
        if (sdcStudentSagaData.getSchool().getSchoolCategoryCode().equals(SchoolCategoryCodes.PUBLIC.getCode()) && sdcStudentSagaData.getSchool().getSchoolReportingRequirementCode().equals(SchoolReportingRequirementCodes.CSF.getCode()) && !enrolledProgramCodes.contains(EnrolledProgramCodes.PROGRAMME_FRANCOPHONE.getCode())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.ENROLLED_NO_FRANCOPHONE));
        }

        return errors;
    }
}
