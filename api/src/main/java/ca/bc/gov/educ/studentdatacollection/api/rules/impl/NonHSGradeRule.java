package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.CareerProgramCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EnrolledProgramCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Component
public class NonHSGradeRule implements BaseRule {
    private final ValidationRulesService validationRulesService;
    public NonHSGradeRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }
    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode()) &&
                !sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals("HS") &&
                sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes().length() % 2 == 0;
    }
    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = splitString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());

        List<EnrolledProgramCode> activeEnrolledPrograms = validationRulesService.getActiveEnrolledProgramCodes();
        if(activeEnrolledPrograms.stream().noneMatch(programs -> enrolledProgramCodes.contains(programs.getEnrolledProgramCode()))) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.ENROLLED_CODE_INVALID));
        }

        if (FrenchPrograms.getFrenchProgramCodes().stream().filter(enrolledProgramCodes::contains).count() > 1) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.ENROLLED_CODE_COUNT_ERR));
        }

        if (sdcStudentSagaData.getSchool().getSchoolCategoryCode().equals(Constants.PUBLIC) && CareerPrograms.getCodes().stream().filter(enrolledProgramCodes::contains).count() > 1) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.CAREER_CODE_COUNT_ERR));
        }

        List<CareerProgramCode> activeCareerPrograms = validationRulesService.getActiveCareerProgramCodes();
        if(sdcStudentSagaData.getSchool().getSchoolCategoryCode().equals(Constants.PUBLIC) && activeCareerPrograms.stream().noneMatch(program -> program.getCareerProgramCode().equals(sdcStudentSagaData.getSdcSchoolCollectionStudent().getCareerProgramCode()))) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.CAREER_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.CAREER_CODE_INVALID));
        }

        if (enrolledProgramCodes.contains(Constants.LATE_FRENCH_IMMERSION_CODE) && !sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals(Constants.GRADE_06) && !sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals(Constants.GRADE_07)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.ENROLLED_CODE_FRANCOPHONE_ERR));
        }

        if (IndigenousPrograms.getCodes().stream().anyMatch(enrolledProgramCodes::contains) && !sdcStudentSagaData.getSdcSchoolCollectionStudent().getNativeAncestryInd().equals("Y")) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.PROGRAM_CODE_IND));
        }

        if(sdcStudentSagaData.getSchool().getSchoolCategoryCode().equals(Constants.PUBLIC)
                && (activeCareerPrograms.stream().noneMatch(program -> program.getCareerProgramCode().equals(sdcStudentSagaData.getSdcSchoolCollectionStudent().getCareerProgramCode()))
                || CareerPrograms.getCodes().stream().noneMatch(enrolledProgramCodes::contains))) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.CAREER_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.CAREER_CODE_PROG_ERR));
        }

        if(sdcStudentSagaData.getSchool().getSchoolCategoryCode().equals(Constants.PUBLIC) && CareerPrograms.getCodes().stream().anyMatch(enrolledProgramCodes::contains) && !EightPlusGradeCodes.getNonGraduateGrades().contains(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_GRADE_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.CAREER_CODE_GRADE_ERR));
        }

        return errors;
    }

    private List<String> splitString(String enrolledProgramCode) {
        return Pattern.compile(".{1,2}").matcher(enrolledProgramCode).results().map(MatchResult::group).toList();
    }
}
