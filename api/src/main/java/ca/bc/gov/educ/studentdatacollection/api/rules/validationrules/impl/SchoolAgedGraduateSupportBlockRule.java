package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V69 | WARNING  | Graduated school-aged students in grade 10, 11, 12, or SU,            | V28,V04,V05  |
 *                     should not be reported with support blocks.                             V06,V07,V08
 *                                                                                             V09,V10,V11
 *                                                                                             V12, V67
 */
@Component
@Slf4j
@Order(740)
public class SchoolAgedGraduateSupportBlockRule extends BaseAdultSchoolAgeRule implements ValidationBaseRule {

    public SchoolAgedGraduateSupportBlockRule(ValidationRulesService validationRulesService) {
        super(validationRulesService);
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of SchoolAgedGraduateSupportBlockRule-V69: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        log.debug("In shouldExecute of SchoolAgedGraduateSupportBlockRule-V69: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                SchoolGradeCodes.get8PlusGradesNoGA().contains(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode())
                        && !studentRuleData.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                        && isValidationDependencyResolved("V69", validationErrorsMap),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return SchoolGradeCodes.get8PlusGradesNoGA().contains(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode())
                && !studentRuleData.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V69", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of SchoolAgedGraduateSupportBlockRule-V69 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

        this.setupGraduateValues(studentRuleData);

        if (DOBUtil.isSchoolAged(student.getDob()) && Boolean.TRUE.equals(student.getIsGraduated()) && StringUtils.isNotEmpty(student.getSupportBlocks())) {
            log.debug("SchoolAgedGraduateSupportBlockRule-V69: Incorrect values for sdcSchoolCollectionStudentID::{}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.SUPPORT_BLOCKS, StudentValidationIssueTypeCode.SCHOOL_AGED_GRADUATE_SUPPORT_BLOCKS));
        }
        return errors;
    }

}
