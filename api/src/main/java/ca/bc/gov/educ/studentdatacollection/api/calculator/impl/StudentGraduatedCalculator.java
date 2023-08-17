package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.helpers.BooleanString;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Order(15)
public class StudentGraduatedCalculator implements FteCalculator {
    @Override
    public void setNext(FteCalculator nextCalculator) {
        // This is a final node of the decision tree, so there is no next to set
    }
    @Override
    public FteCalculationResult calculateFte(SdcStudentSagaData studentData) {
        BigDecimal fteMultiplier = new BigDecimal("0.125");
        BigDecimal numCourses = StringUtils.isBlank(studentData.getSdcSchoolCollectionStudent().getNumberOfCourses())
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(studentData.getSdcSchoolCollectionStudent().getNumberOfCourses(), studentData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID()));
        FteCalculationResult fteCalculationResult = new FteCalculationResult();
        if (BooleanString.areEqual(studentData.getSdcSchoolCollectionStudent().getIsGraduated(), Boolean.TRUE)) {
            fteCalculationResult.setFte(numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
        } else {
            BigDecimal numSupportBlocks = new BigDecimal(studentData.getSdcSchoolCollectionStudent().getSupportBlocks());
            BigDecimal fte = (numCourses.add(numSupportBlocks).multiply(fteMultiplier)).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
            fteCalculationResult.setFte(fte.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : fte);
        }
        fteCalculationResult.setFteZeroReason(null);
        return fteCalculationResult;
    }
}
