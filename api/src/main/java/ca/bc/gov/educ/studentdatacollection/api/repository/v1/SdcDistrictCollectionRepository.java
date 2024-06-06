package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.MonitorSdcDistrictCollectionQueryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcDistrictCollectionRepository extends JpaRepository<SdcDistrictCollectionEntity, UUID> {
  @Query(value="""
            SELECT SSC FROM SdcDistrictCollectionEntity SSC, CollectionEntity C WHERE SSC.districtID=:districtID
            AND C.collectionID = SSC.collectionEntity.collectionID
            AND C.openDate <= CURRENT_TIMESTAMP AND C.closeDate >= CURRENT_TIMESTAMP""")
  Optional<SdcDistrictCollectionEntity> findActiveCollectionByDistrictId(UUID districtID);

  Optional<SdcDistrictCollectionEntity> findBySdcDistrictCollectionID(UUID sdcDistrictCollectionID);

  @Query(value="""
            SELECT SSD FROM SdcDistrictCollectionEntity SSD, CollectionEntity C
            WHERE SSD.districtID=:districtID
            AND C.collectionID = SSD.collectionEntity.collectionID
            AND SSD.sdcDistrictCollectionID != :currentSdcDistrictCollectionID
            AND C.collectionTypeCode = :collectionTypeCode
            ORDER BY C.snapshotDate desc
            LIMIT 1""")
  Optional<SdcDistrictCollectionEntity> findLastCollectionByType(UUID districtID, String collectionTypeCode, UUID currentSdcDistrictCollectionID);

  @Query("""
            SELECT
                    s.sdcDistrictCollectionID as sdcDistrictCollectionID,
                    s.districtID as districtID,
                    s.sdcDistrictCollectionStatusCode as sdcDistrictCollectionStatusCode,
                    COUNT(DISTINCT CASE WHEN sc.sdcSchoolCollectionStatusCode = 'SUBMITTED' OR sc.sdcSchoolCollectionStatusCode = 'COMPLETED' THEN sc.sdcSchoolCollectionID END) as submittedSchools,
                    COUNT(DISTINCT sc.sdcSchoolCollectionID) as totalSchools,
                    COUNT(DISTINCT CASE WHEN de.duplicateResolutionCode IS NULL AND de.duplicateLevelCode = 'PROVINCIAL' AND de.duplicateTypeCode = 'PROGRAM' THEN de.sdcDuplicateID END) as unresolvedProgramDuplicates,
                    COUNT(DISTINCT CASE WHEN de.duplicateResolutionCode IS NULL AND de.duplicateLevelCode = 'PROVINCIAL' AND de.duplicateTypeCode = 'ENROLLMENT' THEN de.sdcDuplicateID END) as unresolvedEnrollmentDuplicates
                FROM SdcDistrictCollectionEntity s
                     LEFT JOIN SdcSchoolCollectionEntity sc ON s.sdcDistrictCollectionID = sc.sdcDistrictCollectionID
                     LEFT JOIN SdcDuplicateStudentEntity ds ON s.sdcDistrictCollectionID = ds.sdcDistrictCollectionID
                     LEFT JOIN SdcDuplicateEntity de ON ds.sdcDuplicateEntity.sdcDuplicateID = de.sdcDuplicateID
                WHERE (sc.sdcSchoolCollectionStatusCode IS NULL OR sc.sdcSchoolCollectionStatusCode != 'DELETED')
                  AND s.collectionEntity.collectionID = :collectionID
                GROUP BY s.sdcDistrictCollectionID
            """)
  List<MonitorSdcDistrictCollectionQueryResponse> findAllSdcDistrictCollectionMonitoringByCollectionID(UUID collectionID);
}