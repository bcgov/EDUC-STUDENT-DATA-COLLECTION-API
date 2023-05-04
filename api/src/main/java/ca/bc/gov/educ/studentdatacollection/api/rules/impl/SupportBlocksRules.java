package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SupportBlockGradeCodes;
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
public class SupportBlocksRules implements BaseRule {
    private static final DecimalFormat df = new DecimalFormat("00.00");

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                !sdcStudentSagaData.getCollectionTypeCode().equals(Constants.JULY);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();


        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode())
                && SupportBlockGradeCodes.findByValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode()).isPresent()
                && StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSupportBlocks())
                && StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getNumberOfCourses())
                && Double.parseDouble(df.format(Double.valueOf(sdcStudentSagaData.getSdcSchoolCollectionStudent().getNumberOfCourses()))) >= 8) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.SUPPORT_BLOCKS, SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_BLOCKS_NOT_COUNT));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING, SdcSchoolCollectionStudentValidationFieldCode.NUMBER_OF_COURSES, SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_BLOCKS_NOT_COUNT));
        }

        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSupportBlocks()) &&
                Integer.parseInt(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSupportBlocks()) > 8) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.SUPPORT_BLOCKS, SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_BLOCKS_INVALID));
        }

        if((sdcStudentSagaData.getSchool().getFacilityTypeCode().equals(Constants.PROV_ONLINE) || sdcStudentSagaData.getSchool().getFacilityTypeCode().equals(Constants.DISTRICT_ONLINE)) &&
                (StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSupportBlocks()) && !sdcStudentSagaData.getSdcSchoolCollectionStudent().getSupportBlocks().equals("0"))) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.SUPPORT_BLOCKS, SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_FACILITY_NA));
        }

        if(DOBUtil.isValidDate(sdcStudentSagaData.getSdcSchoolCollectionStudent().getDob())
                && DOBUtil.isSchoolAged(sdcStudentSagaData.getSdcSchoolCollectionStudent().getDob())
        && StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode())
                && sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals("GA")) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.DOB, SdcSchoolCollectionStudentValidationIssueTypeCode.GA_ERROR));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_GRADE_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.GA_ERROR));
        }
        return errors;
    }


}
