package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CareerPrograms;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.IndigenousPrograms;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.CareerProgramCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FundingCode14IndigenousProgramRule implements BaseRule {

    private final ValidationRulesService validationRulesService;

    public FundingCode14IndigenousProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode()) &&
                StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes()) &&
                sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes().length() % 2 == 0 &&
                StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSchoolFundingCode()) &&
                !sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals(Constants.HS) && sdcStudentSagaData.getSchool().getSchoolCategoryCode().equals(Constants.PUBLIC)
                && sdcStudentSagaData.getSdcSchoolCollectionStudent().getSchoolFundingCode().equals(Constants.FUNDING_CODE_14);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = validationRulesService.splitString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());

        if (IndigenousPrograms.getCodes().stream().anyMatch(enrolledProgramCodes::contains)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.ENROLLED_CODE_IND_ERR));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.SCHOOL_FUNDING_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.ENROLLED_CODE_IND_ERR));
        }

        return errors;
    }

}
