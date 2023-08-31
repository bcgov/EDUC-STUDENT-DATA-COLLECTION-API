package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class FteCalculatorUtils {

    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final RestUtils restUtils;
    private static final String START_DATE_KEY = "startDate";
    private static final String END_DATE_KEY = "endDate";

    @Autowired
    private FteCalculatorUtils(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
        this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.restUtils = restUtils;
    }

    /**
     * This method returns the LocalDateTime of the first and last day of the month of the previous collection
     * to the one of the given student
     */
    private Map<String, LocalDateTime> getPreviousCollectionStartAndEndDates(StudentRuleData studentRuleData) {
        LocalDateTime startOfCollectionDate;
        LocalDateTime endOfCollectionDate;
        //if it's a February collection, get the previous september collection
        if(studentRuleData.getCollectionTypeCode().equals(CollectionTypeCodes.FEBRUARY.getTypeCode())) {
            int previousYear = studentRuleData.getSdcSchoolCollectionStudentEntity().getCreateDate().minusYears(1).getYear();
            startOfCollectionDate = LocalDate.of(previousYear, Month.SEPTEMBER, 1).atTime(LocalTime.MIN);
            endOfCollectionDate = LocalDate.of(previousYear, Month.SEPTEMBER, 30).atTime(LocalTime.MAX);
        } else { //it's a May collection, so get the previous february collection
            int currentYear = studentRuleData.getSdcSchoolCollectionStudentEntity().getCreateDate().getYear();
            LocalDate february1st = LocalDate.of(currentYear, Month.FEBRUARY, 1);
            startOfCollectionDate = february1st.atTime(LocalTime.MIN);
            int daysInFebruary = february1st.lengthOfMonth();
            endOfCollectionDate = LocalDate.of(currentYear, Month.FEBRUARY, daysInFebruary).atTime(LocalTime.MAX);
        }
        Map<String, LocalDateTime> startAndEndOfCollectionMap = new HashMap<>();
        startAndEndOfCollectionMap.put(START_DATE_KEY, startOfCollectionDate);
        startAndEndOfCollectionMap.put(END_DATE_KEY, endOfCollectionDate);
        return startAndEndOfCollectionMap;
    }

    /**
     * Returns true if the collection is a February or May collection; otherwise it is false
     */
    public boolean isSpringCollection(StudentRuleData studentRuleData) {
        return StringUtils.equals(studentRuleData.getCollectionTypeCode(), CollectionTypeCodes.FEBRUARY.getTypeCode()) || StringUtils.equals(studentRuleData.getCollectionTypeCode(), CollectionTypeCodes.MAY.getTypeCode());
    }

    /**
     * Returns true if the given student of a spring collection is a public online learning or continuing ed school
     * (of a certain grade) was reported in the previous collection for the same district
     */
    public boolean studentPreviouslyReportedInDistrict(StudentRuleData studentRuleData) {
        var school = studentRuleData.getSchool();
        var isPublicOnlineOrContEdSchool = (school.getSchoolCategoryCode().equals(SchoolCategoryCodes.PUBLIC.getCode()) &&
                (school.getFacilityTypeCode().equals(FacilityTypeCodes.DIST_LEARN.getCode()) || school.getFacilityTypeCode().equals(FacilityTypeCodes.DISTONLINE.getCode()))) ||
                school.getFacilityTypeCode().equals(FacilityTypeCodes.CONT_ED.getCode());
        var isStudentInDistrictFundedGrade = SchoolGradeCodes.getDistrictFundingGrades().contains(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode());

        if(isSpringCollection(studentRuleData) && isPublicOnlineOrContEdSchool && isStudentInDistrictFundedGrade && StringUtils.isNotBlank(school.getDistrictId())) {
            var startAndEndDateOfCollectionMap = getPreviousCollectionStartAndEndDates(studentRuleData);
            var startOfCollectionDate = startAndEndDateOfCollectionMap.get(START_DATE_KEY);
            var endOfCollectionDate = startAndEndDateOfCollectionMap.get(END_DATE_KEY);
            var previousCollections = sdcSchoolCollectionRepository.findAllByDistrictIDAndCreateDateBetween(UUID.fromString(school.getDistrictId()), startOfCollectionDate, endOfCollectionDate);
            return sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId(), previousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList()) > 0;
        }
        return false;
    }

    /**
     * Returns true if the given student of a spring collection is an independent online learning school
     * (of a certain grade) was reported in the previous collection for the same authority
     */
    public boolean studentPreviouslyReportedInIndependentAuthority(StudentRuleData studentRuleData) {
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var school = studentRuleData.getSchool();
        var isIndependentOnlineSchool = school != null && StringUtils.equals(school.getSchoolCategoryCode(), SchoolCategoryCodes.INDEPEND.getCode()) && StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DIST_LEARN.getCode());
        var isStudentInDistrictFundedGrade = SchoolGradeCodes.getDistrictFundingGrades().contains(student.getEnrolledGradeCode());

        if(isSpringCollection(studentRuleData) && isIndependentOnlineSchool && isStudentInDistrictFundedGrade) {
            var startAndEndDateOfCollectionMap = getPreviousCollectionStartAndEndDates(studentRuleData);
            var startOfCollectionDate = startAndEndDateOfCollectionMap.get(START_DATE_KEY);
            var endOfCollectionDate = startAndEndDateOfCollectionMap.get(END_DATE_KEY);
            if(StringUtils.isNotBlank(school.getIndependentAuthorityId())) {
                var schoolIDs = restUtils.getSchoolIDsByIndependentAuthorityID(school.getIndependentAuthorityId());
                if (schoolIDs.isPresent()) {
                    var previousCollections = sdcSchoolCollectionRepository.findAllBySchoolIDInAndCreateDateBetween(schoolIDs.get(), startOfCollectionDate, endOfCollectionDate);
                    return sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(student.getAssignedStudentId(), previousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList()) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the given student (in a correct grade) is part of a spring (Feb or May) collection reported
     * by an online school and the student was reported as an HS student in the previous collection
     */
    public boolean homeSchoolStudentIsNowOnlineKto9Student(StudentRuleData studentRuleData) {
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var studentReportedByOnlineSchool = studentRuleData.getSchool().getFacilityTypeCode().equals(FacilityTypeCodes.DIST_LEARN.getCode()) || studentRuleData.getSchool().getFacilityTypeCode().equals(FacilityTypeCodes.DISTONLINE.getCode());
        var isStudentGradeKToNine = SchoolGradeCodes.getKToNineGrades().contains(student.getEnrolledGradeCode());

        if(isSpringCollection(studentRuleData) && studentReportedByOnlineSchool && isStudentGradeKToNine) {
            var startAndEndDateOfPreviousCollection = getPreviousCollectionStartAndEndDates(studentRuleData);
            var startDate = startAndEndDateOfPreviousCollection.get(START_DATE_KEY);
            var endDate = startAndEndDateOfPreviousCollection.get(END_DATE_KEY);
            //Check if student was in previous collection as HS student
            var count = sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndEnrolledGradeCodeAndCreateDateBetween(student.getAssignedStudentId(), SchoolGradeCodes.HOMESCHOOL.getCode(), startDate, endDate);
            return count > 0;
        }
        return false;
    }

    /**
     * Returns true if the given student (of the correct grade) is reported by a provincial or district online school
     * with zero courses and the student has not been reported with courses > 0 for the last two years
     */
    public boolean noCoursesForStudentInLastTwoYears(StudentRuleData studentRuleData) {
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var school = studentRuleData.getSchool();
        var isEightPlusGradeCode = SchoolGradeCodes.get8PlusGrades().contains(student.getEnrolledGradeCode());
        var reportedByOnlineSchoolWithNoCourses = (StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DIST_LEARN.getCode()) || StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DISTONLINE.getCode())) && (StringUtils.isBlank(student.getNumberOfCourses()) || StringUtils.equals(student.getNumberOfCourses(), "0"));
        boolean isSchoolAged = student.getIsSchoolAged() == Boolean.TRUE;

        if (isSchoolAged && isEightPlusGradeCode && reportedByOnlineSchoolWithNoCourses) {
            var startOfMonth = student.getCreateDate().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);
            var lastTwoYearsOfCollections = sdcSchoolCollectionRepository.findAllBySchoolIDAndCreateDateBetween(UUID.fromString(school.getSchoolId()), startOfMonth.minusYears(2), startOfMonth);
            return lastTwoYearsOfCollections.isEmpty() || sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(student.getAssignedStudentId(), lastTwoYearsOfCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList(), "0") == 0;
        }
        return false;
    }
}
