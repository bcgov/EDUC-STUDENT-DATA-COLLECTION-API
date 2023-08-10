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

    Optional<List<SdcSchoolCollectionEntity>> findAllBySchoolIDAndCreateDateBetween(UUID schoolId, LocalDateTime startDate, LocalDateTime endDate);

    Optional<List<SdcSchoolCollectionEntity>> findAllByDistrictIDAndCreateDateBetween(UUID schoolId, LocalDateTime startDate, LocalDateTime endDate);

    Optional<List<SdcSchoolCollectionEntity>> findAllBySchoolIDInAndCreateDateBetween(List<UUID> schoolIds, LocalDateTime startDate, LocalDateTime endDate);

    List<SdcSchoolCollectionEntity> findAllBySchoolID(UUID schoolID);

}
