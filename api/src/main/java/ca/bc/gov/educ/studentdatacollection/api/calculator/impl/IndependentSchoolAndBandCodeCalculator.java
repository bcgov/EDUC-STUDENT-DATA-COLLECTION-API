package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.BandCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.BandCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class IndependentSchoolAndBandCodeCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Getter
    private int processingSequenceNumber = 5;
    BandCodeRepository bandCodeRepository;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        var isIndependentSchool = studentData.getSchool() != null && StringUtils.equals(studentData.getSchool().getSchoolCategoryCode(), SchoolCategoryCodes.INDEPEND.getCode());
        Optional<BandCodeEntity> bandCode = StringUtils.isBlank(studentData.getSdcSchoolCollectionStudent().getBandCode()) ? (Optional.empty()): bandCodeRepository.findById(studentData.getSdcSchoolCollectionStudent().getBandCode());
        var hasValidBandCode = false;
        if (bandCode.isPresent()) {
            hasValidBandCode = bandCode.get().getEffectiveDate().isBefore(LocalDateTime.now()) && bandCode.get().getExpiryDate().isAfter(LocalDateTime.now());
        }
        var fundingCode = studentData.getSdcSchoolCollectionStudent().getSchoolFundingCode();
        if(isIndependentSchool && (StringUtils.equals(fundingCode, Constants.IND_FUNDING_CODE) || (hasValidBandCode && StringUtils.isBlank(fundingCode)))) {
            Map<String, Object> fteValues = new HashMap<>();
            fteValues.put("fte", BigDecimal.ZERO);
            fteValues.put("fteZeroReason", "The student is Nominal Roll eligible and is federally funded.");
            return fteValues;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
