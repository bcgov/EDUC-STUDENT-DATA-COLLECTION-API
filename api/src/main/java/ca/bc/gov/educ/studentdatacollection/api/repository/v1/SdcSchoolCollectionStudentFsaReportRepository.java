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
    @Query("SELECT s FROM SdcSchoolCollectionStudentFsaReportEntity s " +
            "WHERE s.sdcSchoolCollectionID IN :sdcSchoolCollectionIDs " +
            "AND s.enrolledGradeCode IN :enrolledGradeCode " +
            "AND s.sdcSchoolCollectionStudentStatusCode <> :statusCode")
    List<SdcSchoolCollectionStudentFsaReportEntity> findAllBySdcSchoolCollectionIDsAndEnrolledGradeCodeInAndSdcSchoolCollectionStudentStatusCodeIsNot(
            @Param("sdcSchoolCollectionIDs") List<UUID> sdcSchoolCollectionIDs,
            @Param("enrolledGradeCode") List<String> enrolledGradeCode,
            @Param("statusCode") String sdcSchoolCollectionStudentStatusCode);
}
