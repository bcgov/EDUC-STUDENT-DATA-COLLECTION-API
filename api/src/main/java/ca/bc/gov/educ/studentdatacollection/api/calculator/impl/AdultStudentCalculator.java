package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
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
@Order(9)
public class AdultStudentCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(SdcStudentSagaData studentData) {
        var sdcSchoolStudent = studentData.getSdcSchoolCollectionStudent();
        boolean isAdult = BooleanString.equal(sdcSchoolStudent.getIsAdult(), Boolean.TRUE);
        if (isAdult || StringUtils.equals(sdcSchoolStudent.getEnrolledGradeCode(), SchoolGradeCodes.GRADUATED_ADULT.getCode())) {
            BigDecimal fteMultiplier = new BigDecimal("0.125");
            BigDecimal numCourses = StringUtils.isBlank(sdcSchoolStudent.getNumberOfCourses()) ? BigDecimal.ZERO : BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(sdcSchoolStudent.getNumberOfCourses(), sdcSchoolStudent.getSdcSchoolCollectionStudentID()));
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
            fteCalculationResult.setFteZeroReason(null);
            return fteCalculationResult;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
