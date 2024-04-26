package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcDistrictCollectionRepository extends JpaRepository<SdcDistrictCollectionEntity, UUID> {

  Optional<SdcDistrictCollectionEntity> findByDistrictIDAndSdcDistrictCollectionStatusCodeNotIgnoreCase(UUID districtID, String sdcDistrictCollectionStatusCode);

  Optional<SdcDistrictCollectionEntity> findBySdcDistrictCollectionID(UUID sdcDistrictCollectionID);

  @Query(value="""
            SELECT SSD FROM SdcDistrictCollectionEntity SSD, CollectionEntity C 
            WHERE SSD.districtID=:districtID 
            AND C.collectionID = SSD.collectionEntity.collectionID
            AND SSD.sdcDistrictCollectionID != :currentSdcDistrictCollectionID
            AND C.collectionTypeCode = :collectionTypeCode
            ORDER BY C.snapshotDate desc
            LIMIT 1""")
  Optional<SdcDistrictCollectionEntity> findLastCollectionByType(UUID districtID, String collectionTypeCode, UUID currentSdcDistrictCollectionID);

}