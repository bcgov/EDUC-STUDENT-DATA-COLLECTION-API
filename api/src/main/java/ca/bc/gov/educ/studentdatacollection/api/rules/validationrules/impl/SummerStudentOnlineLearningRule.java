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
import java.util.UUID;

/**
 * | ID   | Severity | Rule                                                          | Dependent On |
 * |------|----------|-------------------------------------------------------------- |--------------|
 * | V102 | ERROR    | The student is not reported in the Online School in July and  | V92          |
 *                     was not reported in the online school in any previous
 *                     collections this school year
 */
@Component
@Slf4j
@Order(924)
public class SummerStudentOnlineLearningRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;
    private final RestUtils restUtils;

    public SummerStudentOnlineLearningRule(ValidationRulesService validationRulesService, RestUtils restUtils) {
        this.validationRulesService = validationRulesService;
        this.restUtils = restUtils;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("strictRule-V102: for collectionType {} and sdcSchoolCollectionStudentID :: {}", FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return !studentRuleData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.SUMMER.getCode()) &&
                FteCalculatorUtils.getCollectionTypeCode(studentRuleData).equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode()) &&
                isValidationDependencyResolved("V102", validationErrorsMap);

    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of SummerStudentOnlineLearningRule-V102 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        boolean isOnlineEnrolled = false;
        if (studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() != null && !isOnlineSchool(studentRuleData.getSchool().getFacilityTypeCode())) {
            var historicalStudentCollection = validationRulesService.getStudentInHistoricalCollectionInAllDistrict(studentRuleData);
            for (SdcSchoolCollectionStudentEntity studentEntity : historicalStudentCollection) {
                Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(studentEntity.getSdcSchoolCollection().getSchoolID().toString());
                if (school.isPresent() && isOnlineSchool(school.get().getFacilityTypeCode())) {
                    isOnlineEnrolled = true;
                    break;
                }
            }
            if (!isOnlineEnrolled)
                errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.ENROLLED_GRADE_CODE, StudentValidationIssueTypeCode.SUMMER_STUDENT_ONLINE_LEARNING_ERROR));
        }
        return errors;
    }

    private boolean isOnlineSchool(String facilityTypeCode) {
        return FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(facilityTypeCode);
    }

}
