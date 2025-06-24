package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@Order(11)
public class PRPorYouthSchoolRule implements ProgramEligibilityBaseRule {
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> list) {
        log.debug("In shouldExecute of ProgramEligibilityBaseRule - PRPorYouthSchoolRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        var prpAndYouthSchools = Arrays.asList(FacilityTypeCodes.SHORT_PRP.getCode(), FacilityTypeCodes.LONG_PRP.getCode(), FacilityTypeCodes.YOUTH.getCode());

        return prpAndYouthSchools.contains(studentRuleData.getSchool().getFacilityTypeCode());
    }

    @Override
    public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of ProgramEligibilityBaseRule - PRPorYouthSchoolRule for sdcSchoolCollectionStudentID :: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

        List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
        errors.add(ProgramEligibilityIssueCode.PRP_YOUTH);
        return errors;
    }
}
