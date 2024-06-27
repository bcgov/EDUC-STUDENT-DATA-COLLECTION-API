package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolFundingCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V90 | WARNING  | School-aged students reported with a funding code of 16 must not be   |     V26      |
 *                     reported in any previous collection to receive funding in Feb.
 *
 *                     They also must be reported in a school with a facility type of Public:
 *                     Standard, Alt-Progs, Youth, Short-PRP, Long-PRP to receive funding in
 *                     Feb.
 */
@Component
@Slf4j
@Order(900)
public class RefugeeFundingRule implements ValidationBaseRule {

    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

    public RefugeeFundingRule(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
        this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of RefugeeFundingRule-V90: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        var shouldExecute = FteCalculatorUtils.getCollectionTypeCode(studentRuleData).equals(CollectionTypeCodes.FEBRUARY.getTypeCode()) &&
                StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getSchoolFundingCode()) &&
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSchoolFundingCode().equals(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode()) &&
                Boolean.TRUE.equals(studentRuleData.getSdcSchoolCollectionStudentEntity().getIsSchoolAged());
                isValidationDependencyResolved("V90", validationErrorsMap);

        log.debug("In shouldExecute of RefugeeFundingRule-V90: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                shouldExecute,
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return shouldExecute;
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of RefugeeFundingRule-V90 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        List<String> eligibleFacilityTypeCodes = Arrays.asList(
            FacilityTypeCodes.STANDARD.getCode(),
            FacilityTypeCodes.ALT_PROGS.getCode(),
            FacilityTypeCodes.YOUTH.getCode(),
            FacilityTypeCodes.SHORT_PRP.getCode(),
            FacilityTypeCodes.LONG_PRP.getCode()
        );

        Boolean notEligibleCategoryCode = !SchoolCategoryCodes.PUBLIC.getCode().equals(studentRuleData.getSchool().getSchoolCategoryCode());
        Boolean notEligibleFacilityTypeCode = !eligibleFacilityTypeCodes.contains(studentRuleData.getSchool().getFacilityTypeCode());

        if (Boolean.TRUE.equals(notEligibleCategoryCode) || Boolean.TRUE.equals(notEligibleFacilityTypeCode) || Boolean.TRUE.equals(studentInPreviousCollection(studentRuleData))) {
            log.debug("RefugeeFundingRule-V90: Refugee not reported in September Collection for sdcSchoolCollectionStudentID:: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.SCHOOL_FUNDING_CODE, StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL));
        }

        return errors;
    }


    private Boolean studentInPreviousCollection(StudentRuleData studentRuleData){
        UUID assignedStudentId = studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId();
        if (assignedStudentId == null) {
            return false;
        }

        var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
        var allPreviousCollections  = sdcSchoolCollectionRepository.findAllCollectionsBeforeCurrentCollection(currentSnapshotDate);
        var previousCollectionCount = sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(assignedStudentId, allPreviousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList());

        return previousCollectionCount > 0;
    }
}
