package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.AllowedAdultGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EightPlusGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class AdultStudentRule implements BaseRule {

    private static final DecimalFormat df = new DecimalFormat("00.00");

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                !sdcStudentSagaData.getCollectionTypeCode().equalsIgnoreCase(Constants.JULY)
                && DOBUtil.isValidDate(sdcStudentSagaData.getSdcSchoolCollectionStudent().getDob())
                && DOBUtil.isAdult(sdcStudentSagaData.getSdcSchoolCollectionStudent().getDob());
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if (conditionPassed(sdcStudentSagaData) && StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getNumberOfCourses()) && Double.parseDouble(df.format(Double.valueOf(sdcStudentSagaData.getSdcSchoolCollectionStudent().getNumberOfCourses()))) == 0) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.NUMBER_OF_COURSES, SdcSchoolCollectionStudentValidationIssueTypeCode.ADULT_ZERO_COURSES));
        }

        if(AllowedAdultGradeCodes.findByValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode()).isEmpty()) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.DOB, SdcSchoolCollectionStudentValidationIssueTypeCode.ADULT_INCORRECT_GRADE));
        }

        if(AllowedAdultGradeCodes.getNonGraduateGrades().contains(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode())
                && StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSupportBlocks())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.SUPPORT_BLOCKS, SdcSchoolCollectionStudentValidationIssueTypeCode.ADULT_SUPPORT_ERR));
        }
        return errors;
    }

    private boolean conditionPassed(SdcStudentSagaData sdcStudentSagaData) {
        return AllowedAdultGradeCodes.findByValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode()).isEmpty() &&
                (!sdcStudentSagaData.getSchool().getFacilityTypeCode().equalsIgnoreCase(Constants.DISTRICT_ONLINE) || !sdcStudentSagaData.getSchool().getFacilityTypeCode().equalsIgnoreCase(Constants.PROV_ONLINE));
    }
}
