package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolFundingCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes.NOMINAL_ROLL_ELIGIBLE;

@Component
@Slf4j
@Order(50)
public class IndependentSchoolAndBandCodeCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        log.debug("IndependentSchoolAndBandCodeCalculator: Starting calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        var isIndependentSchool = studentData.getSchool() != null && SchoolCategoryCodes.INDEPENDENTS.contains(studentData.getSchool().getSchoolCategoryCode());
        var hasBandCode = StringUtils.isNotBlank(studentData.getSdcSchoolCollectionStudentEntity().getBandCode());
        var fundingCode = studentData.getSdcSchoolCollectionStudentEntity().getSchoolFundingCode();

        if(isIndependentSchool && (StringUtils.equals(fundingCode, SchoolFundingCodes.STATUS_FIRST_NATION.getCode()) || (hasBandCode && StringUtils.isBlank(fundingCode)))) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(BigDecimal.ZERO);
            fteCalculationResult.setFteZeroReason(NOMINAL_ROLL_ELIGIBLE.getCode());
            log.debug("IndependentSchoolAndBandCodeCalculator: Fte result {} calculated with zero reason '{}' for student :: {}", fteCalculationResult.getFte(), fteCalculationResult.getFteZeroReason(), studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return fteCalculationResult;
        } else {
            log.debug("IndependentSchoolAndBandCodeCalculator: No FTE result, moving to next calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
