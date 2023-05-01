package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CareerPrograms;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Component
public class OffshoreSchoolRules implements BaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes().length() % 2 == 0
                && (sdcStudentSagaData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(Constants.OFFSHORE)
                || sdcStudentSagaData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(Constants.INDEPEND));
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final List<String> enrolledProgramCodes = splitString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledProgramCodes());

        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSpecialEducationCategoryCode())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.SPECIAL_EDUCATION_CATEGORY_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.SPED_OFFSHORE_ERR));
        }

        if(CareerPrograms.getCodes().stream().anyMatch(enrolledProgramCodes::contains)) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_PROGRAM_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.CAREER_OFFSHORE_ERR));
        }
        return errors;
    }

    private List<String> splitString(String enrolledProgramCode) {
        return Pattern.compile(".{1,2}").matcher(enrolledProgramCode).results().map(MatchResult::group).toList();
    }
}
