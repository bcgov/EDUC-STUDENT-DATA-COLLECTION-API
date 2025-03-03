package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes.NUM_COURSES;

@Component
@Slf4j
@Order(160)
public class StudentGraduatedCalculator implements FteCalculator {
    @Override
    public void setNext(FteCalculator nextCalculator) {
        // This is a final node of the decision tree, so there is no next to set
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        log.debug("StudentGraduatedCalculator: Starting calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        BigDecimal fteMultiplier = new BigDecimal("0.125");
        BigDecimal numCourses = StringUtils.isBlank(studentData.getSdcSchoolCollectionStudentEntity().getNumberOfCourses())
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(studentData.getSdcSchoolCollectionStudentEntity().getNumberOfCourses(), studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID()));
        FteCalculationResult fteCalculationResult = new FteCalculationResult();
        if (Boolean.TRUE.equals(studentData.getSdcSchoolCollectionStudentEntity().getIsGraduated())) {
            log.debug("StudentGraduatedCalculator: calculating for a graduated student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            fteCalculationResult.setFte(numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
            if(SchoolCategoryCodes.INDEPENDENTS.contains(studentData.getSchool().getSchoolCategoryCode()) && FacilityTypeCodes.STANDARD.getCode().equals(studentData.getSchool().getFacilityTypeCode())) {
                fteCalculationResult.setFte(fteCalculationResult.getFte().compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : fteCalculationResult.getFte());
            }
        } else {
            log.debug("StudentGraduatedCalculator: calculating for a non-graduated student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            BigDecimal fte = (numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
            fteCalculationResult.setFte(fte);

            if(fte.compareTo(BigDecimal.ONE) < 0){
                BigDecimal numSupportBlocks = new BigDecimal(studentData.getSdcSchoolCollectionStudentEntity().getSupportBlocks());
                fte = (numCourses.add(numSupportBlocks).multiply(fteMultiplier)).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
                fteCalculationResult.setFte(fte.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : fte);
            }

            if(SchoolCategoryCodes.INDEPENDENTS.contains(studentData.getSchool().getSchoolCategoryCode()) && FacilityTypeCodes.STANDARD.getCode().equals(studentData.getSchool().getFacilityTypeCode())) {
                fteCalculationResult.setFte(fte.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : fte);
            }
        }
        if (fteCalculationResult.getFte() != null && fteCalculationResult.getFte().compareTo(BigDecimal.ZERO) > 0) {
            fteCalculationResult.setFteZeroReason(null);
        }else{
            fteCalculationResult.setFteZeroReason(NUM_COURSES.getCode());
        }
        log.debug("StudentGraduatedCalculator: Fte result {} calculated for student :: {}", fteCalculationResult.getFte(), studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return fteCalculationResult;
    }
}
