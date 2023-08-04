package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class AlternateProgramsCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Getter
    private int processingSequenceNumber = 12;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(SdcStudentSagaData studentData) {
        var isNonGraduate = !Boolean.TRUE.equals(studentData.getSdcSchoolCollectionStudent().getIsGraduated());
        var schoolHasAlternateProgram = studentData.getSchool() != null && StringUtils.equals(studentData.getSchool().getFacilityTypeCode(), FacilityTypeCodes.ALT_PROGS.getCode());
        var gradeCodeInList = SchoolGradeCodes.getKfOneToSevenEuGrades().contains(studentData.getSdcSchoolCollectionStudent().getEnrolledGradeCode());

        if(gradeCodeInList || (isNonGraduate && schoolHasAlternateProgram)) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(BigDecimal.ONE);
            fteCalculationResult.setFteZeroReason(null);
            return fteCalculationResult;
        } else {
            return nextCalculator.calculateFte(studentData);
        }
    }
}
