package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
        validationRulesService.setupMergedStudentIdValues(studentRuleData);
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
        validationRulesService.setupMergedStudentIdValues(studentRuleData);
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
        validationRulesService.setupMergedStudentIdValues(studentRuleData);
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
                var collectionIds = previousCollections.stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList();
                var count = sdcSchoolCollectionStudentRepository.countAllByAssignedStudentIdInAndEnrolledGradeCodeAndSdcSchoolCollectionStudentStatusCodeIsNotAndSdcSchoolCollection_SdcSchoolCollectionIDIn(studentRuleData.getHistoricStudentIds(), SchoolGradeCodes.HOMESCHOOL.getCode(), SdcSchoolStudentStatus.DELETED.getCode(), collectionIds);
                return count > 0;
            }
        }
        return false;
    }

    /**
     * Returns true if the given school aged student (of the correct grade) is reported by a provincial or district online school
     * with zero courses and the student has not been reported with courses > 0 for the last two years
     */
    public boolean noCoursesForSchoolAgedStudentInLastTwoYears(StudentRuleData studentRuleData) {
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var school = studentRuleData.getSchool();
        var isEightPlusGradeCode = SchoolGradeCodes.get8PlusGradesNoGA().contains(student.getEnrolledGradeCode());
        var reportedByOnlineOrContEdSchool = StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DIST_LEARN.getCode()) || StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DISTONLINE.getCode());
        var zeroCourses = TransformUtil.parseNumberOfCourses(student.getNumberOfCourses(), student.getSdcSchoolCollection().getSdcSchoolCollectionID()) == 0;
        boolean isSchoolAged = Boolean.TRUE.equals(student.getIsSchoolAged());

        if (isSchoolAged && isEightPlusGradeCode && reportedByOnlineOrContEdSchool && zeroCourses) {
            validationRulesService.setupMergedStudentIdValues(studentRuleData);
            if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() == null) {
                return true;
            }
            var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
            var lastTwoYearsOfStudentRecords = sdcSchoolCollectionStudentRepository.findLastTwoYearsOfStudentRecordsWithinSchool(studentRuleData.getHistoricStudentIds(), UUID.fromString(school.getSchoolId()), currentSnapshotDate);

            return lastTwoYearsOfStudentRecords.isEmpty() || !hasCoursesInLastTwoYears(lastTwoYearsOfStudentRecords);
        }
        return false;
    }

    /**
     * Returns true if the given adult student (of the correct grade) is reported by a provincial or district online school
     * with zero courses and the student has not been reported with courses > 0 for the last two years
     */
    public boolean noCoursesForAdultStudentInLastTwoYears(StudentRuleData studentRuleData) {
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var school = studentRuleData.getSchool();
        var isAllowedAdultGradeCode = SchoolGradeCodes.getAllowedAdultGrades().contains(student.getEnrolledGradeCode());
        var reportedByOnlineSchool = StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DIST_LEARN.getCode()) || StringUtils.equals(school.getFacilityTypeCode(), FacilityTypeCodes.DISTONLINE.getCode());
        var zeroCourses = TransformUtil.parseNumberOfCourses(student.getNumberOfCourses(), student.getSdcSchoolCollection().getSdcSchoolCollectionID()) == 0;
        boolean isAdult = Boolean.TRUE.equals(student.getIsAdult());

        if (isAdult && isAllowedAdultGradeCode && reportedByOnlineSchool && zeroCourses) {
            validationRulesService.setupMergedStudentIdValues(studentRuleData);
            if(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() == null) {
                return true;
            }
            var currentSnapshotDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getSnapshotDate();
            var lastTwoYearsOfStudentRecords = sdcSchoolCollectionStudentRepository.findLastTwoYearsOfStudentRecordsWithinSchool(studentRuleData.getHistoricStudentIds(), UUID.fromString(school.getSchoolId()), currentSnapshotDate);

            return lastTwoYearsOfStudentRecords.isEmpty() || !hasCoursesInLastTwoYears(lastTwoYearsOfStudentRecords);
        }
        return false;
    }

    private boolean hasCoursesInLastTwoYears(List<SdcSchoolCollectionStudentEntity> lastTwoYearsOfStudentRecords){
        for(SdcSchoolCollectionStudentEntity studentEntity : lastTwoYearsOfStudentRecords){
            try {
                var noOfCourses = Double.parseDouble(studentEntity.getNumberOfCourses()) / 100;
                if(noOfCourses > 0){
                    return true;
                }
            } catch (NumberFormatException e) {
                //this is ok
            }
        }
        return false;
    }

    public boolean includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeNotOnline(StudentRuleData studentRuleData) {
        // non-zero fte is checked in query
        validationRulesService.setupMergedStudentIdValues(studentRuleData);
        List<SdcSchoolCollectionStudentEntity> historicalCollections = sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(UUID.fromString(studentRuleData.getSchool().getDistrictId()), studentRuleData.getHistoricStudentIds(), "3");

        for (SdcSchoolCollectionStudentEntity studentEntity : historicalCollections) {
            String schoolId = studentEntity.getSdcSchoolCollection().getSchoolID().toString();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolId);
            if (school.isPresent() && FacilityTypeCodes.getOnlineFacilityTypeCodes().stream().noneMatch(code -> code.equals(school.get().getFacilityTypeCode()))) {
                    return true;
                }

        }
        return false;
    }

    public boolean includedInCollectionThisSchoolYearForDistrictWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(StudentRuleData studentRuleData) {
        // non-zero fte is checked in query
        validationRulesService.setupMergedStudentIdValues(studentRuleData);
        List<SdcSchoolCollectionStudentEntity> historicalCollections = sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(UUID.fromString(studentRuleData.getSchool().getDistrictId()), studentRuleData.getHistoricStudentIds(), "3");

        for (SdcSchoolCollectionStudentEntity studentEntity : historicalCollections) {
            String schoolId = studentEntity.getSdcSchoolCollection().getSchoolID().toString();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolId);
            if (school.isPresent() && FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(school.get().getFacilityTypeCode()) && SchoolGradeCodes.getKToNineGrades().contains(studentEntity.getEnrolledGradeCode())) {
                    return true;
                }

        }
        return false;
    }

    public boolean includedInCollectionThisSchoolYearForAuthWithNonZeroFteWithSchoolTypeNotOnline(StudentRuleData studentRuleData) {
        // non-zero fte is checked in query
        validationRulesService.setupMergedStudentIdValues(studentRuleData);
        List<SchoolTombstone> allSchools = this.restUtils.getAllSchoolTombstones();
        List<UUID> independentSchoolIDsWithSameAuthorityID = allSchools.stream()
                .filter(school -> school.getIndependentAuthorityId() != null && school.getIndependentAuthorityId().equals(studentRuleData.getSchool().getIndependentAuthorityId()))
                .map(school -> UUID.fromString(school.getSchoolId()))
                .toList();
        List<SdcSchoolCollectionStudentEntity> historicalCollections = sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameAuthority(independentSchoolIDsWithSameAuthorityID, studentRuleData.getHistoricStudentIds(), "3");

        for (SdcSchoolCollectionStudentEntity studentEntity : historicalCollections) {
            String schoolId = studentEntity.getSdcSchoolCollection().getSchoolID().toString();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolId);
            if (school.isPresent() && FacilityTypeCodes.getOnlineFacilityTypeCodes().stream().noneMatch(code -> code.equals(school.get().getFacilityTypeCode()))) {
                return true;
            }

        }
        return false;
    }

    public boolean includedInCollectionThisSchoolYearForAuthWithNonZeroFteWithSchoolTypeOnlineInGradeKto9(StudentRuleData studentRuleData) {
        // non-zero fte is checked in query
        validationRulesService.setupMergedStudentIdValues(studentRuleData);
        List<SchoolTombstone> allSchools = this.restUtils.getAllSchoolTombstones();
        List<UUID> independentSchoolIDsWithSameAuthorityID = allSchools.stream()
                .filter(school -> school.getIndependentAuthorityId() != null && school.getIndependentAuthorityId().equals(studentRuleData.getSchool().getIndependentAuthorityId()))
                .map(school -> UUID.fromString(school.getSchoolId()))
                .toList();
        List<SdcSchoolCollectionStudentEntity> historicalCollections = sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalWithInSameAuthority(independentSchoolIDsWithSameAuthorityID, studentRuleData.getHistoricStudentIds(), "3");

        for (SdcSchoolCollectionStudentEntity studentEntity : historicalCollections) {
            String schoolId = studentEntity.getSdcSchoolCollection().getSchoolID().toString();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolId);
            if (school.isPresent() && FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(school.get().getFacilityTypeCode()) && SchoolGradeCodes.getKToNineGrades().contains(studentEntity.getEnrolledGradeCode())) {
                return true;
            }

        }
        return false;
    }

    public boolean reportedInOnlineSchoolInAnyPreviousCollectionThisSchoolYear(StudentRuleData studentRuleData) {
        validationRulesService.setupMergedStudentIdValues(studentRuleData);
        List<SdcSchoolCollectionStudentEntity> historicalCollections = sdcSchoolCollectionStudentRepository.findStudentInCurrentFiscalInAllDistrict(studentRuleData.getHistoricStudentIds(), "3");
        historicalCollections.add(studentRuleData.getSdcSchoolCollectionStudentEntity());

        for (SdcSchoolCollectionStudentEntity studentEntity : historicalCollections) {
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(studentEntity.getSdcSchoolCollection().getSchoolID().toString());
            if (school.isPresent() && FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(school.get().getFacilityTypeCode())) {
                return true;
            }
        }
        return false;
    }

    public boolean reportedInOnlineSchoolInCurrentCollection(StudentRuleData studentRuleData) {
        List<SdcSchoolCollectionStudentEntity> currentCollections = sdcSchoolCollectionStudentRepository.findStudentInCurrentCollectionInAllDistrict(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getCollectionID(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getSdcSchoolCollectionID());

        for (SdcSchoolCollectionStudentEntity studentEntity : currentCollections) {
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(studentEntity.getSdcSchoolCollection().getSchoolID().toString());
            if (school.isPresent() && FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(school.get().getFacilityTypeCode())) {
                return true;
            }
        }
        return false;
    }

    public boolean reportedInOtherDistrictsInPreviousCollectionThisSchoolYearInGrade8Or9WithNonZeroFte(StudentRuleData studentRuleData) {
        validationRulesService.setupMergedStudentIdValues(studentRuleData);
        return validationRulesService.studentExistsInCurrentFiscalInGrade8Or9(studentRuleData);
    }

    private LocalDate getFiscalDateFromCurrentSnapshot(LocalDate currentSnapshotDate){
        return currentSnapshotDate.minusYears(1).withMonth(9).withDayOfMonth(1);
    }
}
