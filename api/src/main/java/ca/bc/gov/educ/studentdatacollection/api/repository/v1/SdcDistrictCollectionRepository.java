package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcDistrictCollectionRepository extends JpaRepository<SdcDistrictCollectionEntity, UUID>, JpaSpecificationExecutor<SdcDistrictCollectionEntity> {
  @Query(value="""
            SELECT SSC FROM SdcDistrictCollectionEntity SSC, CollectionEntity C WHERE SSC.districtID=:districtID
            AND C.collectionID = SSC.collectionEntity.collectionID
            AND C.collectionStatusCode != 'COMPLETED'""")
  Optional<SdcDistrictCollectionEntity> findActiveCollectionByDistrictId(UUID districtID);

  @Query(value="""
            SELECT SSD FROM SdcDistrictCollectionEntity SSD, CollectionEntity C
            WHERE SSD.districtID=:districtID
            AND C.collectionID = SSD.collectionEntity.collectionID
            AND SSD.sdcDistrictCollectionID != :currentSdcDistrictCollectionID
            AND C.collectionTypeCode = :collectionTypeCode
            ORDER BY C.snapshotDate desc
            LIMIT 1""")
  Optional<SdcDistrictCollectionEntity> findLastCollectionByType(UUID districtID, String collectionTypeCode, UUID currentSdcDistrictCollectionID);


  @Query("""
    SELECT sdc FROM SdcDistrictCollectionEntity sdc
    JOIN sdc.collectionEntity c
    WHERE c.collectionTypeCode = :collectionTypeCode
    AND sdc.districtID = :districtID
    AND c.snapshotDate <= :currentSnapshotDate
    ORDER BY c.snapshotDate DESC
    LIMIT 1
    """)
  Optional<SdcDistrictCollectionEntity> findLastOrCurrentSdcDistrictCollectionByCollectionType(String collectionTypeCode, UUID districtID, LocalDate currentSnapshotDate);

  @Query("""
    SELECT sdc FROM SdcDistrictCollectionEntity sdc
    JOIN sdc.collectionEntity c
    WHERE c.collectionTypeCode = :collectionTypeCode
    AND sdc.districtID = :districtID
    AND c.snapshotDate < :currentSnapshotDate
    ORDER BY c.snapshotDate DESC
    LIMIT 1
    """)
  Optional<SdcDistrictCollectionEntity> findLastSdcDistrictCollectionByCollectionTypeBefore(String collectionTypeCode, UUID districtID, LocalDate currentSnapshotDate);

  @Modifying
  @Query(value = "UPDATE SdcDistrictCollectionEntity sdc SET sdc.sdcDistrictCollectionStatusCode = :sdcDistrictCollectionStatusCode WHERE sdc.collectionEntity.collectionID = :collectionID")
  void updateAllDistrictCollectionStatus(UUID collectionID, String sdcDistrictCollectionStatusCode);

  @Query(value = "SELECT sdc FROM SdcDistrictCollectionEntity sdc WHERE sdc.collectionEntity.collectionID = :collectionID AND sdc.sdcDistrictCollectionStatusCode != 'COMPLETED'")
  List<SdcDistrictCollectionEntity> findAllIncompleteDistrictCollections(UUID collectionID);

  List<SdcDistrictCollectionEntity> findAllByCollectionEntityCollectionID(UUID collectionID);
}
