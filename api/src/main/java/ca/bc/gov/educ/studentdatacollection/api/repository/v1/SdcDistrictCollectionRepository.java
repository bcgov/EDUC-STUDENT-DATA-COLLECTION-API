package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcDistrictCollectionRepository extends JpaRepository<SdcDistrictCollectionEntity, UUID> {

  Optional<SdcDistrictCollectionEntity> findByDistrictIDAndSdcDistrictCollectionStatusCodeNotIgnoreCase(UUID districtID, String sdcDistrictCollectionStatusCode);

  Optional<SdcDistrictCollectionEntity> findBySdcDistrictCollectionID(UUID sdcDistrictCollectionID);
}
