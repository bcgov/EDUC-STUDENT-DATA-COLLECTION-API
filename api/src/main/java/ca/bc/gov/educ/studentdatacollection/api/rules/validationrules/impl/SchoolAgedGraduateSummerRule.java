package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V53 | ERROR    | School-aged students, reported in the                                 | V28,V04,V05  |
 *                     summer collection, cannot be graduated.                                 V06,V07,V08
 *                                                                                             V09,V10,V11
 *                                                                                             V12
 */
@Component
@Slf4j
@Order(720)
public class SchoolAgedGraduateSummerRule extends BaseAdultSchoolAgeRule implements ValidationBaseRule {

    public SchoolAgedGraduateSummerRule(ValidationRulesService validationRulesService) {
        super(validationRulesService);
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of SchoolAgedGraduateSummerRule-V53: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        log.debug("In shouldExecute of SchoolAgedGraduateSummerRule-V53: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                studentRuleData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.PUBLIC.getCode())
                        && studentRuleData.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                        && isValidationDependencyResolved("V53", validationErrorsMap),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return studentRuleData.getSchool().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.PUBLIC.getCode())
                && studentRuleData.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V53", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of SchoolAgedGraduateSummerRule-V53 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();

        this.setupGraduateValues(studentRuleData);

        if (DOBUtil.isSchoolAged(student.getDob()) && Boolean.TRUE.equals(student.getIsGraduated())) {
            log.debug("SchoolAgedGraduateSummerRule-V53: Incorrect dob value and graduated flag for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.DOB, StudentValidationIssueTypeCode.SCHOOL_AGED_GRADUATE_SUMMER));
        }
        return errors;
    }

}
