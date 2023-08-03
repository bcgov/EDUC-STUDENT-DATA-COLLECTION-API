package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class OffshoreStudentCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Getter
    private int processingSequenceNumber = 1;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        if(studentData.getSchool() != null && StringUtils.equals(studentData.getSchool().getSchoolCategoryCode(), SchoolCategoryCodes.OFFSHORE.getCode())) {
            Map<String, Object> fteValues = new HashMap<>();
            fteValues.put("fte", BigDecimal.ZERO);
            fteValues.put("fteZeroReason", "Offshore students do not receive funding.");
            return fteValues;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
