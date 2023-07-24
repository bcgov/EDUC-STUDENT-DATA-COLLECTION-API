package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CareerPrograms;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OffshoreCareerProgramRule implements BaseRule {
    private final ValidationRulesService validationRulesService;

    public OffshoreCareerProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent()
                && StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes())
                && sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes().length() % 2 == 0
                && !validationRulesService.isEnrolledProgramCodeInvalid(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes())
                && (sdcStudentSagaData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(Constants.OFFSHORE)
                || sdcStudentSagaData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(Constants.INDEPEND));
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = validationRulesService.splitString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());

        if(CareerPrograms.getCodes().stream().anyMatch(enrolledProgramCodes::contains)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.CAREER_OFFSHORE_ERR));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.CAREER_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.CAREER_OFFSHORE_ERR));
        }
        return errors;
    }
}
