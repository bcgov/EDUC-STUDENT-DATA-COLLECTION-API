package ca.bc.gov.educ.studentdatacollection.api.rules.validationrulesimpl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V25 | WARNING  | Students registered in homeschool (enrolled grade of HS),             |  V60,V28     |
 *                     should not be reported with any special education designations.
 *
 */
@Component
@Order(670)
public class HomeSchoolSpecialEducRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent() &&
                isValidationDependencyResolved("V25", validationErrorsMap) &&
                sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode().equals(Constants.HS);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = sdcStudentSagaData.getSdcSchoolCollectionStudent();

        if (StringUtils.isNotEmpty(student.getSpecialEducationCategoryCode())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.FUNDING_WARNING, SdcSchoolCollectionStudentValidationFieldCode.ENROLLED_GRADE_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.PROGRAM_CODE_HS_SPED));
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.FUNDING_WARNING, SdcSchoolCollectionStudentValidationFieldCode.SPECIAL_EDUCATION_CATEGORY_CODE, SdcSchoolCollectionStudentValidationIssueTypeCode.PROGRAM_CODE_HS_SPED));
        }
        return errors;
    }
}
