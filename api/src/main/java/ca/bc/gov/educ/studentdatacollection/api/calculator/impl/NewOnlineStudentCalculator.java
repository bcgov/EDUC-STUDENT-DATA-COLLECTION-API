package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class NewOnlineStudentCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Getter
    private int processingSequenceNumber = 11;
    FteCalculatorUtils fteCalculatorUtils;

    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        if(fteCalculatorUtils.homeSchoolStudentIsNowOnlineKto9Student(studentData)) {
            var student = studentData.getSdcSchoolCollectionStudent();
            Map<String, Object> fteValues = new HashMap<>();
            if(student.getEnrolledGradeCode().equals(SchoolGradeCodes.KINDHALF.getCode())) {
                fteValues.put("fte", new BigDecimal("0.4529"));
            } else if (student.getEnrolledGradeCode().equals(SchoolGradeCodes.GRADE08.getCode()) || student.getEnrolledGradeCode().equals(SchoolGradeCodes.GRADE09.getCode())) {
                BigDecimal fteMultiplier = new BigDecimal("0.125");
                BigDecimal numCourses = new BigDecimal(studentData.getSdcSchoolCollectionStudent().getNumberOfCourses());
                BigDecimal largestFte = new BigDecimal("0.9529");
                var fte = (numCourses.multiply(fteMultiplier).add(new BigDecimal("0.5"))).setScale(4, RoundingMode.HALF_UP);
                fteValues.put("fte", fte.compareTo(largestFte) > 0 ? largestFte : fte);
            } else {
                fteValues.put("fte", new BigDecimal("0.9529"));
            }
            fteValues.put("fteZeroReason", null);
            return fteValues;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
