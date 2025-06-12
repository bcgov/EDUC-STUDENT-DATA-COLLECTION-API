package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Order(10)
public class SummerFacilityProgramRule implements ProgramEligibilityBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> list) {
        log.debug("In shouldExecute of ProgramEligibilityBaseRule - SummerFacilityProgramRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        log.debug("In shouldExecute of ProgramEligibilityBaseRule - SummerFacilityProgramRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
                studentRuleData.getSchool().getFacilityTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return studentRuleData.getSchool().getFacilityTypeCode().equals(FacilityTypeCodes.SUMMER.getCode())
                && studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getCollectionTypeCode().equals(CollectionTypeCodes.JULY.getTypeCode());
    }

    @Override
    public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of ProgramEligibilityBaseRule - SummerFacilityProgramRule for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
        errors.add(ProgramEligibilityIssueCode.SUMMER_SCHOOL_CAREER);
        errors.add(ProgramEligibilityIssueCode.SUMMER_SCHOOL_FRENCH);
        return errors;
    }

}
