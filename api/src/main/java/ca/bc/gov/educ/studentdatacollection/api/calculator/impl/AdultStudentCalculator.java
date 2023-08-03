package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
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
public class AdultStudentCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Getter
    private int processingSequenceNumber = 9;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        var sdcSchoolStudent = studentData.getSdcSchoolCollectionStudent();
        if(Boolean.TRUE.equals(sdcSchoolStudent.getIsAdult()) || StringUtils.equals(sdcSchoolStudent.getEnrolledGradeCode(), SchoolGradeCodes.GRADUATED_ADULT.getCode())) {
            BigDecimal fteMultiplier = new BigDecimal("0.125");
            BigDecimal numCourses = StringUtils.isBlank(sdcSchoolStudent.getNumberOfCourses()) ? BigDecimal.ZERO : new BigDecimal(sdcSchoolStudent.getNumberOfCourses());
            Map<String, Object> fteValues = new HashMap<>();
            fteValues.put("fte", numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
            fteValues.put("fteZeroReason", null);
            return fteValues;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
