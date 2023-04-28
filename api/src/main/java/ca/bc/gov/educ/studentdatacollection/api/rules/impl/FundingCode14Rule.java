package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.CareerProgramCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Component
public class FundingCode14Rule implements BaseRule {

    private final ValidationRulesService validationRulesService;

    public FundingCode14Rule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode()) &&
                StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSchoolFundingCode()) &&
                !sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals(Constants.HS) && sdcStudentSagaData.getSchool().getSchoolCategoryCode().equals(Constants.PUBLIC)
                && sdcStudentSagaData.getSdcSchoolCollectionStudent().getSchoolFundingCode().equals(Constants.FUNDING_CODE_14);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = splitString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());
        List<CareerProgramCode> activeCareerPrograms = validationRulesService.getActiveCareerProgramCodes();

        if (FrenchPrograms.getCodes().stream().anyMatch(enrolledProgramCodes::contains)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.ENROLLED_CODE_FUNDING_ERR));
        }

        if (IndigenousPrograms.getCodes().stream().anyMatch(enrolledProgramCodes::contains)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.ENROLLED_CODE_IND_ERR));
        }

        if (CareerPrograms.getCodes().stream().anyMatch(enrolledProgramCodes::contains) || activeCareerPrograms.stream().anyMatch(programs -> enrolledProgramCodes.contains(programs.getCareerProgramCode()))) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.ENROLLED_CODE_CAREER_ERR));
        }

        return errors;
    }

    private List<String> splitString(String enrolledProgramCode) {
        return Pattern.compile(".{1,2}").matcher(enrolledProgramCode).results().map(MatchResult::group).toList();
    }
}
