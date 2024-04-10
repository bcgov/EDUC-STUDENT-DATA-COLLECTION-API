package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
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
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        log.debug("NewOnlineStudentCalculator: Starting calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        if(fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9StudentOrHs(studentData)) {
            var student = studentData.getSdcSchoolCollectionStudentEntity();
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            if(student.getEnrolledGradeCode().equals(SchoolGradeCodes.KINDHALF.getCode())) {
                log.debug("NewOnlineStudentCalculator: calculating for a half kindergarten student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(new BigDecimal("0.4529"));
            } else if (student.getEnrolledGradeCode().equals(SchoolGradeCodes.GRADE08.getCode()) || student.getEnrolledGradeCode().equals(SchoolGradeCodes.GRADE09.getCode())) {
                log.debug("NewOnlineStudentCalculator: calculating for a grade 8 or 9 student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                BigDecimal fteMultiplier = new BigDecimal("0.125");
                BigDecimal numCourses = StringUtils.isBlank(studentData.getSdcSchoolCollectionStudentEntity().getNumberOfCourses()) ? BigDecimal.ZERO : BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(student.getNumberOfCourses(), student.getSdcSchoolCollectionStudentID()));
                BigDecimal largestFte = new BigDecimal("0.9529");
                var fte = (numCourses.multiply(fteMultiplier).add(new BigDecimal("0.5"))).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
                fteCalculationResult.setFte(fte.compareTo(largestFte) > 0 ? largestFte : fte);
            } else if (student.getEnrolledGradeCode().equals(SchoolGradeCodes.HOMESCHOOL.getCode())) {
                log.debug("NewOnlineStudentCalculator: calculating for a homeschool student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(BigDecimal.ZERO);
            } else {
                log.debug("NewOnlineStudentCalculator: calculating for all other grades with student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(new BigDecimal("0.9529"));
            }
            fteCalculationResult.setFteZeroReason(null);
            log.debug("NewOnlineStudentCalculator: Fte result {} calculated for student :: {}", fteCalculationResult.getFte(), studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return fteCalculationResult;
        } else {
            log.debug("NewOnlineStudentCalculator: No FTE result, moving to next calculation for student  :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
