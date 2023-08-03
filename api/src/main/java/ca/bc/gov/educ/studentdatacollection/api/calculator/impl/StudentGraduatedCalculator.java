package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class StudentGraduatedCalculator implements FteCalculator {
    @Getter
    private int processingSequenceNumber = 15;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        // This is a final node of the decision tree, so there is no next to set
    }
    @Override
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        BigDecimal fteMultiplier = new BigDecimal("0.125");
        BigDecimal numCourses = StringUtils.isBlank(studentData.getSdcSchoolCollectionStudent().getNumberOfCourses()) ? BigDecimal.ZERO : new BigDecimal(studentData.getSdcSchoolCollectionStudent().getNumberOfCourses());
        Map<String, Object> fteValues = new HashMap<>();
        if (Boolean.TRUE.equals(studentData.getSdcSchoolCollectionStudent().getIsGraduated())) {
            fteValues.put("fte", numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
        } else {
            BigDecimal numSupportBlocks = new BigDecimal(studentData.getSdcSchoolCollectionStudent().getSupportBlocks());
            BigDecimal fte = (numCourses.add(numSupportBlocks).multiply(fteMultiplier)).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
            fteValues.put("fte", fte.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : fte);
        }
        fteValues.put("fteZeroReason", null);
        return fteValues;
    }
}
