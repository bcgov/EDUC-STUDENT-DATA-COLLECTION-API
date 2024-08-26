package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface SdcSchoolCollectionStudentLightRepository extends JpaRepository<SdcSchoolCollectionStudentLightEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentLightEntity> {
    List<SdcSchoolCollectionStudentLightEntity> findAllBySdcSchoolCollectionEntity_CollectionEntity_CollectionID(UUID collection);

    List<SdcSchoolCollectionStudentLightEntity> findAllBySdcSchoolCollectionEntity_SdcDistrictCollectionID(UUID sdcDistrictCollectionID);

    List<SdcSchoolCollectionStudentLightEntity> findAllBySdcSchoolCollectionID(UUID sdcSchoolCollectionUUID);


}
