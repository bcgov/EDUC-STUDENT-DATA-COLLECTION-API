package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionRepository extends JpaRepository<SdcSchoolCollectionEntity, UUID> {

    @Query(value="""
            SELECT SSC FROM SdcSchoolCollectionEntity SSC, CollectionEntity C WHERE SSC.schoolID=:schoolID 
            AND SSC.createDate >= C.openDate AND SSC.createDate <= C.closeDate""")
    Optional<SdcSchoolCollectionEntity> findCollectionBySchoolId(UUID schoolID);

}
