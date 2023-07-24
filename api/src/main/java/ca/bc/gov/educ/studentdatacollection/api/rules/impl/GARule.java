package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GARule implements BaseRule {

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
        final String enrolledGradeCode = student.getEnrolledGradeCode();

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
