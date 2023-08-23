package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V65 | WARNING  | For students in grade 10, 11, 12, or SU, reported with Support        | V67,V28,V29  |
 *                     Blocks and 8 courses (number of courses) or more, support blocks
 *                     will not be counted toward funding.
 */
@Component
@Order(530)
public class SupportBlocksRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes
            .findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode())
            .isPresent() && !sdcStudentSagaData.getCollectionTypeCode().equals(CollectionTypeCodes.JULY.getTypeCode())
             && isValidationDependencyResolved("V65", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final SdcSchoolCollectionStudent student = sdcStudentSagaData.getSdcSchoolCollectionStudent();
        final String supportBlocks = student.getSupportBlocks();
        final String courseCountStr = student.getNumberOfCourses();
        final Double courseCount = TransformUtil.parseNumberOfCourses(courseCountStr, student.getSdcSchoolCollectionID());
        final String enrolledGradeCode = student.getEnrolledGradeCode();

        if (StringUtils.isNotEmpty(enrolledGradeCode) && SchoolGradeCodes.getSupportBlockGrades().contains(enrolledGradeCode)
                && StringUtils.isNotEmpty(supportBlocks) && StringUtils.isNotEmpty(courseCountStr) && courseCount >= 8) {
            errors.add(createValidationIssue(
                SdcSchoolCollectionStudentValidationIssueSeverityCode.FUNDING_WARNING,
                SdcSchoolCollectionStudentValidationFieldCode.SUPPORT_BLOCKS,
                SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_BLOCKS_NOT_COUNT
            ));
            errors.add(createValidationIssue(
                SdcSchoolCollectionStudentValidationIssueSeverityCode.FUNDING_WARNING,
                SdcSchoolCollectionStudentValidationFieldCode.NUMBER_OF_COURSES,
                SdcSchoolCollectionStudentValidationIssueTypeCode.SUPPORT_BLOCKS_NOT_COUNT
            ));
        }
        return errors;
    }


}
