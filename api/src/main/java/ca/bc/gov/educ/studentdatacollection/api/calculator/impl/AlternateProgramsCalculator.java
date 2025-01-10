package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@Order(130)
public class AlternateProgramsCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        log.debug("AlternateProgramsCalculator: Starting calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        boolean hasGraduated = Boolean.TRUE.equals(studentData.getSdcSchoolCollectionStudentEntity().getIsGraduated());
        var schoolHasAlternateProgram = studentData.getSchool() != null && StringUtils.equals(studentData.getSchool().getFacilityTypeCode(), FacilityTypeCodes.ALT_PROGS.getCode());
        var gradeCodeInList = SchoolGradeCodes.getKfOneToSevenEuGrades().contains(studentData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode());
        var isHSGrade = StringUtils.equals(studentData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode(), SchoolGradeCodes.HOMESCHOOL.getCode());

        if(gradeCodeInList || (!hasGraduated && schoolHasAlternateProgram && !isHSGrade)) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(BigDecimal.ONE);
            fteCalculationResult.setFteZeroReason(null);
            log.debug("AlternateProgramsCalculator: Fte result {} calculated for student :: {}", fteCalculationResult.getFte(), studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return fteCalculationResult;
        } else {
            log.debug("AlternateProgramsCalculator: No FTE result, moving to next calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return nextCalculator.calculateFte(studentData);
        }
    }
}
