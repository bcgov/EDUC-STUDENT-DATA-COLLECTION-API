package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
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
import java.util.List;
import java.util.UUID;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V90 | WARNING  | Students reported with a Funding Code of 16 must not be reported in   |     V26      |
 *                     September Collection to receive funding
 *
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
                isValidationDependencyResolved("V90", validationErrorsMap) &&
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSchoolFundingCode().equals(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());

        log.debug("In shouldExecute of RefugeeFundingRule-V90: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                shouldExecute,
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        return shouldExecute;
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of RefugeeFundingRule-V90 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();

        if (Boolean.FALSE.equals(studentInSeptemberCollection(studentRuleData))) {
            log.debug("RefugeeFundingRule-V90: Refugee not reported in September Collection for sdcSchoolCollectionStudentID:: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.FUNDING_WARNING, StudentValidationFieldCode.SCHOOL_FUNDING_CODE, StudentValidationIssueTypeCode.REFUGEE_IN_SEPT_COL));
        }

        return errors;
    }


    private Boolean studentInSeptemberCollection(StudentRuleData studentRuleData){
        UUID assignedStudentId = studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId();
        if (assignedStudentId == null) {
            return false;
        }

        UUID districtId = UUID.fromString(studentRuleData.getSchool().getDistrictId());
        var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
        var previousSeptemberCollections = sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(districtId, currentSnapshotDate.minusYears(1).withMonth(9).withDayOfMonth(1), currentSnapshotDate);

        return sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(assignedStudentId, previousSeptemberCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList()) > 0;
    }
}
