package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolCollectionSchoolID;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionsForAutoSubmit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionRepository extends JpaRepository<SdcSchoolCollectionEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionEntity> {

    @Query(value="""
            SELECT SSC FROM SdcSchoolCollectionEntity SSC, CollectionEntity C WHERE SSC.schoolID=:schoolID 
            AND C.collectionID = SSC.collectionEntity.collectionID
            AND C.collectionStatusCode != 'COMPLETED'""")
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
        AND C.collection_status_code = 'COMPLETED'
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
    Optional<SdcSchoolCollectionEntity> findLastCollectionBySchoolIDAndType(UUID schoolID, String collectionTypeCode, UUID currentSdcCollectionID);

    @Query(value="""
            SELECT C FROM CollectionEntity C 
            WHERE C.collectionID != :currentCollectionID
            AND C.collectionTypeCode = :collectionTypeCode
            ORDER BY C.snapshotDate desc
            LIMIT 1""")
    Optional<CollectionEntity> findLastCollectionByType(String collectionTypeCode, UUID currentCollectionID);

    List<SdcSchoolCollectionEntity> findAllBySchoolID(UUID schoolID);

    @Query(value="""
            SELECT new ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolCollectionSchoolID(sdc.sdcSchoolCollectionID, sdc.schoolID) FROM SdcSchoolCollectionEntity sdc 
            WHERE sdc.sdcSchoolCollectionID in :sdcSchoolCollectionIDs
            """)
    List<SchoolCollectionSchoolID> findSchoolIDBySdcSchoolCollectionIDIn(List<UUID> sdcSchoolCollectionIDs);
    Optional<SdcSchoolCollectionEntity> findBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);
    List<SdcSchoolCollectionEntity> findAllByCollectionEntityCollectionID(UUID collectionID);

    List<SdcSchoolCollectionEntity> findAllBySdcDistrictCollectionID(UUID sdcDistrictCollectionID);

    @Query(value="""
    SELECT ssc.sdcSchoolCollectionID as sdcSchoolCollectionID,
    (SELECT COUNT(s) FROM SdcSchoolCollectionStudentEntity s WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = ssc.sdcSchoolCollectionID
    AND s.sdcSchoolCollectionStudentStatusCode = 'ERROR') as errorCount,
    (SELECT COUNT(stud)
    FROM SdcSchoolCollectionStudentEntity stud
    WHERE assignedStudentId IN (SELECT assignedStudentId
                FROM SdcSchoolCollectionStudentEntity
                where sdcSchoolCollection.sdcSchoolCollectionID = ssc.sdcSchoolCollectionID
                and assignedStudentId is not null
                and sdcSchoolCollectionStudentStatusCode != 'DELETED'
                GROUP BY assignedStudentId
                HAVING COUNT(assignedStudentId) > 1)
    and sdcSchoolCollection.sdcSchoolCollectionID = ssc.sdcSchoolCollectionID
    and sdcSchoolCollectionStudentStatusCode != 'DELETED'
    and assignedStudentId is not null) as dupeCount
    FROM SdcSchoolCollectionEntity ssc
    WHERE ssc.sdcSchoolCollectionStatusCode = 'DIS_UPLOAD'
    AND ssc.sdcSchoolCollectionID NOT IN (
        SELECT sscs.sdcSchoolCollection.sdcSchoolCollectionID
        FROM SdcSchoolCollectionStudentEntity sscs
        WHERE sscs.sdcSchoolCollectionStudentStatusCode = 'LOADED')
    ORDER BY ssc.uploadDate asc
    LIMIT :numberOfSchoolCollToProcess""")
    List<SdcSchoolCollectionsForAutoSubmit> findSchoolCollectionsWithStudentsNotInLoadedStatus(String numberOfSchoolCollToProcess);

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

    @Query("""
    SELECT ssc FROM SdcSchoolCollectionEntity ssc
    WHERE ssc.sdcSchoolCollectionID NOT IN (
        SELECT sds.sdcSchoolCollectionID FROM SdcDuplicateStudentEntity sds
        WHERE 
        sds.sdcDuplicateEntity.collectionID = :collectionID AND 
        sds.sdcDuplicateEntity.duplicateLevelCode = 'PROVINCIAL')
    AND ssc.collectionEntity.collectionID = :collectionID
    """)
    List<SdcSchoolCollectionEntity> findAllSchoolCollectionsWithoutProvincialDupes(UUID collectionID);

    @Query("""
    SELECT ssc FROM SdcSchoolCollectionEntity ssc
    WHERE ssc.sdcSchoolCollectionID IN (
        SELECT sds.sdcSchoolCollectionID FROM SdcDuplicateStudentEntity sds
        WHERE 
        sds.sdcDuplicateEntity.collectionID = :collectionID AND 
        sds.sdcDuplicateEntity.duplicateLevelCode = 'PROVINCIAL')
    AND ssc.collectionEntity.collectionID = :collectionID
    """)
    List<SdcSchoolCollectionEntity> findAllSchoolCollectionsWithProvincialDupes(UUID collectionID);

    @Query(value = "SELECT ssc FROM SdcSchoolCollectionEntity ssc WHERE ssc.collectionEntity.collectionID = :collectionID AND ssc.sdcSchoolCollectionStatusCode != 'COMPLETED'")
    List<SdcSchoolCollectionEntity> findUncompletedSchoolCollections(UUID collectionID);

    @Query("SELECT s FROM SdcSchoolCollectionEntity s WHERE s.collectionEntity.collectionID = :collectionID AND s.sdcDistrictCollectionID IS NULL AND s.sdcSchoolCollectionStatusCode = 'COMPLETED'")
    List<SdcSchoolCollectionEntity> findSchoolsInCollectionWithStatus(UUID collectionID);

}
