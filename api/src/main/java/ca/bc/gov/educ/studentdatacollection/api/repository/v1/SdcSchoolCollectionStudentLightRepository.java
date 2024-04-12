package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SdcSchoolCollectionStudentLightRepository extends JpaRepository<SdcSchoolCollectionStudentLightEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentLightEntity> {
    List<SdcSchoolCollectionStudentLightEntity> findAllBySdcSchoolCollectionID(UUID sdcSchoolCollectionUUID);

    @Query(value = """
            SELECT sscs FROM SdcSchoolCollectionStudentLightEntity sscs
            JOIN sscs.sdcSchoolCollectionEntity sdc
            WHERE sdc.sdcDistrictCollectionID = :districtCollectionID
            """)
    List<SdcSchoolCollectionStudentLightEntity> findAllBySdcDistrictCollectionID(@Param("districtCollectionID") UUID districtCollectionID);
}
