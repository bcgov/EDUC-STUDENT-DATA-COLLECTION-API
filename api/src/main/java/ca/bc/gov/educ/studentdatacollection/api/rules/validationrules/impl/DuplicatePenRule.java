package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V21 | ERROR    | A PEN can only appear once in a school.                               | V02          |
 */
@Component
@Slf4j
@Order(320)
public class DuplicatePenRule implements ValidationBaseRule {

    private final ValidationRulesService validationRulesService;

    public DuplicatePenRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of DuplicatePenRule-V21: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return isValidationDependencyResolved("V21", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        var studentEntity = studentRuleData.getSdcSchoolCollectionStudentEntity();
        log.debug("In executeValidation of DuplicatePenRule-V21 for sdcSchoolCollectionStudentID ::" + studentEntity.getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        if(StringUtils.isNotEmpty(studentEntity.getStudentPen())) {
            Long penCount = validationRulesService.getDuplicatePenCount(studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID(), studentEntity.getStudentPen());
            if ((penCount > 1) || (penCount == 1 && studentEntity.getSdcSchoolCollectionStudentID() == null)) {
                log.debug("DuplicatePenRule-V21: Duplicate PEN's found - count {} for PEN number, sdcSchoolCollectionStudentID  :: {}" , penCount, studentEntity.getSdcSchoolCollectionStudentID());
                errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.STUDENT_PEN, StudentValidationIssueTypeCode.STUDENT_PEN_DUPLICATE));
            }
        }
        return errors;
    }
}
