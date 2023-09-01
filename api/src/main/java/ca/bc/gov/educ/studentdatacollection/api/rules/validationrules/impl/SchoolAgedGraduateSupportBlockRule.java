package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.PenMatchAndGradStatusService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V69 | WARNING  | Graduated school-aged students in grade 10, 11, 12, or SU,            | V04,V28,V67  |
 *                     should not be reported with support blocks.
 */
@Component
@Order(740)
public class SchoolAgedGraduateSupportBlockRule implements ValidationBaseRule {

    private final PenMatchAndGradStatusService penMatchAndGradStatusService;

    public SchoolAgedGraduateSupportBlockRule(PenMatchAndGradStatusService penMatchAndGradStatusService) {
        this.penMatchAndGradStatusService = penMatchAndGradStatusService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent()
                && !studentRuleData.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V69", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

        if(student.getIsGraduated() == null){
            penMatchAndGradStatusService.updatePenMatchAndGradStatusColumns(student, studentRuleData.getSchool().getMincode());
        }

        if (student.getIsSchoolAged() && student.getIsGraduated() && StringUtils.isNotEmpty(student.getSupportBlocks())) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.SUPPORT_BLOCKS, StudentValidationIssueTypeCode.SCHOOL_AGED_GRADUATE_SUPPORT_BLOCKS));
        }
        return errors;
    }

}
