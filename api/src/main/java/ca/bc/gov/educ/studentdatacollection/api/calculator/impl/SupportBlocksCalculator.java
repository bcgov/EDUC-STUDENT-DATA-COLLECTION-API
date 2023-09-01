package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
@Order(14)
public class SupportBlocksCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        if (StringUtils.isBlank(studentData.getSdcSchoolCollectionStudentEntity().getSupportBlocks()) || studentData.getSdcSchoolCollectionStudentEntity().getSupportBlocks().equals("0")) {
            BigDecimal fteMultiplier = new BigDecimal("0.125");
            var numCoursesString = studentData.getSdcSchoolCollectionStudentEntity().getNumberOfCourses();
            BigDecimal numCourses = StringUtils.isBlank(numCoursesString) ? BigDecimal.ZERO : BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(numCoursesString, studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID()));

            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
            fteCalculationResult.setFteZeroReason(null);
            return fteCalculationResult;
        } else {
            return nextCalculator.calculateFte(studentData);
        }
    }
}
