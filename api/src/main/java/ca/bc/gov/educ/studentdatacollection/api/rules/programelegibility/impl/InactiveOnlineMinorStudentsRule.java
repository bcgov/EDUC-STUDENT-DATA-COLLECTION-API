package ca.bc.gov.educ.studentdatacollection.api.rules.programelegibility.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramEligibilityIssueCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.helpers.BooleanString;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rules.ProgramEligibilityBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
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
  public boolean shouldExecute(SdcStudentSagaData saga, List<ProgramEligibilityIssueCode> list) {
    String facilityType = saga.getSchool().getFacilityTypeCode();
    String gradeCode = saga.getSdcSchoolCollectionStudent().getEnrolledGradeCode();
    String numberOfCoursesString = saga.getSdcSchoolCollectionStudent().getNumberOfCourses();

    boolean isOnlineSchool = StringUtils.equals(facilityType, FacilityTypeCodes.DIST_LEARN.getCode()) || StringUtils.equals(facilityType, FacilityTypeCodes.DISTONLINE.getCode());
    boolean isInRelevantGrade = SchoolGradeCodes.get8PlusGrades().contains(gradeCode);
    boolean has0Courses = StringUtils.isNotEmpty(numberOfCoursesString) && Double.parseDouble(df.format(Double.valueOf(numberOfCoursesString))) == 0;
    String isSchoolAged = saga.getSdcSchoolCollectionStudent().getIsSchoolAged();

    return isOnlineSchool && isInRelevantGrade && has0Courses && BooleanString.areEqual(isSchoolAged, Boolean.TRUE);
  }

  @Override
  public List<ProgramEligibilityIssueCode> executeValidation(SdcStudentSagaData saga) {
    List<ProgramEligibilityIssueCode> errors = new ArrayList<>();
    School school = saga.getSchool();
    SdcSchoolCollectionStudent student = saga.getSdcSchoolCollectionStudent();
    LocalDateTime startOfMonth = LocalDateTime.parse(student.getCreateDate())
      .with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);

    List<SdcSchoolCollectionEntity> lastTwoYearsOfCollections = sdcSchoolCollectionRepository
      .findAllBySchoolIDAndCreateDateBetween(
        UUID.fromString(school.getSchoolId()),
        startOfMonth.minusYears(2),
        startOfMonth
      );

    if (lastTwoYearsOfCollections.isEmpty()
    || sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(
      UUID.fromString(student.getAssignedStudentId()),
      lastTwoYearsOfCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList(),
      "0"
    ) == 0) {
      errors.add(ProgramEligibilityIssueCode.INACTIVE_SCHOOL_AGE);
    }

    return errors;
  }

}
