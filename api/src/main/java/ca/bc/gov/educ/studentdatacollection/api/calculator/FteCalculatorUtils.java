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
import java.util.List;
import java.util.UUID;

@Component
public class FteCalculatorUtils {

    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final RestUtils restUtils;

    @Autowired
    private FteCalculatorUtils(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
        this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.restUtils = restUtils;
    }

    public static String getCollectionTypeCode(StudentRuleData studentRuleData){
        return studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getCollectionTypeCode();
    }

    /**
     * Returns true if the collection is a February or May collection; otherwise it is false
     */
    public boolean isSpringCollection(StudentRuleData studentRuleData) {
        var collectionTypeCode = getCollectionTypeCode(studentRuleData);
        return StringUtils.equals(collectionTypeCode, CollectionTypeCodes.FEBRUARY.getTypeCode()) || StringUtils.equals(collectionTypeCode, CollectionTypeCodes.MAY.getTypeCode());
    }

    /**
     * Returns true if the given student of a spring collection is a public online learning or continuing ed school
     * (of a certain grade) was reported in the previous collection for the same district
     */
    public boolean studentPreviouslyReportedInDistrict(StudentRuleData studentRuleData) {
        if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() == null) {
            return false;
        }
        var school = studentRuleData.getSchool();
        var isPublicOnlineOrContEdSchool = (school.getSchoolCategoryCode().equals(SchoolCategoryCodes.PUBLIC.getCode()) &&
                (school.getFacilityTypeCode().equals(FacilityTypeCodes.DIST_LEARN.getCode()) || school.getFacilityTypeCode().equals(FacilityTypeCodes.DISTONLINE.getCode()))) ||
                school.getFacilityTypeCode().equals(FacilityTypeCodes.CONT_ED.getCode());
        var isStudentInDistrictFundedGrade = SchoolGradeCodes.getDistrictFundingGrades().contains(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode());

        if(isSpringCollection(studentRuleData) && isPublicOnlineOrContEdSchool && isStudentInDistrictFundedGrade && StringUtils.isNotBlank(school.getDistrictId())) {
            var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
            var fiscalSnapshotDate = getFiscalDateFromCurrentSnapshot(currentSnapshotDate);
            var previousCollections = sdcSchoolCollectionRepository.findAllCollectionsForDistrictForFiscalYearToCurrentCollection(UUID.fromString(school.getDistrictId()), fiscalSnapshotDate, currentSnapshotDate);
            return sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId(), previousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList()) > 0;
        }
        return false;
    }

    /**
     * Returns true if the given student of a spring collection is an independent online learning school
     * (of a certain grade) was reported in the previous collection for the same authority
     */
    public boolean studentPreviouslyReportedInIndependentAuthority(StudentRuleData studentRuleData) {
        if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() == null) {
            return false;
        }
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var school = studentRuleData.getSchool();
        var isIndependentOnlineSchool = school != null && StringUtils.equals(school.getSchoolCategoryCode(), SchoolCategoryCodes.INDEPEND.getCode()) && StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DIST_LEARN.getCode());
        var isStudentInDistrictFundedGrade = SchoolGradeCodes.getDistrictFundingGrades().contains(student.getEnrolledGradeCode());

        if(isSpringCollection(studentRuleData) && isIndependentOnlineSchool && isStudentInDistrictFundedGrade && (StringUtils.isNotBlank(school.getIndependentAuthorityId()))) {
            var schoolIDs = restUtils.getSchoolIDsByIndependentAuthorityID(school.getIndependentAuthorityId());
            if (schoolIDs.isPresent()) {
                var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
                var fiscalSnapshotDate = getFiscalDateFromCurrentSnapshot(currentSnapshotDate);
                var previousCollections = sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(schoolIDs.get(), fiscalSnapshotDate, currentSnapshotDate);
                return sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(student.getAssignedStudentId(), previousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList()) > 0;
            }

        }
        return false;
    }

    /**
     * Returns true if the given student (in a correct grade) is part of a spring (Feb or May) collection reported
     * by an online school and the student was reported as an HS student in the previous collection
     */
    public boolean homeSchoolStudentIsNowOnlineKto9Student(StudentRuleData studentRuleData) {
        if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() == null) {
            return false;
        }
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var studentReportedByOnlineSchool = studentRuleData.getSchool().getFacilityTypeCode().equals(FacilityTypeCodes.DIST_LEARN.getCode()) || studentRuleData.getSchool().getFacilityTypeCode().equals(FacilityTypeCodes.DISTONLINE.getCode());
        var isStudentGradeKToNine = SchoolGradeCodes.getKToNineGrades().contains(student.getEnrolledGradeCode());

        if(isSpringCollection(studentRuleData) && studentReportedByOnlineSchool && isStudentGradeKToNine) {
            var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
            var fiscalSnapshotDate = getFiscalDateFromCurrentSnapshot(currentSnapshotDate);
            var previousCollections = sdcSchoolCollectionRepository.findAllCollectionsForSchoolForFiscalYearToCurrentCollection(UUID.fromString(studentRuleData.getSchool().getSchoolId()), fiscalSnapshotDate, currentSnapshotDate);
            var collectionIds = previousCollections.stream().map(collection -> collection.getSdcSchoolCollectionID()).toList();
            var count = sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(student.getAssignedStudentId(), SchoolGradeCodes.HOMESCHOOL.getCode(), collectionIds);
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
        var reportedByOnlineSchoolWithNoCourses = (StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DIST_LEARN.getCode()) || StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DISTONLINE.getCode())) &&
                (StringUtils.isBlank(student.getNumberOfCourses()) || StringUtils.equals(student.getNumberOfCourses(), "0000"));
        boolean isSchoolAged = Boolean.TRUE.equals(student.getIsSchoolAged());

        if (isSchoolAged && isEightPlusGradeCode && reportedByOnlineSchoolWithNoCourses) {
            if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() == null) {
                return true;
            }

            var lastTwoYearsOfCollections = sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(UUID.fromString(school.getSchoolId()), student.getSdcSchoolCollection().getSdcSchoolCollectionID());
            return lastTwoYearsOfCollections.isEmpty() || sdcSchoolCollectionStudentRepository.countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(student.getAssignedStudentId(), lastTwoYearsOfCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList(), "0") == 0;
        }
        return false;
    }

    private LocalDate getFiscalDateFromCurrentSnapshot(LocalDate currentSnapshotDate){
        return currentSnapshotDate.minusYears(1).withMonth(9).withDayOfMonth(1);
    }
}
