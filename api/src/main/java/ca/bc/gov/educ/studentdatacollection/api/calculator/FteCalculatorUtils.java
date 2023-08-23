package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.helpers.BooleanString;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
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
    private Map<String, LocalDateTime> getPreviousCollectionStartAndEndDates(SdcStudentSagaData sdcStudentSagaData) {
        LocalDateTime startOfCollectionDate;
        LocalDateTime endOfCollectionDate;
        //if it's a February collection, get the previous september collection
        if(sdcStudentSagaData.getCollectionTypeCode().equals(CollectionTypeCodes.FEBRUARY.getTypeCode())) {
            int previousYear = LocalDateTime.parse(sdcStudentSagaData.getSdcSchoolCollectionStudent().getCreateDate()).minusYears(1).getYear();
            startOfCollectionDate = LocalDate.of(previousYear, Month.SEPTEMBER, 1).atTime(LocalTime.MIN);
            endOfCollectionDate = LocalDate.of(previousYear, Month.SEPTEMBER, 30).atTime(LocalTime.MAX);
        } else { //it's a May collection, so get the previous february collection
            int currentYear = LocalDateTime.parse(sdcStudentSagaData.getSdcSchoolCollectionStudent().getCreateDate()).getYear();
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
    public boolean isSpringCollection(SdcStudentSagaData sdcStudentSagaData) {
        return StringUtils.equals(sdcStudentSagaData.getCollectionTypeCode(), CollectionTypeCodes.FEBRUARY.getTypeCode()) || StringUtils.equals(sdcStudentSagaData.getCollectionTypeCode(), CollectionTypeCodes.MAY.getTypeCode());
    }

    /**
     * Returns true if the given student of a spring collection is a public online learning or continuing ed school
     * (of a certain grade) was reported in the previous collection for the same district
     */
    public boolean studentPreviouslyReportedInDistrict(SdcStudentSagaData sdcStudentSagaData) {
        var school = sdcStudentSagaData.getSchool();
        var isPublicOnlineOrContEdSchool = (school.getSchoolCategoryCode().equals(SchoolCategoryCodes.PUBLIC.getCode()) &&
                (school.getFacilityTypeCode().equals(FacilityTypeCodes.DIST_LEARN.getCode()) || school.getFacilityTypeCode().equals(FacilityTypeCodes.DISTONLINE.getCode()))) ||
                school.getFacilityTypeCode().equals(FacilityTypeCodes.CONT_ED.getCode());
        var isStudentInDistrictFundedGrade = SchoolGradeCodes.getDistrictFundingGrades().contains(sdcStudentSagaData.getSdcSchoolCollectionStudent().getEnrolledGradeCode());

        if(isSpringCollection(sdcStudentSagaData) && isPublicOnlineOrContEdSchool && isStudentInDistrictFundedGrade) {
            var startAndEndDateOfCollectionMap = getPreviousCollectionStartAndEndDates(sdcStudentSagaData);
            var startOfCollectionDate = startAndEndDateOfCollectionMap.get(START_DATE_KEY);
            var endOfCollectionDate = startAndEndDateOfCollectionMap.get(END_DATE_KEY);
            if(StringUtils.isNotBlank(school.getDistrictId())) {
                var previousCollections = sdcSchoolCollectionRepository.findAllByDistrictIDAndCreateDateBetween(UUID.fromString(school.getDistrictId()), startOfCollectionDate, endOfCollectionDate);
                return previousCollections.isPresent() && sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollectionIDIn(UUID.fromString(sdcStudentSagaData.getSdcSchoolCollectionStudent().getAssignedStudentId()), previousCollections.get().stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList()) > 0;
            }
        }
        return false;
    }

    /**
     * Returns true if the given student of a spring collection is an independent online learning school
     * (of a certain grade) was reported in the previous collection for the same authority
     */
    public boolean studentPreviouslyReportedInIndependentAuthority(SdcStudentSagaData sdcStudentSagaData) {
        var student = sdcStudentSagaData.getSdcSchoolCollectionStudent();
        var school = sdcStudentSagaData.getSchool();
        var isIndependentOnlineSchool = school != null && StringUtils.equals(school.getSchoolCategoryCode(), SchoolCategoryCodes.INDEPEND.getCode()) && StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DIST_LEARN.getCode());
        var isStudentInDistrictFundedGrade = SchoolGradeCodes.getDistrictFundingGrades().contains(student.getEnrolledGradeCode());

        if(isSpringCollection(sdcStudentSagaData) && isIndependentOnlineSchool && isStudentInDistrictFundedGrade) {
            var startAndEndDateOfCollectionMap = getPreviousCollectionStartAndEndDates(sdcStudentSagaData);
            var startOfCollectionDate = startAndEndDateOfCollectionMap.get(START_DATE_KEY);
            var endOfCollectionDate = startAndEndDateOfCollectionMap.get(END_DATE_KEY);
            if(StringUtils.isNotBlank(school.getIndependentAuthorityId())) {
                var schoolIDs = restUtils.getSchoolIDsByIndependentAuthorityID(school.getIndependentAuthorityId());
                if (schoolIDs.isPresent()) {
                    var previousCollections = sdcSchoolCollectionRepository.findAllBySchoolIDInAndCreateDateBetween(schoolIDs.get(), startOfCollectionDate, endOfCollectionDate);
                    return previousCollections.isPresent() && sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollectionIDIn(UUID.fromString(student.getAssignedStudentId()), previousCollections.get().stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList()) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the given student (in a correct grade) is part of a spring (Feb or May) collection reported
     * by an online school and the student was reported as an HS student in the previous collection
     */
    public boolean homeSchoolStudentIsNowOnlineKto9Student(SdcStudentSagaData sdcStudentSagaData) {
        var student = sdcStudentSagaData.getSdcSchoolCollectionStudent();
        var studentReportedByOnlineSchool = sdcStudentSagaData.getSchool().getFacilityTypeCode().equals(FacilityTypeCodes.DIST_LEARN.getCode()) || sdcStudentSagaData.getSchool().getFacilityTypeCode().equals(FacilityTypeCodes.DISTONLINE.getCode());
        var isStudentGradeKToNine = SchoolGradeCodes.getKToNineGrades().contains(student.getEnrolledGradeCode());

        if(isSpringCollection(sdcStudentSagaData) && studentReportedByOnlineSchool && isStudentGradeKToNine) {
            var startAndEndDateOfPreviousCollection = getPreviousCollectionStartAndEndDates(sdcStudentSagaData);
            var startDate = startAndEndDateOfPreviousCollection.get(START_DATE_KEY);
            var endDate = startAndEndDateOfPreviousCollection.get(END_DATE_KEY);
            //Check if student was in previous collection as HS student
            return sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndEnrolledGradeCodeAndCreateDateBetween(UUID.fromString(student.getAssignedStudentId()), SchoolGradeCodes.HOMESCHOOL.getCode(), startDate, endDate) > 0;
        }
        return false;
    }

    /**
     * Returns true if the given student (of the correct grade) is reported by a provincial or district online school
     * with zero courses and the student has not been reported with courses > 0 for the last two years
     */
    public boolean noCoursesForStudentInLastTwoYears(SdcStudentSagaData sdcStudentSagaData) {
        var student = sdcStudentSagaData.getSdcSchoolCollectionStudent();
        var school = sdcStudentSagaData.getSchool();
        var isEightPlusGradeCode = SchoolGradeCodes.get8PlusGrades().contains(student.getEnrolledGradeCode());
        var reportedByOnlineSchoolWithNoCourses = (StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DIST_LEARN.getCode()) || StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DISTONLINE.getCode())) && (StringUtils.isBlank(student.getNumberOfCourses()) || StringUtils.equals(student.getNumberOfCourses(), "0"));
        boolean isSchoolAged = BooleanString.areEqual(student.getIsSchoolAged(), Boolean.TRUE);

        if (isSchoolAged && isEightPlusGradeCode && reportedByOnlineSchoolWithNoCourses) {
            var startOfMonth = LocalDateTime.parse(student.getCreateDate()).with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);
            var lastTwoYearsOfCollections = sdcSchoolCollectionRepository.findAllBySchoolIDAndCreateDateBetween(UUID.fromString(school.getSchoolId()), startOfMonth.minusYears(2), startOfMonth);
            return lastTwoYearsOfCollections.isEmpty() || sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(UUID.fromString(student.getAssignedStudentId()), lastTwoYearsOfCollections.get().stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList(), "0") == 0;
        }
        return false;
    }
}
