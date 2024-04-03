package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcDistrictCollectionRepository extends JpaRepository<SdcDistrictCollectionEntity, UUID> {

  @Query(value="""
            SELECT SSC FROM SdcDistrictCollectionEntity SSC, CollectionEntity C WHERE SSC.districtID=:districtID
            AND C.collectionID = SSC.collectionEntity.collectionID
            AND C.openDate <= CURRENT_TIMESTAMP AND C.closeDate >= CURRENT_TIMESTAMP""")
  Optional<SdcDistrictCollectionEntity> findActiveCollectionByDistrictId(UUID districtID);

  @Query(value= "SELECT SSC.districtID FROM SdcDistrictCollectionEntity SSC WHERE SSC.sdcDistrictCollectionID=:sdcDistrictCollectionID")
  Optional<SdcDistrictCollectionEntity> findDistrictIDByDistrictCollectionId(UUID sdcDistrictCollectionID);
}
