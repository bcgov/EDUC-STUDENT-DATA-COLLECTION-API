package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class FteCalculatorChainProcessor {

    private final List<FteCalculator> fteCalculators;
    private final ValidationRulesService validationRulesService;

    public FteCalculatorChainProcessor(final List<FteCalculator> fteCalculators, ValidationRulesService validationRulesService) {
        this.fteCalculators = fteCalculators;
        this.validationRulesService = validationRulesService;
    }

    public FteCalculationResult processFteCalculator(StudentRuleData studentRuleData) {
       log.debug("Starting FTE calculation for student :: {} with data :: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID(), studentRuleData);
       validationRulesService.setupPENMatchAndEllAndGraduateValues(studentRuleData);
       var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
       String collectionType = student.getSdcSchoolCollection().getCollectionEntity().getCollectionTypeCode();
       String facilityType = studentRuleData.getSchool().getFacilityTypeCode();
       var onlineFacilityCodes = Arrays.asList(FacilityTypeCodes.DISTONLINE.getCode(), FacilityTypeCodes.DIST_LEARN.getCode());
       var historicalIndyStudents = validationRulesService.findIndyStudentInCurrentFiscal(studentRuleData, "8", studentRuleData.getSchool().getIndependentAuthorityId());

       for (int i = 0; i < fteCalculators.size() - 1; i++) {
           FteCalculator currentCalculator = fteCalculators.get(i);
           FteCalculator nextCalculator = fteCalculators.get(i + 1);
           currentCalculator.setNext(nextCalculator);
       }
       var fteResult = fteCalculators.get(0).calculateFte(studentRuleData);
       if(SchoolCategoryCodes.INDEPENDENTS.contains(studentRuleData.getSchool().getSchoolCategoryCode())
               && student.getFte() != null
               && collectionType.equalsIgnoreCase(CollectionTypeCodes.FEBRUARY.getTypeCode()) && onlineFacilityCodes.contains(facilityType)
               && (historicalIndyStudents.isEmpty()
               || historicalIndyStudents.stream().noneMatch(stu -> stu.getFte().compareTo(BigDecimal.ZERO) > 0))
               && student.getFte().compareTo(BigDecimal.ZERO) == 0) {
          log.debug("ProgramEligibilityBaseRule - SpecialEducationProgramsRule: CollectionTypeCodes - {}, facilityType - {}, for sdcSchoolCollectionStudentID :: {}", collectionType, facilityType, studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
          studentRuleData.getSdcSchoolCollectionStudentEntity().setSpecialEducationNonEligReasonCode(ProgramEligibilityIssueCode.FEB_ONLINE_WITH_HISTORICAL_FUNDING.getCode());
       }
       return fteResult;
    }
}
