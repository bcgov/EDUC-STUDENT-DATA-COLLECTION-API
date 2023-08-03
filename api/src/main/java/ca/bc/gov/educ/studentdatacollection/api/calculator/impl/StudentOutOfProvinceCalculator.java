package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.Constants;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class StudentOutOfProvinceCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Getter
    private int processingSequenceNumber = 2;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        if(StringUtils.equals(studentData.getSdcSchoolCollectionStudent().getSchoolFundingCode(), Constants.FUNDING_CODE_14)) {
            Map<String, Object> fteValues = new HashMap<>();
            fteValues.put("fte", BigDecimal.ZERO);
            fteValues.put("fteZeroReason", "Out-of-Province/International Students are not eligible for funding.");
            return fteValues;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
