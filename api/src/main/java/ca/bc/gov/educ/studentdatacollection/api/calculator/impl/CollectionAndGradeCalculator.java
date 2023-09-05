package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
@Order(10)
public class CollectionAndGradeCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        var student = studentData.getSdcSchoolCollectionStudentEntity();
        var isJulyCollection = StringUtils.equals(studentData.getCollectionTypeCode(), CollectionTypeCodes.JULY.getTypeCode());
        if(isJulyCollection) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            if(SchoolGradeCodes.get1To7Grades().contains(student.getEnrolledGradeCode())) {
                fteCalculationResult.setFte(BigDecimal.ONE);
            } else if (SchoolGradeCodes.get8To12Grades().contains(student.getEnrolledGradeCode())) {
                BigDecimal fteMultiplier = new BigDecimal("0.125");
                BigDecimal numCourses = StringUtils.isBlank(student.getNumberOfCourses()) ? BigDecimal.ZERO : BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(student.getNumberOfCourses(), student.getSdcSchoolCollectionStudentID()));
                fteCalculationResult.setFte(numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
            } else {
                String errorMessage = "SdcStudentSagaData has invalid enrolledGradeCode for a summer collection :: " + student.getSdcSchoolCollection().getSdcSchoolCollectionID();
                log.error(errorMessage);
                throw new StudentDataCollectionAPIRuntimeException(errorMessage);
            }
            fteCalculationResult.setFteZeroReason(null);
            return fteCalculationResult;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
