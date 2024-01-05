package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

    @Query(value="""
            SELECT SSC FROM SdcSchoolCollectionEntity SSC, CollectionEntity C 
            WHERE SSC.schoolID=:schoolID 
            AND C.collectionID = SSC.collectionEntity.collectionID
            AND SSC.sdcSchoolCollectionID != :currentSdcCollectionID
            AND C.collectionTypeCode = :collectionTypeCode
            ORDER BY C.snapshotDate desc
            LIMIT 1""")
    Optional<SdcSchoolCollectionEntity> findLastCollectionByType(UUID schoolID, String collectionTypeCode, UUID currentSdcCollectionID);

    List<SdcSchoolCollectionEntity> findAllByDistrictIDAndCreateDateBetween(UUID schoolId, LocalDateTime startDate, LocalDateTime endDate);

    List<SdcSchoolCollectionEntity> findAllBySchoolIDInAndCreateDateBetween(List<UUID> schoolIds, LocalDateTime startDate, LocalDateTime endDate);

    List<SdcSchoolCollectionEntity> findAllBySchoolID(UUID schoolID);
    Optional<SdcSchoolCollectionEntity> findBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);

}
