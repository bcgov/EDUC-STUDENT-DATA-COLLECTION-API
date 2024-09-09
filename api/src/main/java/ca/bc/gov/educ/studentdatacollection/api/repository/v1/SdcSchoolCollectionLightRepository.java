package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionLightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SdcSchoolCollectionLightRepository extends JpaRepository<SdcSchoolCollectionLightEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionLightEntity> {
    Optional<SdcSchoolCollectionLightEntity> findBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);
}
