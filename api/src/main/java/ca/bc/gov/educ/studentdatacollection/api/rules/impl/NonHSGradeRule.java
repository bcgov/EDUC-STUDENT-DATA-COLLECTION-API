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

@Component
public class NonHSGradeRule implements BaseRule {
    private final ValidationRulesService validationRulesService;
    public NonHSGradeRule(ValidationRulesService validationRulesService) {
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

        List<CareerProgramCode> activeCareerPrograms = validationRulesService.getActiveCareerProgramCodes();

        if(sdcStudentSagaData.getSchool().getSchoolCategoryCode().equals(Constants.PUBLIC)
                && (activeCareerPrograms.stream().noneMatch(program -> program.getCareerProgramCode().equals(sdcStudentSagaData.getSdcSchoolCollectionStudent().getCareerProgramCode()))
                || CareerPrograms.getCodes().stream().noneMatch(enrolledProgramCodes::contains))) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.CAREER_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.CAREER_CODE_PROG_ERR));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.CAREER_CODE_PROG_ERR));
        }

        return errors;
    }
}
