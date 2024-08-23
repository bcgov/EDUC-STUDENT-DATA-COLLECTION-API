package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 *  | ID  | Severity | Rule                                                                    | Dependent On |
 *  |-----|----------|-------------------------------------------------------------------------|--------------|
 *  | V44 | WARNING    | Student's grade does not fall within the grade range for which the    |           |
 *                      school has been approved.
 */
@Component
@Slf4j
@Order(481)
public class SchoolFundingGroupGradeRangeRule implements ValidationBaseRule {

    private final ValidationRulesService validationRulesService;

    public SchoolFundingGroupGradeRangeRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of SchoolFundingGroupGradeRangeRule-V44: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        var shouldExecute = SchoolCategoryCodes.INDEPENDENTS.contains(studentRuleData.getSchool().getSchoolCategoryCode())
                && !FteCalculatorUtils.getCollectionTypeCode(studentRuleData).equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode());

        log.debug("In shouldExecute of SchoolFundingGroupGradeRangeRule-V44: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                shouldExecute,
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return shouldExecute;
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of SchoolFundingGroupGradeRangeRule-V44 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        final var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        final String studentGrade = student.getEnrolledGradeCode();
        var schoolFundingGroups = validationRulesService.getSchoolFundingGroups(studentRuleData.getSchool().getSchoolId());
        var gradesMapped = schoolFundingGroups.stream().map(IndependentSchoolFundingGroup::getSchoolGradeCode).toList();
        var schoolGrades = gradesMapped.stream().map(SchoolGradeCodes::findByTypeCode)
                .flatMap(grade -> grade.isPresent() ? Stream.of(grade.get().getCode()) : Stream.empty()).toList();

        if (!schoolGrades.contains(studentGrade)) {
            log.debug("SchoolFundingGroupGradeRangeRule-V44: School funding groups {} and grade code {} for sdcSchoolCollectionStudentID:: {}",gradesMapped, studentGrade, studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

            errors.add(createValidationIssue(
                StudentValidationIssueSeverityCode.INFO_WARNING,
                StudentValidationFieldCode.ENROLLED_GRADE_CODE,
                StudentValidationIssueTypeCode.INVALID_GRADE_SCHOOL_FUNDING_GROUP
            ));
        }

        return errors;
    }
}
