package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes.NUM_COURSES;

@Component
@Slf4j
@Order(100)
public class AdultStudentCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        log.debug("AdultStudentCalculator: Starting calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        var sdcSchoolStudent = studentData.getSdcSchoolCollectionStudentEntity();
        boolean isAdult = Boolean.TRUE.equals(sdcSchoolStudent.getIsAdult());
        if (isAdult || StringUtils.equals(sdcSchoolStudent.getEnrolledGradeCode(), SchoolGradeCodes.GRADUATED_ADULT.getCode())) {
            BigDecimal fteMultiplier = new BigDecimal("0.125");
            BigDecimal numCourses = StringUtils.isBlank(sdcSchoolStudent.getNumberOfCourses()) ? BigDecimal.ZERO : BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(sdcSchoolStudent.getNumberOfCourses(), sdcSchoolStudent.getSdcSchoolCollectionStudentID()));
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
            if (fteCalculationResult.getFte() != null && fteCalculationResult.getFte().compareTo(BigDecimal.ZERO) > 0) {
                fteCalculationResult.setFteZeroReason(null);
            }else{
                fteCalculationResult.setFteZeroReason(NUM_COURSES.getCode());
            }
            log.debug("AdultStudentCalculator: Fte result {} calculated for student :: {}", fteCalculationResult.getFte(), studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return fteCalculationResult;
        } else {
            log.debug("AdultStudentCalculator: No FTE result, moving to next calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
