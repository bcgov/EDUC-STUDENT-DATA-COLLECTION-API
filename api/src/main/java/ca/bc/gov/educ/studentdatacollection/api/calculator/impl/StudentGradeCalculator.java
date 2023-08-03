package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.SagaRuntimeException;
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
public class StudentGradeCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Getter
    private int processingSequenceNumber = 13;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        String grade = studentData.getSdcSchoolCollectionStudent().getEnrolledGradeCode();
        BigDecimal fte;
        Map<String, Object> fteValues = new HashMap<>();

        if(StringUtils.equals(SchoolGradeCodes.KINDHALF.getCode(), grade)) {
            fteValues.put("fte", new BigDecimal("0.5"));
        } else if (StringUtils.equals(SchoolGradeCodes.HOMESCHOOL.getCode(), grade)) {
            fteValues.put("fte", new BigDecimal("0.0471"));
        } else if (StringUtils.equals(SchoolGradeCodes.GRADE08.getCode(), grade) || StringUtils.equals(SchoolGradeCodes.GRADE09.getCode(), grade)) {
            BigDecimal fteMultiplier = new BigDecimal("0.125");
            BigDecimal numCourses = new BigDecimal(StringUtils.isBlank(studentData.getSdcSchoolCollectionStudent().getNumberOfCourses()) ? "0" : studentData.getSdcSchoolCollectionStudent().getNumberOfCourses());
            fte = (numCourses.multiply(fteMultiplier).add(new BigDecimal("0.5"))).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
            fteValues.put("fte", fte.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : fte);
        } else if (SchoolGradeCodes.getHighSchoolGrades().contains(grade)) {
            return nextCalculator.calculateFte(studentData);
        } else {
            String errorMessage = "SdcStudentSagaData has invalid enrolledGradeCode for :: " + studentData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionID();
            log.error(errorMessage);
            throw new SagaRuntimeException(errorMessage);
        }
        fteValues.put("fteZeroReason", null);
        return fteValues;
    }
}
