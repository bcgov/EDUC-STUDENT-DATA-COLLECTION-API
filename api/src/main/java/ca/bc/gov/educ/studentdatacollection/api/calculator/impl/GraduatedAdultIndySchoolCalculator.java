package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes.GRADUATED_ADULT_IND_AUTH;

@Component
@Slf4j
@Order(4)
public class GraduatedAdultIndySchoolCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        var isIndependentSchool = studentData.getSchool() != null && StringUtils.equals(studentData.getSchool().getSchoolCategoryCode(), SchoolCategoryCodes.INDEPEND.getCode());
        var isGraduatedAdultStudent = StringUtils.equals(studentData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode(), SchoolGradeCodes.GRADUATED_ADULT.getCode());
        if(isGraduatedAdultStudent && isIndependentSchool) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(BigDecimal.ZERO);
            fteCalculationResult.setFteZeroReason(GRADUATED_ADULT_IND_AUTH.getCode());
            return fteCalculationResult;
        } else {
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
