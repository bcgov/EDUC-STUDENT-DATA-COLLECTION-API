package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcDistrictCollectionRepository extends JpaRepository<SdcDistrictCollectionEntity, UUID> {

  Optional<SdcDistrictCollectionEntity> findByDistrictIDAndSdcDistrictCollectionStatusCodeNotIgnoreCase(UUID districtID, String sdcDistrictCollectionStatusCode);

  Optional<SdcDistrictCollectionEntity> findBySdcDistrictCollectionID(UUID sdcDistrictCollectionID);

  @Query(value = """
          SELECT SSC.sdc_school_collection_id
          FROM sdc_school_collection SSC
          WHERE SSC.sdc_district_collection_id = :sdcDistrictCollectionID
          AND SSC.sdc_school_collection_status_code = 'LOADED'
          """
          , nativeQuery = true)
  List<UUID> getListOfCollectionsInProgress(UUID sdcDistrictCollectionID);

}