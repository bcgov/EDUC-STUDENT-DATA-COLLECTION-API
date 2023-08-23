package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V70 | ERROR    | School-aged students must not be reported in grade "GA".              | V04,V28      |
 */
@Component
@Order(440)
public class GARule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes
            .findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode())
            .isPresent() && isValidationDependencyResolved("V70", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final SdcSchoolCollectionStudent student = sdcStudentSagaData.getSdcSchoolCollectionStudent();
        final String enrolledGradeCode = student.getEnrolledGradeCode();

        final String studentDOB = student.getDob();
        if (DOBUtil.isSchoolAged(studentDOB) && enrolledGradeCode.equals("GA")) {
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
