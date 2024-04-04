package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SdcSchoolCollectionStudentLightRepository extends JpaRepository<SdcSchoolCollectionStudentLightEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentLightEntity> {
    List<SdcSchoolCollectionStudentLightEntity> findAllBySdcSchoolCollectionID(UUID sdcSchoolCollectionUUID);

    // TODO make query jpa, need to somehow get sdc.school_id into the light student entity
    @Query(value="""
            select sscs.*, sdc.school_id
            from sdc_school_collection sdc, sdc_district_collection disCol, sdc_school_collection_student sscs
            where sdc.sdc_district_collection_id = discol.sdc_district_collection_id
            and sdc.sdc_school_collection_id = sscs.sdc_school_collection_id
            and disCol.sdc_district_collection_id = :districtCollectionID
            """, nativeQuery = true)
    List<SdcSchoolCollectionStudentLightEntity> findAllBySdcDistrictCollectionID(UUID districtCollectionID);
}
