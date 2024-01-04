package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@Order(5)
public class InactiveOnlineMinorStudentsRule implements ProgramEligibilityBaseRule {

  private static final DecimalFormat df = new DecimalFormat("00.00");
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  public InactiveOnlineMinorStudentsRule(
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository,
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository
  ) {
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
  }

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> list) {
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - InactiveOnlineMinorStudentsRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
    String facilityType = studentRuleData.getSchool().getFacilityTypeCode();
    String gradeCode = student.getEnrolledGradeCode();
    String numberOfCoursesString = student.getNumberOfCourses();

    boolean isOnlineSchool = StringUtils.equals(facilityType, FacilityTypeCodes.DIST_LEARN.getCode()) || StringUtils.equals(facilityType, FacilityTypeCodes.DISTONLINE.getCode());
    boolean isInRelevantGrade = SchoolGradeCodes.get8PlusGrades().contains(gradeCode);
    boolean has0Courses = StringUtils.isNotEmpty(numberOfCoursesString) && Double.parseDouble(df.format(Double.valueOf(numberOfCoursesString))) == 0;
    Boolean isSchoolAged = student.getIsSchoolAged();

    log.debug("In shouldExecute of ProgramEligibilityBaseRule - InactiveOnlineMinorStudentsRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
            isOnlineSchool && isInRelevantGrade && has0Courses && Boolean.TRUE.equals(isSchoolAged),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
    return isOnlineSchool && isInRelevantGrade && has0Courses && Boolean.TRUE.equals(isSchoolAged);
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    log.debug("In executeValidation of ProgramEligibilityBaseRule - InactiveOnlineMinorStudentsRule for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
    School school = studentRuleData.getSchool();

    List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(UUID.fromString(school.getSchoolId()),student.getSdcSchoolCollection().getSdcSchoolCollectionID());
    log.debug("In executeValidation of ProgramEligibilityBaseRule - InactiveOnlineMinorStudentsRule: No of collections - {},  for school :: {}" ,lastTwoYearsOfCollections.size(), school.getSchoolId());

    if (lastTwoYearsOfCollections.isEmpty() || student.getAssignedStudentId() == null
    || sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(student.getAssignedStudentId(), lastTwoYearsOfCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList(), "0") == 0) {
      errors.add(ProgramEligibilityIssueCode.INACTIVE_SCHOOL_AGE);
    }

    return errors;
  }

}
