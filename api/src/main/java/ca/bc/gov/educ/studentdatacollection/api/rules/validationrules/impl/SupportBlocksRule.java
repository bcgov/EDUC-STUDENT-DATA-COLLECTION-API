package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Order(530)
public class SupportBlocksRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of SupportBlocksRule-V65: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        log.debug("In shouldExecute of SupportBlocksRule-V65: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                !studentRuleData.getCollectionTypeCode().equals(CollectionTypeCodes.JULY.getTypeCode())
                        && isValidationDependencyResolved("V65", validationErrorsMap),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return !studentRuleData.getCollectionTypeCode().equals(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V65", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of SupportBlocksRule-V65 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        final String supportBlocks = student.getSupportBlocks();
        final String courseCountStr = student.getNumberOfCourses();
        final Double courseCount = TransformUtil.parseNumberOfCourses(courseCountStr, student.getSdcSchoolCollection().getSdcSchoolCollectionID());
        final String enrolledGradeCode = student.getEnrolledGradeCode();

        log.debug("SupportBlocksRule-V65: No of courses {} for sdcSchoolCollectionStudentID:: {}", courseCount,  studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        if (StringUtils.isNotEmpty(enrolledGradeCode) && SchoolGradeCodes.getSupportBlockGrades().contains(enrolledGradeCode)
                && StringUtils.isNotEmpty(supportBlocks) && StringUtils.isNotEmpty(courseCountStr) && courseCount >= 8) {
            log.debug("SupportBlocksRule-V65: sdcSchoolCollectionStudentID::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.SUPPORT_BLOCKS, StudentValidationIssueTypeCode.SUPPORT_BLOCKS_NOT_COUNT));
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.NUMBER_OF_COURSES, StudentValidationIssueTypeCode.SUPPORT_BLOCKS_NOT_COUNT));
        }
        return errors;
    }


}
