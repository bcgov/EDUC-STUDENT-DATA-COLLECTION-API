package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@Order(80)
public class CollectionAndFacilityTypeCalculator implements FteCalculator {
    FteCalculator nextCalculator;
    @Autowired
    FteCalculatorUtils fteCalculatorUtils;
    @Autowired
    ValidationRulesService validationRulesService;
    @Override
    public void setNext(FteCalculator nextCalculator) { this.nextCalculator = nextCalculator; }
    @Override
    public FteCalculationResult calculateFte(StudentRuleData studentData) {
        log.debug("CollectionAndFacilityTypeCalculator: Starting calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        var isJulyCollection = FteCalculatorUtils.getCollectionTypeCode(studentData).equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode());
        var isFacilityTypeSummerSchool = studentData.getSchool().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.SUMMER.getCode());
        // For July Collection and facility type different than Summer School:
        if (isJulyCollection && !isFacilityTypeSummerSchool) {
            FteCalculationResult fteCalculationResult = new FteCalculationResult();

            // v93
            var includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline(studentData);
            // The student was included in any collection in this school year for the district with a non-zero FTE
            // and was reported in any school with a type different than Online.
            if (includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline) {
                log.debug("CollectionAndFacilityTypeCalculator: FTE Zero; The district has already received funding for the student this year. :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(BigDecimal.ZERO);
                fteCalculationResult.setFteZeroReason(ZeroFteReasonCodes.DISTRICT_DUPLICATE_FUNDING.getCode());
                return fteCalculationResult;
            }

            // v93
            var includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9 = fteCalculatorUtils.includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(studentData);
            // The student was included in any collection in this school year for the district with a non-zero FTE
            // and was reported in an Online school in grade K to 9.
            if (includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9) {
                log.debug("CollectionAndFacilityTypeCalculator: FTE Zero; The district has already received funding for the student this year. :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(BigDecimal.ZERO);
                fteCalculationResult.setFteZeroReason(ZeroFteReasonCodes.DISTRICT_DUPLICATE_FUNDING.getCode());
                return fteCalculationResult;
            }

            // v99
            // The student was not reported in grade 8 or 9 with FTE>0 in any other districts in any previous collections this school year.
            var studentExistsInCurrentFiscalInGrade8Or9 = fteCalculatorUtils.reportedInOtherDistrictsInPreviousCollectionThisSchoolYearInGrade8Or9WithNonZeroFte(studentData);
            if (!studentExistsInCurrentFiscalInGrade8Or9) {
                log.debug("CollectionAndFacilityTypeCalculator: FTE Zero; Student was not reported in Grade 8 or 9 outside of district this school year. :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(BigDecimal.ZERO);
                fteCalculationResult.setFteZeroReason(ZeroFteReasonCodes.NOT_REPORTED.getCode());
                return fteCalculationResult;
            }

            // v102
            var reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear = fteCalculatorUtils.reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(studentData);
            var reportedInOnlineSchoolInCurrentCollection = fteCalculatorUtils.reportedInOnlineSchoolInCurrentCollection(studentData);
            //  The student was not reported in the Online School in July and was not reported in the online school in any previous collections this school year.
            // v102: trigger for students that are not enrolled with positive FTE in any online schools during the current school year (in any of the collections: Sep, Feb, May, or July)
            if (!reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear && !reportedInOnlineSchoolInCurrentCollection) {
                log.debug("CollectionAndFacilityTypeCalculator: FTE Zero; None of student's educational program was delivered through online learning this year. :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(BigDecimal.ZERO);
                fteCalculationResult.setFteZeroReason(ZeroFteReasonCodes.NO_ONLINE_LEARNING.getCode());
                return fteCalculationResult;
            }

            // v103
            var includedInCollectionThisSchoolYearForAuthWithNonZeroFteWithSchoolTypeNotOnline = fteCalculatorUtils.includedInCollectionThisSchoolYearForAuthWithNonZeroFteWithSchoolTypeNotOnline(studentData);
            // The student was included in any collection in this school year for the auth with a non-zero FTE
            // and was reported in any school with a type different than Online.
            if (includedInCollectionThisSchoolYearForAuthWithNonZeroFteWithSchoolTypeNotOnline) {
                log.debug("CollectionAndFacilityTypeCalculator: FTE Zero; The authority has already received funding for the student this year. :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(BigDecimal.ZERO);
                fteCalculationResult.setFteZeroReason(ZeroFteReasonCodes.IND_AUTH_DUPLICATE_FUNDING.getCode());
                return fteCalculationResult;
            }

            // v103
            var includedInCollectionThisSchoolYearForAuthWithNonZeroFteWithSchoolTypeOnlineInGradeKto9 = fteCalculatorUtils.includedInCollectionThisSchoolYearForAuthWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(studentData);
            // The student was included in any collection in this school year for the auth with a non-zero FTE
            // and was reported in an Online school in grade K to 9.
            if (includedInCollectionThisSchoolYearForAuthWithNonZeroFteWithSchoolTypeOnlineInGradeKto9) {
                log.debug("CollectionAndFacilityTypeCalculator: FTE Zero; The authority has already received funding for the student this year. :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                fteCalculationResult.setFte(BigDecimal.ZERO);
                fteCalculationResult.setFteZeroReason(ZeroFteReasonCodes.IND_AUTH_DUPLICATE_FUNDING.getCode());
                return fteCalculationResult;
            }

            log.debug("CollectionAndFacilityTypeCalculator: No FTE result, moving to next calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return this.nextCalculator.calculateFte(studentData);
        } else {
            log.debug("CollectionAndFacilityTypeCalculator: No FTE result, moving to next calculation for student :: " + studentData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            return this.nextCalculator.calculateFte(studentData);
        }
    }
}
