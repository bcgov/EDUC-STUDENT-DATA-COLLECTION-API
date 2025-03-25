package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.dto.sdc.SdcSchoolCollectionIdSchoolId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SdcSchoolCollectionLightRepository extends JpaRepository<SdcSchoolCollectionLightEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionLightEntity> {
    Optional<SdcSchoolCollectionLightEntity> findBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);
    @Query("SELECT new ca.bc.gov.educ.studentdatacollection.api.model.v1.dto.sdc.SdcSchoolCollectionIdSchoolId(sc.sdcSchoolCollectionID, sc.schoolID) " +
            "FROM SdcSchoolCollectionLightEntity sc " +
            "WHERE sc.collectionID = :collectionID")
    List<SdcSchoolCollectionIdSchoolId> findSdcSchoolCollectionIdSchoolIdByCollectionId(@Param("collectionID") UUID collectionID);
}
