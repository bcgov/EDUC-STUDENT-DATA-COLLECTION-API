package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(12)
public class AlternateProgramsCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        boolean hasGraduated = Boolean.TRUE.equals(studentData.getSdcSchoolCollectionStudentEntity().getIsGraduated());
        var schoolHasAlternateProgram = studentData.getSchool() != null && StringUtils.equals(studentData.getSchool().getFacilityTypeCode(), FacilityTypeCodes.ALT_PROGS.getCode());
        var gradeCodeInList = SchoolGradeCodes.getKfOneToSevenEuGrades().contains(studentData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode());

        if(gradeCodeInList || (!hasGraduated && schoolHasAlternateProgram)) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(BigDecimal.ONE);
            fteCalculationResult.setFteZeroReason(null);
            return fteCalculationResult;
        } else {
            return nextCalculator.calculateFte(studentData);
        }
    }
}
