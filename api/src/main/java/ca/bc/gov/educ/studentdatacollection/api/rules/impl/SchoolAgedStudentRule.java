package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
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
public class SchoolAgedStudentRule implements BaseRule {
    private static final DecimalFormat df = new DecimalFormat("00.00");

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent()
                && DOBUtil.isValidDate(sdcStudentSagaData.getSdcSchoolCollectionStudent().getDob());
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = sdcStudentSagaData.getSdcSchoolCollectionStudent();

        if (conditionPassed(sdcStudentSagaData) && StringUtils.isNotEmpty(student.getNumberOfCourses()) && Double.parseDouble(df.format(Double.valueOf(student.getNumberOfCourses()))) == 0) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.NUMBER_OF_COURSES, SdcSchoolCollectionStudentValidationIssueTypeCode.SCHOOLAGE_ZERO_COURSES));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_GRADE_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.SCHOOLAGE_ZERO_COURSES));
        }
        return errors;
    }

    private boolean conditionPassed(SdcStudentSagaData sdcStudentSagaData) {
        var student = sdcStudentSagaData.getSdcSchoolCollectionStudent();
        return DOBUtil.isSchoolAged(student.getDob()) && EightPlusGradeCodes.findByValue(student.getEnrolledGradeCode()).isPresent() &&
                (!sdcStudentSagaData.getSchool().getFacilityTypeCode().equalsIgnoreCase(Constants.DISTRICT_ONLINE) || !sdcStudentSagaData.getSchool().getFacilityTypeCode().equalsIgnoreCase(Constants.PROV_ONLINE));
    }

}
