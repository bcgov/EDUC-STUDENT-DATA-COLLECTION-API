package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.OtherCoursesCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(170)
public class InvalidOtherCoursesRule implements BaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getOtherCourses()) && OtherCoursesCodes.findByValue(sdcStudentSagaData.getSdcSchoolCollectionStudent().getOtherCourses()).isEmpty()) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.OTHER_COURSES, SdcSchoolCollectionStudentValidationIssueTypeCode.OTHER_COURSE_INVALID));
        }
        return errors;
    }
}
