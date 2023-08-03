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
public class SupportBlocksCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Getter
    private int processingSequenceNumber = 14;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        if (StringUtils.isBlank(studentData.getSdcSchoolCollectionStudent().getSupportBlocks()) || studentData.getSdcSchoolCollectionStudent().getSupportBlocks().equals("0")) {
            BigDecimal fteMultiplier = new BigDecimal("0.125");
            var numCoursesString = studentData.getSdcSchoolCollectionStudent().getNumberOfCourses();
            BigDecimal numCourses = StringUtils.isBlank(numCoursesString) ? BigDecimal.ZERO : new BigDecimal(numCoursesString);

            Map<String, Object> fteValues = new HashMap<>();
            fteValues.put("fte", numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
            fteValues.put("fteZeroReason", null);
            return fteValues;
        } else {
            return nextCalculator.calculateFte(studentData);
        }
    }
}
