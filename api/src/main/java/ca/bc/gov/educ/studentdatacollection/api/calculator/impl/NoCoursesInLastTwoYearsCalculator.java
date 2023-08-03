package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class NoCoursesInLastTwoYearsCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Getter
    private int processingSequenceNumber = 6;
    FteCalculatorUtils fteCalculatorUtils;

    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        if(fteCalculatorUtils.noCoursesForStudentInLastTwoYears(studentData)) {
            Map<String, Object> fteValues = new HashMap<>();
            fteValues.put("fte", BigDecimal.ZERO);
            fteValues.put("fteZeroReason", "The student has not been reported as \"active\" in a new course in the last two years.");
            return fteValues;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
