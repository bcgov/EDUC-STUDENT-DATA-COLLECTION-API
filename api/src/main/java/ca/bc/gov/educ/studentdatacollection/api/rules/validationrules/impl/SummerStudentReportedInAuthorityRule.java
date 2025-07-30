package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * | ID   | Severity | Rule                                                          | Dependent On |
 * |----- |----------|-------------------------------------------------------------- |--------------|
 * | V103 | ERROR    | Student included in any collection in this school year        | V04, V92          |
 *                     for the authority with FTE > 0 in any school with type
 *                     different from online
 *                     OR if the student reported in Online school in the authority
 *                     in grade K to 9 with FTE >0
 */
@Component
@Slf4j
@Order(1030)
public class SummerStudentReportedInAuthorityRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;
    private final RestUtils restUtils;

    public SummerStudentReportedInAuthorityRule(ValidationRulesService validationRulesService, RestUtils restUtils) {
        this.validationRulesService = validationRulesService;
        this.restUtils = restUtils;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of SummerStudentReportedInAuthorityRule-V93: for collectionType {} and sdcSchoolCollectionStudentID :: {}", FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return !studentRuleData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.SUMMER.getCode()) &&
                FteCalculatorUtils.getCollectionTypeCode(studentRuleData).equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode()) &&
                isValidationDependencyResolved("V103", validationErrorsMap);
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of SummerStudentReportedInAuthorityRule-V93 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        validationRulesService.setupPENMatchAndEllAndGraduateValues(studentRuleData);
        if (studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() != null && studentRuleData.getSchool().getIndependentAuthorityId() != null) {
            var historicalStudentCollection = validationRulesService.getStudentInHistoricalCollectionWithInSameAuthority(studentRuleData, "3");
            for (SdcSchoolCollectionStudentEntity studentEntity : historicalStudentCollection) {
                Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(studentEntity.getSdcSchoolCollection().getSchoolID().toString());
                if (school.isPresent()) {
                    boolean isOnlineSchool = FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(school.get().getFacilityTypeCode());
                    if (!isOnlineSchool || SchoolGradeCodes.getKToNineGrades().contains(studentEntity.getEnrolledGradeCode())) {
                        errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_GRADE_CODE, StudentValidationIssueTypeCode.SUMMER_STUDENT_ALREADY_REPORTED_AUTHORITY_ERROR));
                        break;
                    }
                }
            }
        }
        return errors;
    }

}

