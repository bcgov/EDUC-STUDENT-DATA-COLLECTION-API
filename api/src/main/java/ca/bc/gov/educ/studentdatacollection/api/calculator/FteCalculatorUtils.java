package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class FteCalculatorUtils {

    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final RestUtils restUtils;
    private final ValidationRulesService validationRulesService;

    @Autowired
    private FteCalculatorUtils(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, ValidationRulesService validationRulesService) {
        this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.restUtils = restUtils;
        this.validationRulesService = validationRulesService;
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
     * Returns true if the collection is a February or May collection; otherwise it is false
     */
    public boolean isFebruaryCollection(StudentRuleData studentRuleData) {
        var collectionTypeCode = getCollectionTypeCode(studentRuleData);
        return StringUtils.equals(collectionTypeCode, CollectionTypeCodes.FEBRUARY.getTypeCode());
    }

    /**
     * Returns true if the collection is a May collection; otherwise it is false
     */
    public boolean isMayCollection(StudentRuleData studentRuleData) {
        var collectionTypeCode = getCollectionTypeCode(studentRuleData);
        return StringUtils.equals(collectionTypeCode, CollectionTypeCodes.MAY.getTypeCode());
    }


    /**
     * Returns true if the given student of a spring collection is a public online learning or continuing ed school (of a certain grade)
     * 1. was reported in the previous September collection for the same district, not in HS
     * 2. was reported in the previous February collection for the same district, not in HS, and received a non zero-FTE
     */
    public boolean studentPreviouslyReportedInDistrict(StudentRuleData studentRuleData) {
        if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() == null) {
            return false;
        }
        var school = studentRuleData.getSchool();
        var isPublicOnlineOrContEdSchool = (school.getSchoolCategoryCode().equals(SchoolCategoryCodes.PUBLIC.getCode()) &&
                FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(school.getFacilityTypeCode())) ||
                school.getFacilityTypeCode().equals(FacilityTypeCodes.CONT_ED.getCode());
        var isStudentInDistrictFundedGrade = SchoolGradeCodes.getDistrictFundingGrades().contains(studentRuleData.getSdcSchoolCollectionStudentEntity().getEnrolledGradeCode());
        var assignedStudentId = studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId();

        long countAllByAssignedStudentIdAndSdcSchoolCollectionSdcSchoolCollectionIDIn = 0;
        var isSpringCollection = isSpringCollection(studentRuleData);
        validationRulesService.setupMergedStudentIdValues(studentRuleData);
        log.debug("StudentPreviouslyReportedInDistrict: isSpringCollection: " + isSpringCollection + " :: isPublicOnlineOrContEdSchool: " + isPublicOnlineOrContEdSchool + " :: isStudentInDistrictFundedGrade: " + isStudentInDistrictFundedGrade + " :: districtId: " + StringUtils.isNotBlank(school.getDistrictId()));
        if(isSpringCollection && isPublicOnlineOrContEdSchool && isStudentInDistrictFundedGrade && StringUtils.isNotBlank(school.getDistrictId())) {
            var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
            var fiscalSnapshotDate = getFiscalDateFromCurrentSnapshot(currentSnapshotDate);
            log.debug("StudentPreviouslyReportedInDistrict springCollection: fiscalSnapshotDate: " + fiscalSnapshotDate + " :: currentSnapshotDate: " + currentSnapshotDate + " :: assignedStudentId: " + assignedStudentId);
            var previousSeptemberCollections = sdcSchoolCollectionRepository.findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(UUID.fromString(school.getDistrictId()), fiscalSnapshotDate, currentSnapshotDate);
            countAllByAssignedStudentIdAndSdcSchoolCollectionSdcSchoolCollectionIDIn += sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(studentRuleData.getHistoricStudentIds(), previousSeptemberCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList());
            log.debug("StudentPreviouslyReportedInDistrict: springCollection student count :: " + countAllByAssignedStudentIdAndSdcSchoolCollectionSdcSchoolCollectionIDIn);
        }
        var isMayCollection = isMayCollection(studentRuleData);
        log.debug("StudentPreviouslyReportedInDistrict: isMayCollection: " + isMayCollection + " :: isPublicOnlineOrContEdSchool: " + isPublicOnlineOrContEdSchool + " :: isStudentInDistrictFundedGrade: " + isStudentInDistrictFundedGrade + " :: districtId: " + StringUtils.isNotBlank(school.getDistrictId()));
        if(isMayCollection && isPublicOnlineOrContEdSchool && isStudentInDistrictFundedGrade && StringUtils.isNotBlank(school.getDistrictId())) {
            var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
            var fiscalSnapshotDate = getFiscalDateFromCurrentSnapshot(currentSnapshotDate);
            log.debug("StudentPreviouslyReportedInDistrict mayCollection: fiscalSnapshotDate: " + fiscalSnapshotDate + " :: currentSnapshotDate: " + currentSnapshotDate + " :: assignedStudentId: " + assignedStudentId);
            var previousSeptemberCollections = sdcSchoolCollectionRepository.findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(UUID.fromString(school.getDistrictId()), fiscalSnapshotDate, currentSnapshotDate);
            countAllByAssignedStudentIdAndSdcSchoolCollectionSdcSchoolCollectionIDIn += sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(studentRuleData.getHistoricStudentIds(), previousSeptemberCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList());
            log.debug("StudentPreviouslyReportedInDistrict: mayCollection student count :: " + countAllByAssignedStudentIdAndSdcSchoolCollectionSdcSchoolCollectionIDIn);
        }

        return countAllByAssignedStudentIdAndSdcSchoolCollectionSdcSchoolCollectionIDIn > 0;
    }

    /**
     * Returns true if the given student of a spring collection is an independent online learning school (of a certain grade)
     * 1. was reported in the previous September collection for the same authority, not in HS
     * 2. was reported in the previous February collection for the same authority, not in HS, and received a non-zero FTE
     */
    public boolean studentPreviouslyReportedInIndependentAuthority(StudentRuleData studentRuleData) {
        if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() == null) {
            return false;
        }
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var school = studentRuleData.getSchool();
        var isIndependentOnlineSchool = school != null && SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode()) && FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(school.getFacilityTypeCode());
        var isStudentInDistrictFundedGrade = SchoolGradeCodes.getDistrictFundingGrades().contains(student.getEnrolledGradeCode());

        long countAllByAssignedStudentIdAndSdcSchoolCollectionSdcSchoolCollectionIDIn = 0;

        if(isSpringCollection(studentRuleData) && isIndependentOnlineSchool && isStudentInDistrictFundedGrade && (StringUtils.isNotBlank(school.getIndependentAuthorityId()))) {
            var schoolIDs = restUtils.getSchoolIDsByIndependentAuthorityID(school.getIndependentAuthorityId());
            if (schoolIDs.isPresent()) {
                validationRulesService.setupMergedStudentIdValues(studentRuleData);
                var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
                var fiscalSnapshotDate = getFiscalDateFromCurrentSnapshot(currentSnapshotDate);
                //Check both Sep & Feb
                var previousCollections = sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(schoolIDs.get(), fiscalSnapshotDate, currentSnapshotDate);
                countAllByAssignedStudentIdAndSdcSchoolCollectionSdcSchoolCollectionIDIn += sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(studentRuleData.getHistoricStudentIds(), previousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList());
            }
        }

        return countAllByAssignedStudentIdAndSdcSchoolCollectionSdcSchoolCollectionIDIn > 0;
    }

    /**
     * Returns true if the given student (in a correct grade) is part of a spring (Feb or May) collection reported
     * by an online school and the student was reported as an HS student in the previous collection
     */
    public boolean homeSchoolStudentIsNowOnlineKto9StudentOrHs(StudentRuleData studentRuleData) {
        if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() == null) {
            return false;
        }
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var studentReportedByOnlineSchool = FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(studentRuleData.getSchool().getFacilityTypeCode()) ||
        studentRuleData.getSchool().getFacilityTypeCode().equals(FacilityTypeCodes.CONT_ED.getCode());
        var isStudentGradeKToNineOrHs = SchoolGradeCodes.getDistrictFundingGrades().contains(student.getEnrolledGradeCode());

        if(isSpringCollection(studentRuleData) && studentReportedByOnlineSchool && isStudentGradeKToNineOrHs) {
            var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
            var fiscalSnapshotDate = getFiscalDateFromCurrentSnapshot(currentSnapshotDate);
            List<SdcSchoolCollectionEntity> previousCollections = null;

            if (SchoolCategoryCodes.INDEPENDENTS.contains(studentRuleData.getSchool().getSchoolCategoryCode())) {
                var schoolIds = restUtils.getSchoolIDsByIndependentAuthorityID(studentRuleData.getSchool().getIndependentAuthorityId());
                if (schoolIds.isPresent()) {
                    previousCollections = sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(schoolIds.get(), fiscalSnapshotDate, currentSnapshotDate);
                }
            } else {
                previousCollections = sdcSchoolCollectionRepository.findAllCollectionsForDistrictForFiscalYearToCurrentCollection(UUID.fromString(studentRuleData.getSchool().getDistrictId()), fiscalSnapshotDate, currentSnapshotDate);
            }
            if (previousCollections != null) {
                validationRulesService.setupMergedStudentIdValues(studentRuleData);
                var collectionIds = previousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList();
                var count = sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(studentRuleData.getHistoricStudentIds(), SchoolGradeCodes.HOMESCHOOL.getCode(), collectionIds);
                return count > 0;
            }
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
                (StringUtils.isBlank(student.getNumberOfCourses()) || StringUtils.equals(student.getNumberOfCourses(), "0000") || StringUtils.equals(student.getNumberOfCourses(), "000") || StringUtils.equals(student.getNumberOfCourses(), "00") || StringUtils.equals(student.getNumberOfCourses(), "0"));
        boolean isSchoolAged = Boolean.TRUE.equals(student.getIsSchoolAged());

        if (isSchoolAged && isEightPlusGradeCode && reportedByOnlineSchoolWithNoCourses) {
            if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() == null) {
                return true;
            }
            validationRulesService.setupMergedStudentIdValues(studentRuleData);
            var lastTwoYearsOfCollections = sdcSchoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(UUID.fromString(school.getSchoolId()), student.getSdcSchoolCollection().getSdcSchoolCollectionID());
            return lastTwoYearsOfCollections.isEmpty() || sdcSchoolCollectionStudentRepository.countByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(studentRuleData.getHistoricStudentIds(), lastTwoYearsOfCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList(), "0") == 0;
        }
        return false;
    }

    public boolean includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline(StudentRuleData studentRuleData) {
        var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
        var fiscalSnapshotDate = getFiscalDateFromCurrentSnapshot(currentSnapshotDate);
        String studentDistrictId = studentRuleData.getSchool().getDistrictId();
        List<UUID> notOnlineSchoolIdsInSameDistrict = restUtils.getSchools().stream()
                .filter(school -> FacilityTypeCodes.getOnlineFacilityTypeCodes().stream().noneMatch(code -> code.equals(school.getFacilityTypeCode())))
                .filter(school -> school.getDistrictId().equals(studentDistrictId))
                .map(school -> UUID.fromString(school.getSchoolId()))
                .toList();
        var previousCollections = sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(notOnlineSchoolIdsInSameDistrict, fiscalSnapshotDate, currentSnapshotDate);
        if (previousCollections != null) {
            var collectionIds = previousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList();
            var count = sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInWithNonZeroFTE(studentRuleData.getHistoricStudentIds(), collectionIds);
            return count > 0;
        }
        return false;
    }

    public boolean includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(StudentRuleData studentRuleData) {
        var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
        var fiscalSnapshotDate = getFiscalDateFromCurrentSnapshot(currentSnapshotDate);
        String studentDistrictId = studentRuleData.getSchool().getDistrictId();
        List<UUID> onlineSchoolIdsInSameDistrict = restUtils.getSchools().stream()
                .filter(school -> FacilityTypeCodes.getOnlineFacilityTypeCodes().stream().anyMatch(code -> code.equals(school.getFacilityTypeCode())))
                .filter(school -> school.getDistrictId().equals(studentDistrictId))
                .map(school -> UUID.fromString(school.getSchoolId()))
                .toList();
        var previousCollections = sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(onlineSchoolIdsInSameDistrict, fiscalSnapshotDate, currentSnapshotDate);
        if (previousCollections != null) {
            var collectionIds = previousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList();
            var count = sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInWithNonZeroFTEAndInGradeKto9(studentRuleData.getHistoricStudentIds(), collectionIds);
            return count > 0;
        }
        return false;
    }

    public boolean reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(StudentRuleData studentRuleData) {
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
        var fiscalSnapshotDate = getFiscalDateFromCurrentSnapshot(currentSnapshotDate);
        List<UUID> onlineSchoolIds = restUtils.getSchools().stream().filter(
                school -> FacilityTypeCodes.getOnlineFacilityTypeCodes().stream().anyMatch(
                        code -> code.equals(school.getFacilityTypeCode()))).map(
                                school -> UUID.fromString(school.getSchoolId())).toList();

        var previousCollections = sdcSchoolCollectionRepository.findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(onlineSchoolIds, fiscalSnapshotDate, currentSnapshotDate);

        if (previousCollections != null) {
            var collectionIds = previousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList();
            var count = sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(student.getAssignedStudentId(), collectionIds);
            return count > 0;
        }
        return false;
    }

    private LocalDate getFiscalDateFromCurrentSnapshot(LocalDate currentSnapshotDate){
        return currentSnapshotDate.minusYears(1).withMonth(9).withDayOfMonth(1);
    }
}
