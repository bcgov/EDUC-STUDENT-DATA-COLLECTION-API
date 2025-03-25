package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentFsaReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionStudentFsaReportRepository extends JpaRepository<SdcSchoolCollectionStudentFsaReportEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentFsaReportEntity> {
    //List<SdcSchoolCollectionStudentFsaReportEntity> findAllBySdcSchoolCollection_CollectionIDAndEnrolledGradeCodeInAndSdcSchoolCollectionStudentStatusCodeIsNot(UUID collectionID, List<String> enrolledGradeCode, String sdcSchoolCollectionStudentStatusCode);

    @Query("SELECT s FROM SdcSchoolCollectionStudentFsaReportEntity s " +
            "JOIN FETCH s.sdcSchoolCollection sc " +
            "WHERE sc.collectionID = :collectionID " +
            "AND s.enrolledGradeCode IN :enrolledGradeCode " +
            "AND s.sdcSchoolCollectionStudentStatusCode <> :statusCode")
    List<SdcSchoolCollectionStudentFsaReportEntity> findAllBySdcSchoolCollection_CollectionIDAndEnrolledGradeCodeInAndSdcSchoolCollectionStudentStatusCodeIsNot(
            @Param("collectionID") UUID collectionID,
            @Param("enrolledGradeCode") List<String> enrolledGradeCode,
            @Param("statusCode") String sdcSchoolCollectionStudentStatusCode);
}
