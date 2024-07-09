package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.MonitorIndySdcSchoolCollectionQueryResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.MonitorSdcSchoolCollectionQueryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionRepository extends JpaRepository<SdcSchoolCollectionEntity, UUID> {

    @Query(value="""
            SELECT SSC FROM SdcSchoolCollectionEntity SSC, CollectionEntity C WHERE SSC.schoolID=:schoolID 
            AND C.collectionID = SSC.collectionEntity.collectionID
            AND C.openDate <= CURRENT_TIMESTAMP AND C.closeDate >= CURRENT_TIMESTAMP""")
    Optional<SdcSchoolCollectionEntity> findActiveCollectionBySchoolId(UUID schoolID);

    @Query(value = """
            SELECT SSC.*
            FROM sdc_school_collection SSC, collection C
            WHERE SSC.school_id=:schoolId
            AND C.collection_id  = ssc.collection_id
            AND C.snapshot_date >=
            (select (col.snapshot_date - INTERVAL '2' year - INTERVAL '10' day)
            from collection col, sdc_school_collection ssoc
            where col.collection_id = ssoc.collection_id
            and ssoc.sdc_school_collection_id = :sdcCollectionID
            )
            AND ssc.sdc_school_collection_id != :sdcCollectionID"""
            , nativeQuery = true)
    List<SdcSchoolCollectionEntity> findAllCollectionsForSchoolInLastTwoYears(UUID schoolId, UUID sdcCollectionID);

    @Query(value = """
            SELECT SSC.*
            FROM sdc_school_collection SSC, collection C, sdc_district_collection SSD
            WHERE SSD.district_id=:districtId
            AND SSC.sdc_district_collection_id = SSD.sdc_district_collection_id
            AND C.collection_id  = ssc.collection_id
            AND C.snapshot_date >= :fiscalSnapshotDate
            AND C.snapshot_date < :currentSnapshotDate"""
            , nativeQuery = true)
    List<SdcSchoolCollectionEntity> findAllCollectionsForDistrictForFiscalYearToCurrentCollection(UUID districtId, LocalDate fiscalSnapshotDate, LocalDate currentSnapshotDate);

    @Query(value = """
            SELECT SSC.*
            FROM sdc_school_collection SSC, collection C
            WHERE SSC.school_id in :schoolIds
            AND C.collection_id  = ssc.collection_id
            AND C.snapshot_date >= :fiscalSnapshotDate
            AND C.snapshot_date < :currentSnapshotDate"""
            , nativeQuery = true)
    List<SdcSchoolCollectionEntity> findAllCollectionsForSchoolsForFiscalYearToCurrentCollection(List<UUID> schoolIds, LocalDate fiscalSnapshotDate, LocalDate currentSnapshotDate);

    @Query(value = """
            SELECT SSC.*
            FROM sdc_school_collection SSC, collection C
            WHERE SSC.school_id in :schoolIds
            AND C.collection_id  = ssc.collection_id
            AND C.snapshot_date >= :fiscalSnapshotDate
            AND C.snapshot_date < :currentSnapshotDate
            AND C.collection_type_code = 'SEPTEMBER'
            """
            , nativeQuery = true)
    List<SdcSchoolCollectionEntity> findSeptemberCollectionsForSchoolsForFiscalYearToCurrentCollection(List<UUID> schoolIds, LocalDate fiscalSnapshotDate, LocalDate currentSnapshotDate);

    @Query(value = """
            SELECT SSC.*
            FROM sdc_school_collection SSC, collection C
            WHERE SSC.school_id in :schoolIds
            AND C.collection_id  = ssc.collection_id
            AND C.snapshot_date >= :fiscalSnapshotDate
            AND C.snapshot_date < :currentSnapshotDate
            AND C.collection_type_code = 'FEBRUARY'
            """
            , nativeQuery = true)
    List<SdcSchoolCollectionEntity> findFebruaryCollectionsForSchoolsForFiscalYearToCurrentCollection(List<UUID> schoolIds, LocalDate fiscalSnapshotDate, LocalDate currentSnapshotDate);

    @Query(value = """
            SELECT SSC.*
            FROM sdc_school_collection SSC, collection C, sdc_district_collection SSD
            WHERE SSD.district_id=:districtId
            AND SSC.sdc_district_collection_id = SSD.sdc_district_collection_id
            AND C.collection_id  = ssc.collection_id
            AND C.snapshot_date >= :fiscalSnapshotDate
            AND C.snapshot_date < :currentSnapshotDate
            AND C.collection_type_code = 'SEPTEMBER'
            """
            , nativeQuery = true)
    List<SdcSchoolCollectionEntity> findSeptemberCollectionsForDistrictForFiscalYearToCurrentCollection(UUID districtId, LocalDate fiscalSnapshotDate, LocalDate currentSnapshotDate);

    @Query(value = """
        SELECT SSC.*
        FROM sdc_school_collection SSC
        JOIN sdc_school_collection_student SSCS ON SSCS.sdc_school_collection_id = SSC.sdc_school_collection_id
        JOIN collection C ON C.collection_id = SSC.collection_id
        WHERE SSCS.assigned_student_id = :assignedStudentId
        AND C.snapshot_date < :currentSnapshotDate
        AND C.collection_status_code != 'INPROGRESS'
        """
            , nativeQuery = true)
    List<SdcSchoolCollectionEntity> findAllPreviousCollectionsForStudent(UUID assignedStudentId, LocalDate currentSnapshotDate);

    @Query(value = """
            SELECT SSC.*
            FROM sdc_school_collection SSC, collection C, sdc_district_collection SSD
            WHERE SSD.district_id=:districtId
            AND SSC.sdc_district_collection_id = SSD.sdc_district_collection_id
            AND C.collection_id  = ssc.collection_id
            AND C.snapshot_date >= :fiscalSnapshotDate
            AND C.snapshot_date < :currentSnapshotDate
            AND C.collection_type_code = 'FEBRUARY'
            """
            , nativeQuery = true)
    List<SdcSchoolCollectionEntity> findFebruaryCollectionsForDistrictForFiscalYearToCurrentCollection(UUID districtId, LocalDate fiscalSnapshotDate, LocalDate currentSnapshotDate);

    @Query(value="""
            SELECT SSC FROM SdcSchoolCollectionEntity SSC, CollectionEntity C 
            WHERE SSC.schoolID=:schoolID 
            AND C.collectionID = SSC.collectionEntity.collectionID
            AND SSC.sdcSchoolCollectionID != :currentSdcCollectionID
            AND C.collectionTypeCode = :collectionTypeCode
            ORDER BY C.snapshotDate desc
            LIMIT 1""")
    Optional<SdcSchoolCollectionEntity> findLastCollectionByType(UUID schoolID, String collectionTypeCode, UUID currentSdcCollectionID);

    List<SdcSchoolCollectionEntity> findAllBySchoolID(UUID schoolID);
    Optional<SdcSchoolCollectionEntity> findBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);

    @Query("""
            SELECT
                s.sdcSchoolCollectionID as sdcSchoolCollectionId,
                s.schoolID as schoolId,
                s.sdcSchoolCollectionStatusCode as sdcSchoolCollectionStatusCode,
                s.uploadDate as uploadDate,
                COUNT(DISTINCT CASE WHEN i.validationIssueSeverityCode = 'ERROR' AND (stu.sdcSchoolCollectionStudentStatusCode IS NULL OR stu.sdcSchoolCollectionStudentStatusCode != 'DELETED') THEN i.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID || i.validationIssueCode END) as errors,
                COUNT(DISTINCT CASE WHEN i.validationIssueSeverityCode = 'FUNDING_WARNING' AND (stu.sdcSchoolCollectionStudentStatusCode IS NULL OR stu.sdcSchoolCollectionStudentStatusCode != 'DELETED') THEN i.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID || i.validationIssueCode END) as fundingWarnings,
                COUNT(DISTINCT CASE WHEN i.validationIssueSeverityCode = 'INFO_WARNING' AND (stu.sdcSchoolCollectionStudentStatusCode IS NULL OR stu.sdcSchoolCollectionStudentStatusCode != 'DELETED') THEN i.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID || i.validationIssueCode END) as infoWarnings
            FROM SdcSchoolCollectionEntity s
                     LEFT JOIN s.sdcSchoolStudentEntities stu
                     LEFT JOIN stu.sdcStudentValidationIssueEntities i
            WHERE s.sdcDistrictCollectionID = :sdcDistrictCollectionId
            GROUP BY s.sdcSchoolCollectionID
    """)
    List<MonitorSdcSchoolCollectionQueryResponse> findAllSdcSchoolCollectionMonitoringBySdcDistrictCollectionId(UUID sdcDistrictCollectionId);

    @Query("""
    SELECT
        s.sdcSchoolCollectionID as sdcSchoolCollectionId,
        s.schoolID as schoolId,
        s.sdcSchoolCollectionStatusCode as sdcSchoolCollectionStatusCode,
        s.uploadDate as uploadDate,
        s.uploadReportDate as uploadReportDate,
        COUNT(DISTINCT stu.sdcSchoolCollectionStudentID) as headcount,
        COUNT(DISTINCT CASE WHEN i.validationIssueSeverityCode = 'ERROR' AND (stu.sdcSchoolCollectionStudentStatusCode IS NULL OR stu.sdcSchoolCollectionStudentStatusCode != 'DELETED') THEN i.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID || i.validationIssueCode END) as errors,
        COUNT(DISTINCT CASE WHEN i.validationIssueSeverityCode = 'FUNDING_WARNING' AND (stu.sdcSchoolCollectionStudentStatusCode IS NULL OR stu.sdcSchoolCollectionStudentStatusCode != 'DELETED') THEN i.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID || i.validationIssueCode END) as fundingWarnings,
        COUNT(DISTINCT CASE WHEN i.validationIssueSeverityCode = 'INFO_WARNING' AND (stu.sdcSchoolCollectionStudentStatusCode IS NULL OR stu.sdcSchoolCollectionStudentStatusCode != 'DELETED') THEN i.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID || i.validationIssueCode END) as infoWarnings,
        COUNT(DISTINCT CASE WHEN de.duplicateResolutionCode IS NULL AND de.duplicateLevelCode = 'PROVINCIAL' AND de.duplicateTypeCode = 'PROGRAM' THEN de.sdcDuplicateID END) as unresolvedProgramDuplicates,
        COUNT(DISTINCT CASE WHEN de.duplicateResolutionCode IS NULL AND de.duplicateLevelCode = 'PROVINCIAL' AND de.duplicateTypeCode = 'ENROLLMENT' THEN de.sdcDuplicateID END) as unresolvedEnrollmentDuplicates
        FROM SdcSchoolCollectionEntity s
        LEFT JOIN s.sdcSchoolStudentEntities stu
        LEFT JOIN stu.sdcStudentValidationIssueEntities i
        LEFT JOIN SdcDuplicateStudentEntity ds ON stu.sdcSchoolCollectionStudentID = ds.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID
        LEFT JOIN SdcDuplicateEntity de ON ds.sdcDuplicateEntity.sdcDuplicateID = de.sdcDuplicateID
    WHERE s.collectionEntity.collectionID = :collectionID
    AND s.sdcDistrictCollectionID IS NULL
    GROUP BY s.sdcSchoolCollectionID
    """)
    List<MonitorIndySdcSchoolCollectionQueryResponse> findAllIndySdcSchoolCollectionMonitoringBySdcCollectionId(@Param("collectionID") UUID collectionID);

    List<SdcSchoolCollectionEntity> findAllBySdcDistrictCollectionID(UUID sdcDistrictCollectionID);

    @Query(value="""
    SELECT ssc FROM SdcSchoolCollectionEntity ssc
    WHERE ssc.sdcSchoolCollectionStatusCode = 'DIS_UPLOAD'
    AND ssc.sdcSchoolCollectionID NOT IN (
        SELECT sscs.sdcSchoolCollection.sdcSchoolCollectionID
        FROM SdcSchoolCollectionStudentEntity sscs
        WHERE sscs.sdcSchoolCollectionStudentStatusCode = 'LOADED')
    ORDER BY ssc.createDate""")
    List<SdcSchoolCollectionEntity> findSchoolCollectionsWithStudentsNotInLoadedStatus();

    @Query(value="""
    SELECT COUNT(DISTINCT ssc.sdcSchoolCollectionID) FROM SdcSchoolCollectionEntity ssc, SdcSchoolCollectionStudentEntity sscs
    WHERE ssc.sdcSchoolCollectionID = sscs.sdcSchoolCollection.sdcSchoolCollectionID
    AND sscs.sdcSchoolCollectionStudentStatusCode  = 'LOADED'
    AND ssc.uploadDate <= :uploadDate""")
    long findSdcSchoolCollectionsPositionInQueue(@Param("uploadDate") LocalDateTime uploadDate);

    @Query("""
            select ssc from SdcSchoolCollectionEntity ssc, CollectionEntity c
            where ssc.sdcDistrictCollectionID is null
            and c.collectionID = ssc.collectionEntity.collectionID
            and c.collectionStatusCode != 'COMPLETED'
            and ssc.sdcSchoolCollectionStatusCode != 'SUBMITTED'
    """)
    List<SdcSchoolCollectionEntity> findAllUnsubmittedIndependentSchoolsInCurrentCollection();

    @Modifying
    @Query(value = "UPDATE SdcSchoolCollectionEntity ssc SET ssc.sdcSchoolCollectionStatusCode = :sdcSchoolCollectionStatusCode WHERE ssc.collectionEntity.collectionID = :collectionID")
    void updateAllSchoolCollectionStatus(UUID collectionID, String sdcSchoolCollectionStatusCode);
}
