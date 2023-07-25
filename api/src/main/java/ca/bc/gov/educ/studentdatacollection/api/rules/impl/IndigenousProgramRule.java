package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IndigenousProgramRule implements BaseRule {
    private final ValidationRulesService validationRulesService;
    public IndigenousProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }
    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode())
                && StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes()) &&
                !sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals("HS") &&
                sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes().length() % 2 == 0 &&
                !validationRulesService.isEnrolledProgramCodeInvalid(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());
    }
    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = validationRulesService.splitString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());

        if (IndigenousPrograms.getCodes().stream().anyMatch(enrolledProgramCodes::contains) && !sdcStudentSagaData.getSdcSchoolCollectionStudent().getNativeAncestryInd().equals("Y")) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.PROGRAM_CODE_IND));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.NATIVE_ANCESTRY_IND, SdcSchoolCollectionStudentValidationIssueTypeCode.PROGRAM_CODE_IND));
        }
        return errors;
    }
}
