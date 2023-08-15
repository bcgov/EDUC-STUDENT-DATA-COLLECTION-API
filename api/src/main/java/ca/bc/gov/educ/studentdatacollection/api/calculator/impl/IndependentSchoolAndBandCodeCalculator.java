package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes.NOMINAL_ROLL_ELIGIBLE;

@Component
@Slf4j
@Order(5)
public class IndependentSchoolAndBandCodeCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(SdcStudentSagaData studentData) {
        var isIndependentSchool = studentData.getSchool() != null && StringUtils.equals(studentData.getSchool().getSchoolCategoryCode(), SchoolCategoryCodes.INDEPEND.getCode());
        var hasBandCode = StringUtils.isNotBlank(studentData.getSdcSchoolCollectionStudent().getBandCode());
        var fundingCode = studentData.getSdcSchoolCollectionStudent().getSchoolFundingCode();

        if(isIndependentSchool && (StringUtils.equals(fundingCode, Constants.IND_FUNDING_CODE) || (hasBandCode && StringUtils.isBlank(fundingCode)))) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(BigDecimal.ZERO);
            fteCalculationResult.setFteZeroReason(NOMINAL_ROLL_ELIGIBLE.getCode());
            return fteCalculationResult;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
