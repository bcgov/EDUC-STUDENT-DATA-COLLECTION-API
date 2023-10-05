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
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@Order(4)
public class InactiveOnlineAdultStudentsRule implements ProgramEligibilityBaseRule {

  private static final DecimalFormat df = new DecimalFormat("00.00");
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  public InactiveOnlineAdultStudentsRule(
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository,
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository
  ) {
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
  }

  @Override
  public boolean shouldExecute(StudentRuleData studentRuleData, List<ProgramEligibilityIssueCode> list) {
    log.debug("In shouldExecute of ProgramEligibilityBaseRule - InactiveOnlineAdultStudentsRule: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , studentRuleData.getCollectionTypeCode(),
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    String facilityType = studentRuleData.getSchool().getFacilityTypeCode();
    var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
    String gradeCode = student.getEnrolledGradeCode();
    String numberOfCoursesString = student.getNumberOfCourses();

    boolean isOnlineSchool = StringUtils.equals(facilityType, FacilityTypeCodes.DIST_LEARN.getCode()) || StringUtils.equals(facilityType, FacilityTypeCodes.DISTONLINE.getCode());
    boolean isInRelevantGrade = SchoolGradeCodes.get8PlusGrades().contains(gradeCode);
    boolean has0Courses = StringUtils.isNotEmpty(numberOfCoursesString) && Double.parseDouble(df.format(Double.valueOf(numberOfCoursesString))) == 0;
    boolean isAdult = Boolean.TRUE.equals(student.getIsAdult());

    log.debug("In shouldExecute of ProgramEligibilityBaseRule - InactiveOnlineAdultStudentsRule: Condition returned  - {} for sdcSchoolCollectionStudentID :: {}" ,
            isOnlineSchool && isInRelevantGrade && has0Courses && isAdult,
            studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    return isOnlineSchool && isInRelevantGrade && has0Courses && isAdult;
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(StudentRuleData studentRuleData) {
    log.debug("In executeValidation of ProgramEligibilityBaseRule - InactiveOnlineAdultStudentsRule for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    School school = studentRuleData.getSchool();
    var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
    LocalDateTime startOfMonth = student.getCreateDate()
      .with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);

    List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = sdcSchoolCollectionRepository
      .findAllBySchoolIDAndCreateDateBetween(
        UUID.fromString(school.getSchoolId()),
        startOfMonth.minusYears(2),
        startOfMonth
      );
    log.debug("In executeValidation of ProgramEligibilityBaseRule - InactiveOnlineAdultStudentsRule: No of collections - {},  for sdcSchoolCollectionStudentID :: {}" ,lastTwoYearsOfCollections.size(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

    if (lastTwoYearsOfCollections.isEmpty()
    || sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(
      student.getAssignedStudentId(),
      lastTwoYearsOfCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList(),
      "0"
    ) == 0) {
      errors.add(ProgramEligibilityIssueCode.INACTIVE_ADULT);
    }

    return errors;
  }

}
