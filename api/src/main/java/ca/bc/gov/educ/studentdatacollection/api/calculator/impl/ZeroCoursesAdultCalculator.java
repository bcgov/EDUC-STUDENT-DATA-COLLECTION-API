package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes.ZERO_COURSES;

@Component
@Slf4j
@Order(5)
public class ZeroCoursesAdultCalculator implements FteCalculator {
    FteCalculator nextCalculator;

    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        log.debug("ZeroCoursesAdultCalculator: Starting calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        var student = studentData.getSdcSchoolCollectionStudentEntity();
        if(Boolean.TRUE.equals(student.getIsAdult()) && student.getNumberOfCoursesDec().compareTo(BigDecimal.ZERO) <= 0) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(BigDecimal.ZERO);
            fteCalculationResult.setFteZeroReason(ZERO_COURSES.getCode());
            log.debug("ZeroCoursesAdultCalculator: Fte result {} calculated with zero reason '{}' for student :: {}", fteCalculationResult.getFte(), fteCalculationResult.getFteZeroReason(), studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return fteCalculationResult;
        } else {
            log.debug("ZeroCoursesAdultCalculator: No FTE result, moving to next calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}