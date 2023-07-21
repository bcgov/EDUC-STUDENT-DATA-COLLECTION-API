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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class HomeSchoolCareerProgramRule implements BaseRule {

    private final ValidationRulesService validationRulesService;

    public HomeSchoolCareerProgramRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode()) &&
                sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals(Constants.HS) &&
                StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes()) &&
                sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes().length() % 2 == 0 &&
                !validationRulesService.isEnrolledProgramCodeInvalid(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = sdcStudentSagaData.getSdcSchoolCollectionStudent();
        List<CareerProgramCode> activeCareerCodes = validationRulesService.getActiveCareerProgramCodes();
        final List<String> enrolledProgramCodes = validationRulesService.splitString(student.getEnrolledProgramCodes());

        if (CareerPrograms.getCodes().stream().anyMatch(enrolledProgramCodes::contains) || (StringUtils.isNotEmpty(student.getCareerProgramCode()) && activeCareerCodes.stream().anyMatch(careerCode -> student.getCareerProgramCode().contains(careerCode.getCareerProgramCode())))) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.PROGRAM_CODE_HS_CAREER));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_GRADE_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.PROGRAM_CODE_HS_CAREER));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.CAREER_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.PROGRAM_CODE_HS_CAREER));
        }

        return errors;
    }
}
