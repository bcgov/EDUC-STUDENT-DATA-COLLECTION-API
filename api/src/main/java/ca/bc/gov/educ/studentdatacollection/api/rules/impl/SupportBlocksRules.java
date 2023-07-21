package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SupportBlockGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SupportBlocksRules implements BaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes
            .findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode())
            .isPresent()
            && !sdcStudentSagaData.getCollectionTypeCode().equals(Constants.JULY);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final SdcSchoolCollectionStudent student = sdcStudentSagaData.getSdcSchoolCollectionStudent();
        final String supportBlocks = student.getSupportBlocks();
        final String courseCountStr = student.getNumberOfCourses();
        final Double courseCount = TransformUtil
            .parseNumberOfCourses(courseCountStr, student.getSdcSchoolCollectionID());
        final String enrolledGradeCode = student.getEnrolledGradeCode();
        Optional<SupportBlockGradeCodes> supportBlockGradeCodeOptional = SupportBlockGradeCodes
            .findByValue(enrolledGradeCode);

        if (StringUtils.isNotEmpty(enrolledGradeCode)
        && supportBlockGradeCodeOptional.isPresent()
        && StringUtils.isNotEmpty(supportBlocks)
        && StringUtils.isNotEmpty(courseCountStr)
        && courseCount >= 8) {
            errors.add(createValidationIssue(
                SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING,
                SdcSchoolCollectionStudentValidationFieldCode.SUPPORT_BLOCKS,
                SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_BLOCKS_NOT_COUNT
            ));
            errors.add(createValidationIssue(
                SdcSchoolCollectionStudentValidationIssueSeverityCode.WARNING,
                SdcSchoolCollectionStudentValidationFieldCode.NUMBER_OF_COURSES,
                SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_BLOCKS_NOT_COUNT
            ));
        }

        if (StringUtils.isNotEmpty(supportBlocks) && Integer.parseInt(supportBlocks) > 8) {
            errors.add(createValidationIssue(
                SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR,
                SdcSchoolCollectionStudentValidationFieldCode.SUPPORT_BLOCKS,
                SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_BLOCKS_INVALID
            ));
        }

        final String studentDOB = student.getDob();
        if (DOBUtil.isValidDate(studentDOB)
        && DOBUtil.isSchoolAged(studentDOB)
        && StringUtils.isNotEmpty(enrolledGradeCode)
        && enrolledGradeCode.equals("GA")) {
            errors.add(createValidationIssue(
                SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR,
                SdcSchoolCollectionStudentValidationFieldCode.DOB,
                SdcSchoolCollectionStudentValidationIssueTypeCode.GA_ERROR
            ));
            errors.add(createValidationIssue(
                SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR,
                SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_GRADE_CODE,
                SdcSchoolCollectionStudentValidationIssueTypeCode.GA_ERROR
            ));
        }
        return errors;
    }


}
