package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes.ZERO_COURSES;

@Component
@Slf4j
@Order(50)
public class ZeroCoursesSchoolAgedCalculator implements FteCalculator {
    FteCalculator nextCalculator;

    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        log.debug("ZeroCoursesSchoolAgedCalculator: Starting calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        var student = studentData.getSdcSchoolCollectionStudentEntity();

        if(!isOnlineSchool(studentData)
                && Boolean.TRUE.equals(student.getIsSchoolAged())
                && student.getNumberOfCoursesDec().compareTo(BigDecimal.ZERO) <= 0
                && SchoolGradeCodes.get8PlusGradesNoGA().contains(student.getEnrolledGradeCode())) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(BigDecimal.ZERO);
            fteCalculationResult.setFteZeroReason(ZERO_COURSES.getCode());
            log.debug("ZeroCoursesSchoolAgedCalculator: Fte result {} calculated with zero reason '{}' for student :: {}", fteCalculationResult.getFte(), fteCalculationResult.getFteZeroReason(), studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return fteCalculationResult;
        } else {
            log.debug("ZeroCoursesSchoolAgedCalculator: No FTE result, moving to next calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return this.nextCalculator.calculateFte(studentData);
        }
    }

    private boolean isOnlineSchool(StudentRuleData studentRuleData) {
        return studentRuleData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DISTONLINE.getCode()) ||  studentRuleData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DIST_LEARN.getCode());
    }
}
