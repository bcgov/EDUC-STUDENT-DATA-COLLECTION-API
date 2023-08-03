package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
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
public class CollectionAndGradeCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Getter
    private int processingSequenceNumber = 10;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        var student = studentData.getSdcSchoolCollectionStudent();
        var isJulyCollection = StringUtils.equals(studentData.getCollectionTypeCode(), CollectionTypeCodes.ENTRY4.getTypeCode());
        if(isJulyCollection) {
            Map<String, Object> fteValues = new HashMap<>();
            if(SchoolGradeCodes.get1To7Grades().contains(student.getEnrolledGradeCode())) {
                fteValues.put("fte", BigDecimal.ONE);
            } else if (SchoolGradeCodes.get9To12Grades().contains(student.getEnrolledGradeCode())) {
                BigDecimal fteMultiplier = new BigDecimal("0.125");
                BigDecimal numCourses = StringUtils.isBlank(student.getNumberOfCourses()) ? BigDecimal.ZERO : new BigDecimal(student.getNumberOfCourses());
                fteValues.put("fte", numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
            } else {
                String errorMessage = "SdcStudentSagaData has invalid enrolledGradeCode for a summer collection :: " + student.getSdcSchoolCollectionID();
                log.error(errorMessage);
                throw new SagaRuntimeException(errorMessage);
            }
            fteValues.put("fteZeroReason", null);
            return fteValues;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
