package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SpecialEducationCategoryCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@Order(6)
public class SpecialEducationProgramsRule implements ProgramEligibilityBaseRule {
  private final ValidationRulesService validationRulesService;

  public SpecialEducationProgramsRule(ValidationRulesService validationRulesService) {
    this.validationRulesService = validationRulesService;
  }

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> errors) {
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - SpecialEducationProgramsRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    log.debug("In shouldExecute of ProgramEligibilityBaseRule - SpecialEducationProgramsRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
            hasNotViolatedBaseRules(errors),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    return hasNotViolatedBaseRules(errors);
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    log.debug("In executeValidation of ProgramEligibilityBaseRule - SpecialEducationProgramsRule for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
    var onlineAndContinuingFacilityCodes = Arrays.asList(FacilityTypeCodes.DISTONLINE.getCode(), FacilityTypeCodes.DIST_LEARN.getCode());

    List<String> activeSpecialEdPrograms = validationRulesService.getActiveSpecialEducationCategoryCodes().stream().map(SpecialEducationCategoryCode::getSpecialEducationCategoryCode).toList();

    Boolean isSchoolAged = student.getIsSchoolAged();
    boolean isGraduated = student.getIsGraduated() != null && student.getIsGraduated();
    Boolean isAdult = student.getIsAdult();
    Boolean isGA = SchoolGradeCodes.GRADUATED_ADULT.getCode().equals(student.getEnrolledGradeCode());
    String collectionType = student.getSdcSchoolCollection().getCollectionEntity().getCollectionTypeCode();
    String facilityType = studentRuleData.getSchool().getFacilityTypeCode();
    var historicalStudents = validationRulesService.getStudentInHistoricalCollectionWithInSameDistrict(studentRuleData, "1");

    if (StringUtils.isEmpty(student.getSpecialEducationCategoryCode()) || !activeSpecialEdPrograms.contains(student.getSpecialEducationCategoryCode())) {
      log.debug("ProgramEligibilityBaseRule - SpecialEducationProgramsRule: Sped code value - {} for sdcSchoolCollectionStudentID :: {}", student.getSpecialEducationCategoryCode(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
      errors.add(ProgramEligibilityIssueCode.NOT_ENROLLED_SPECIAL_ED);
    } else if (Boolean.FALSE.equals(isSchoolAged) && (isGraduated || (isAdult && isGA))) {
      log.debug("ProgramEligibilityBaseRule - SpecialEducationProgramsRule: Is school aged - {}, Is non graduated adult - {}, for sdcSchoolCollectionStudentID :: {}", student.getIsSchoolAged(), student.getIsGraduated(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
      errors.add(ProgramEligibilityIssueCode.NON_ELIG_SPECIAL_EDUCATION);
    } else if(collectionType.equalsIgnoreCase(CollectionTypeCodes.FEBRUARY.getTypeCode()) && onlineAndContinuingFacilityCodes.contains(facilityType) &&
            historicalStudents.stream().anyMatch(stu -> stu.getFte().compareTo(BigDecimal.ZERO) > 0)) {
      log.debug("ProgramEligibilityBaseRule - SpecialEducationProgramsRule: CollectionTypeCodes - {}, schoolCategory - {}, for sdcSchoolCollectionStudentID :: {}", student.getIsSchoolAged(), student.getIsGraduated(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
      errors.add(ProgramEligibilityIssueCode.FEB_ONLINE_WITH_HISTORICAL_FUNDING);
    }

    return errors;
  }

}
