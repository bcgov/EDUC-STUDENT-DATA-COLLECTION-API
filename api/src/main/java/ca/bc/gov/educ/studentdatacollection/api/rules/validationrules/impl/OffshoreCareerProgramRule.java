package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
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
 *  | V56 | WARNING    | Students in Independent, Independent First Nations, or Offshore school |  V30,V32      |
 *                     should not be reported with Career Programs or a Career Program Type.
 *
 */
@Component
@Order(600)
public class OffshoreCareerProgramRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;

    public OffshoreCareerProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent()
                && StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes())
                && (studentRuleData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.OFFSHORE.getCode())
                || studentRuleData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDEPEND.getCode())
                || studentRuleData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDP_FNS.getCode()))
                && isValidationDependencyResolved("V56", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = validationRulesService.splitString(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledProgramCodes());

        if(EnrolledProgramCodes.getCareerProgramCodes().stream().anyMatch(enrolledProgramCodes::contains)) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_PROGRAM_CODE, StudentValidationIssueTypeCode.CAREER_OFFSHORE_ERR));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.CAREER_PROGRAM_CODE, StudentValidationIssueTypeCode.CAREER_OFFSHORE_ERR));
        }
        return errors;
    }
}
