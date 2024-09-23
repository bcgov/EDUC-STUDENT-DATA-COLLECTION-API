package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@Order(16)
public class CollectionAndFacilityTypeCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Autowired
    FteCalculatorUtils fteCalculatorUtils;
    @Override
    public void setNext(FteCalculator nextCalculator) { this.nextCalculator = nextCalculator; }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        log.debug("CollectionAndFacilityTypeCalculator: Starting calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        var isJulyCollection = FteCalculatorUtils.getCollectionTypeCode(studentData).equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode());
        var isFacilityTypeSummerSchool = studentData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.SUMMER.getCode());
        var isFacilityTypeOnline = FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(studentData.getSchool().getFacilityTypeCode());
        // For July Collection and facility type different than Summer School:
        if (isJulyCollection && !isFacilityTypeSummerSchool) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();
            var includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline(studentData);
            // The student was included in any collection in this school year for the district with a non-zero FTE
            // and was reported in any school with a type different than Online.
            if (includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline) {
                log.debug("CollectionAndFacilityTypeCalculator: FTE Zero; The district has already received funding for the student this year. :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(BigDecimal.ZERO);
                fteCalculationResult.setFteZeroReason("The district has already received funding for the student this year.");
            }

            var includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9 = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(studentData);
            // The student was included in any collection in this school year for the district with a non-zero FTE
            // and was reported in  an Online school in grade K to 9.
            if (includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9) {
                log.debug("CollectionAndFacilityTypeCalculator: FTE Zero; The district has already received funding for the student this year. :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(BigDecimal.ZERO);
                fteCalculationResult.setFteZeroReason("The district has already received funding for the student this year.");
            }

            //  The student was not reported in the Online School in July.
            if (!isFacilityTypeOnline) {
                var reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear = fteCalculatorUtils.reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(studentData);
                //  and was not reported in the online school in any previous collections this school year.
                if (!reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear) {
                    log.debug("CollectionAndFacilityTypeCalculator: FTE Zero; None of student's educational program was delivered through online learning this year. :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                    fteCalculationResult.setFte(BigDecimal.ZERO);
                    fteCalculationResult.setFteZeroReason("None of student's educational program was delivered through online learning this year.");
                }
            }

            return fteCalculationResult;
        } else {
            log.debug("CollectionAndFacilityTypeCalculator: No FTE result, moving to next calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
