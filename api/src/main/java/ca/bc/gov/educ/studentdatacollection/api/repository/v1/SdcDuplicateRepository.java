package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcDuplicateRepository extends JpaRepository<SdcDuplicateEntity, UUID> {
    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcDistrictCollectionID(UUID sdcDistrictCollectionID);

    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionID(UUID sdcSchoolCollectionID);

    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionStudentEntity_SdcSchoolCollectionStudentIDIn(List<UUID> removedStudents);

    Optional<SdcDuplicateEntity> findBySdcDuplicateID(UUID sdcDuplicateID);

    void deleteAllBySdcDuplicateStudentEntities_SdcSchoolCollectionID(UUID sdcSchoolCollectionID);

    void deleteAllBySdcDuplicateStudentEntities_SdcSchoolCollectionStudentEntity_SdcSchoolCollectionStudentID(UUID sdcSchoolCollectionStudentID);

    @Query("""
        SELECT sde FROM SdcDuplicateEntity sde
        WHERE sde.collectionID = (SELECT C.collectionID FROM CollectionEntity C WHERE C.collectionStatusCode != 'COMPLETED')
        AND sde.duplicateLevelCode = 'PROVINCIAL'
        """)
    List<SdcDuplicateEntity> findAllProvincialDuplicatesForCurrentCollection();

    @Query("""
        SELECT sde FROM SdcDuplicateEntity sde
        WHERE sde.collectionID = (SELECT C.collectionID FROM CollectionEntity C WHERE C.collectionStatusCode = 'PROVDUPES')
        and sde.duplicateResolutionCode is null
        and sde.duplicateTypeCode = 'ENROLLMENT'
        and sde.duplicateSeverityCode = 'NON_ALLOW'
        """)
    List<SdcDuplicateEntity> findAllUnresolvedNonAllowableEnrollmentDuplicatesForCurrentCollection();

    @Query("""
        SELECT sde FROM SdcDuplicateEntity sde
        WHERE sde.duplicateResolutionCode is null
        and sde.duplicateTypeCode = :duplicateTypeCode
        and sde.duplicateSeverityCode = :duplicateSeverityCode
        and sde.sdcDuplicateID IN (SELECT sds.sdcDuplicateEntity.sdcDuplicateID FROM SdcDuplicateStudentEntity sds where sds.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID = :sdcSchoolCollectionStudentID)
        """)
    List<SdcDuplicateEntity> findAllUnresolvedDuplicatesForStudentByTypeAndSeverity(UUID sdcSchoolCollectionStudentID, String duplicateTypeCode, String duplicateSeverityCode);

    @Query("""
        SELECT sde FROM SdcDuplicateEntity sde
        WHERE sde.duplicateResolutionCode is null
        and sde.duplicateTypeCode = 'ENROLLMENT'
        and sde.sdcDuplicateID IN (SELECT sds.sdcDuplicateEntity.sdcDuplicateID FROM SdcDuplicateStudentEntity sds where sds.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID = :sdcSchoolCollectionStudentID)
        """)
    List<SdcDuplicateEntity> findAllUnresolvedEnrollmentDuplicatesForStudent(UUID sdcSchoolCollectionStudentID);

    @Query("""
        SELECT sde FROM SdcDuplicateEntity sde
        WHERE sde.duplicateResolutionCode is null
        and sde.duplicateSeverityCode = 'NON_ALLOW'
        and sde.sdcDuplicateID IN (SELECT sds.sdcDuplicateEntity.sdcDuplicateID FROM SdcDuplicateStudentEntity sds where sds.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID = :sdcSchoolCollectionStudentID)
        """)
    List<SdcDuplicateEntity>findAllUnresolvedNonAllowDuplicatesForStudent(UUID sdcSchoolCollectionStudentID);

    @Query("""
        SELECT sde FROM SdcDuplicateEntity sde
        WHERE sde.duplicateResolutionCode is null
        and sde.sdcDuplicateID IN (SELECT sds.sdcDuplicateEntity.sdcDuplicateID FROM SdcDuplicateStudentEntity sds where sds.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID = :sdcSchoolCollectionStudentID)
        """)
    List<SdcDuplicateEntity> findAllUnresolvedDuplicatesForStudent(UUID sdcSchoolCollectionStudentID);

    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionStudentEntity_SdcSchoolCollectionStudentID(UUID sdcSchoolCollectionStudentID);

    @Query("""
        SELECT sde FROM SdcDuplicateEntity sde
        WHERE sde.duplicateResolutionCode = 'GRADE_CHNG'
        and sde.sdcDuplicateID IN (SELECT sds.sdcDuplicateEntity.sdcDuplicateID FROM SdcDuplicateStudentEntity sds 
        where sds.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID = :sdcSchoolCollectionStudentID)
        """)
    List<SdcDuplicateEntity> findAllResolvedGradeChangeDuplicatesForStudent(UUID sdcSchoolCollectionStudentID);
    @Query("""
        SELECT sde FROM SdcDuplicateEntity sde
        WHERE sde.collectionID = (SELECT C.collectionID FROM CollectionEntity C WHERE C.collectionStatusCode = 'PROVDUPES')
        and sde.duplicateResolutionCode is null
        and sde.duplicateTypeCode = 'PROGRAM'
        and sde.duplicateSeverityCode = 'NON_ALLOW'
        """)
    List<SdcDuplicateEntity> findAllUnresolvedNonAllowableProgramDuplicatesForCurrentCollection();

    List<SdcDuplicateEntity> findAllDuplicatesByCollectionIDAndDuplicateLevelCode(UUID collectionID, String duplicateLevelCode);

    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcDistrictCollectionIDAndDuplicateLevelCode(UUID sdcDistrictCollectionID, String duplicateLevelCode);

    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionIDAndDuplicateLevelCode(UUID sdcSchoolCollectionID, String duplicateLevelCode);

}
