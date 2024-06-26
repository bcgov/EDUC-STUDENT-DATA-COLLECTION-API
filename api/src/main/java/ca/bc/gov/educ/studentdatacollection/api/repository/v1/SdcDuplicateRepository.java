package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcDuplicateRepository extends JpaRepository<SdcDuplicateEntity, UUID> {
    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcDistrictCollectionID(UUID sdcDistrictCollectionID);

    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionStudentEntity_SdcSchoolCollectionStudentIDIn(List<UUID> removedStudents);

    Optional<SdcDuplicateEntity> findBySdcDuplicateID(UUID sdcDuplicateID);

    void deleteAllBySdcDuplicateStudentEntities_SdcSchoolCollectionID(UUID sdcSchoolCollectionID);

    void deleteAllBySdcDuplicateStudentEntities_SdcSchoolCollectionStudentEntity_SdcSchoolCollectionStudentID(UUID sdcSchoolCollectionStudentID);

    @Query("""
        SELECT sde FROM SdcDuplicateEntity sde
        JOIN sde.sdcDuplicateStudentEntities sds
        LEFT JOIN SdcDistrictCollectionEntity sdd ON sds.sdcDistrictCollectionID = sdd.sdcDistrictCollectionID
        LEFT JOIN SdcSchoolCollectionEntity sss ON sds.sdcSchoolCollectionID = sss.sdcSchoolCollectionID
        WHERE (sdd.collectionEntity.collectionID = (SELECT C.collectionID FROM CollectionEntity C WHERE C.openDate <= CURRENT_TIMESTAMP AND C.closeDate >= CURRENT_TIMESTAMP)
        OR sss.collectionEntity.collectionID = (SELECT C.collectionID FROM CollectionEntity C WHERE C.openDate <= CURRENT_TIMESTAMP AND C.closeDate >= CURRENT_TIMESTAMP))
        AND sde.duplicateLevelCode = 'PROVINCIAL'
        """)
    List<SdcDuplicateEntity> findAllProvincialDuplicatesForCurrentCollection();

    @Query("""
        SELECT sde FROM SdcDuplicateEntity sde
        JOIN sde.sdcDuplicateStudentEntities sds
        LEFT JOIN SdcDistrictCollectionEntity sdd ON sds.sdcDistrictCollectionID = sdd.sdcDistrictCollectionID
        LEFT JOIN SdcSchoolCollectionEntity sss ON sds.sdcSchoolCollectionID = sss.sdcSchoolCollectionID
        WHERE (sdd.collectionEntity.collectionID = :collectionID OR sss.collectionEntity.collectionID = :collectionID)
        AND sde.duplicateLevelCode = 'PROVINCIAL'
        """)
    List<SdcDuplicateEntity> findAllProvincialDuplicatesByCollectionID(@Param("collectionID") UUID collectionID);

    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcDistrictCollectionIDAndDuplicateLevelCode(UUID sdcDistrictCollectionID, String duplicateLevelCode);

    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionIDAndDuplicateLevelCode(UUID sdcSchoolCollectionID, String duplicateLevelCode);

}
