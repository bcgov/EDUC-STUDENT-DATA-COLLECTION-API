package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.SagaRuntimeException;
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
@Order(13)
public class StudentGradeCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        String grade = studentData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode();
        BigDecimal fte;
        FteCalculationResult fteCalculationResult = new FteCalculationResult();

        if(StringUtils.equals(SchoolGradeCodes.KINDHALF.getCode(), grade)) {
            fteCalculationResult.setFte(new BigDecimal("0.5"));
        } else if (StringUtils.equals(SchoolGradeCodes.HOMESCHOOL.getCode(), grade)) {
            fteCalculationResult.setFte(new BigDecimal("0.0471"));
        } else if (StringUtils.equals(SchoolGradeCodes.GRADE08.getCode(), grade) || StringUtils.equals(SchoolGradeCodes.GRADE09.getCode(), grade)) {
            BigDecimal fteMultiplier = new BigDecimal("0.125");
            BigDecimal numCourses = StringUtils.isBlank(studentData.getSdcSchoolCollectionStudentEntity().getNumberOfCourses())
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(studentData.getSdcSchoolCollectionStudentEntity().getNumberOfCourses(), studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID()));
            fte = (numCourses.multiply(fteMultiplier).add(new BigDecimal("0.5"))).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
            fteCalculationResult.setFte(fte.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : fte);
        } else if (SchoolGradeCodes.getHighSchoolGrades().contains(grade)) {
            return nextCalculator.calculateFte(studentData);
        } else {
            String errorMessage = "SdcStudentSagaData has invalid enrolledGradeCode for :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getSdcSchoolCollectionID();
            log.error(errorMessage);
            throw new SagaRuntimeException(errorMessage);
        }
        fteCalculationResult.setFteZeroReason(null);
        return fteCalculationResult;
    }
}
