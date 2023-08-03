package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
    public Map<String, Object> calculateFte(SdcStudentSagaData studentData) {
        var isNonGraduate = !Boolean.TRUE.equals(studentData.getSdcSchoolCollectionStudent().getIsGraduated());
        var schoolHasAlternateProgram = studentData.getSchool() != null && StringUtils.equals(studentData.getSchool().getFacilityTypeCode(), FacilityTypeCodes.ALT_PROGS.getCode());
        var gradeCodeInList = SchoolGradeCodes.getKToNineGrades().contains(studentData.getSdcSchoolCollectionStudent().getEnrolledGradeCode());

        if(gradeCodeInList || (isNonGraduate && schoolHasAlternateProgram)) {
            Map<String, Object> fteValues = new HashMap<>();
            fteValues.put("fte", BigDecimal.ONE);
            fteValues.put("fteZeroReason", null);
            return fteValues;
        } else {
            return nextCalculator.calculateFte(studentData);
        }
    }
}
