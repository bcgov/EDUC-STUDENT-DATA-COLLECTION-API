package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
@Order(11)
public class NewOnlineStudentCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Autowired
    FteCalculatorUtils fteCalculatorUtils;

    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(SdcStudentSagaData studentData) {
        if(fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9Student(studentData)) {
            var student = studentData.getSdcSchoolCollectionStudent();
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            if(student.getEnrolledGradeCode().equals(SchoolGradeCodes.KINDHALF.getCode())) {
                fteCalculationResult.setFte(new BigDecimal("0.4529"));
            } else if (student.getEnrolledGradeCode().equals(SchoolGradeCodes.GRADE08.getCode()) || student.getEnrolledGradeCode().equals(SchoolGradeCodes.GRADE09.getCode())) {
                BigDecimal fteMultiplier = new BigDecimal("0.125");
                BigDecimal numCourses = StringUtils.isBlank(studentData.getSdcSchoolCollectionStudent().getNumberOfCourses()) ? BigDecimal.ZERO : BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(student.getNumberOfCourses(), student.getSdcSchoolCollectionStudentID()));
                BigDecimal largestFte = new BigDecimal("0.9529");
                var fte = (numCourses.multiply(fteMultiplier).add(new BigDecimal("0.5"))).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
                fteCalculationResult.setFte(fte.compareTo(largestFte) > 0 ? largestFte : fte);
            } else {
                fteCalculationResult.setFte(new BigDecimal("0.9529"));
            }
            fteCalculationResult.setFteZeroReason(null);
            return fteCalculationResult;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
