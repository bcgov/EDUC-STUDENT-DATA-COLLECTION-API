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
@Order(150)
public class SupportBlocksCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Override
    public void setNext(FteCalculator nextCalculator) {
        this.nextCalculator = nextCalculator;
    }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        log.debug("SupportBlocksCalculator: Starting calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        if (StringUtils.isBlank(studentData.getSdcSchoolCollectionStudentEntity().getSupportBlocks()) || studentData.getSdcSchoolCollectionStudentEntity().getSupportBlocks().equals("0")) {
            BigDecimal fteMultiplier = new BigDecimal("0.125");
            var numCoursesString = studentData.getSdcSchoolCollectionStudentEntity().getNumberOfCourses();
            BigDecimal numCourses = StringUtils.isBlank(numCoursesString) ? BigDecimal.ZERO : BigDecimal.valueOf(TransformUtil.parseNumberOfCourses(numCoursesString, studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID()));

            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            fteCalculationResult.setFte(numCourses.multiply(fteMultiplier).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros());
            if(SchoolCategoryCodes.INDEPENDENTS.contains(studentData.getSchool().getSchoolCategoryCode()) && FacilityTypeCodes.STANDARD.getCode().equals(studentData.getSchool().getFacilityTypeCode())) {
                fteCalculationResult.setFte(fteCalculationResult.getFte().compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : fteCalculationResult.getFte());
            }
            if (fteCalculationResult.getFte() != null && fteCalculationResult.getFte().compareTo(BigDecimal.ZERO) > 0) {
                fteCalculationResult.setFteZeroReason(null);
            }else{
                fteCalculationResult.setFteZeroReason(NUM_COURSES.getCode());
            }
            log.debug("SupportBlocksCalculator: Fte result {} calculated for student :: {}", fteCalculationResult.getFte(), studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return fteCalculationResult;
        } else {
            log.debug("SupportBlocksCalculator: No FTE result, moving to next calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return nextCalculator.calculateFte(studentData);
        }
    }
}
