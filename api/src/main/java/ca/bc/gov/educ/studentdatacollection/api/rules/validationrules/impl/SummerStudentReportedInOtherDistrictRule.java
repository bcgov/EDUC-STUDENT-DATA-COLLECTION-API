package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                          | Dependent On |
 *  |-----|----------|-------------------------------------------------------------- |--------------|
 *  | V99 | ERROR    | Student must NOT be reported in Grade 8 or 9                  |V92           |
 *                     with FTE>0 in any other districts in previous
 *                     collection this school year.
 */
@Component
@Slf4j
@Order(926)
public class SummerStudentReportedInOtherDistrictRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;

    public SummerStudentReportedInOtherDistrictRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of SummerStudentReportedInOtherDistrictRule-V99: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return !studentRuleData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.SUMMER.getCode()) &&
                FteCalculatorUtils.getCollectionTypeCode(studentRuleData).equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())
                && isValidationDependencyResolved("V99", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of SummerStudentReportedInOtherDistrictRule-V99 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        var isStudentReportedInSepColl = validationRulesService.findStudentInHistoricalCollectionInOtherDistricts(studentRuleData, CollectionTypeCodes.SEPTEMBER.getTypeCode());
        var isStudentReportedInFebColl = validationRulesService.findStudentInHistoricalCollectionInOtherDistricts(studentRuleData, CollectionTypeCodes.FEBRUARY.getTypeCode());
        var isStudentReportedInMayColl = validationRulesService.findStudentInHistoricalCollectionInOtherDistricts(studentRuleData, CollectionTypeCodes.MAY.getTypeCode());

        if (isStudentReportedInSepColl || isStudentReportedInFebColl || isStudentReportedInMayColl) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.ENROLLED_GRADE_CODE, StudentValidationIssueTypeCode.SUMMER_STUDENT_REPORTED_NOT_IN_DISTRICT_ERROR));
       }

        return errors;
    }

}
