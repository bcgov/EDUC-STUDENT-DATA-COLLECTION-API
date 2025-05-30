package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SdcSchoolCollectionStudentLightWithEnrolledProgramCodesRepository extends JpaRepository<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> {

    @Query("SELECT DISTINCT s FROM SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity s " +
            "JOIN s.sdcSchoolCollectionEntity sc " +
            "JOIN s.sdcStudentEnrolledProgramEntities ep " +
            "WHERE sc.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
            "AND s.sdcSchoolCollectionStudentStatusCode NOT IN :statusCodes " +
            "AND ep.enrolledProgramCode IN :enrolledProgramCodes")
    List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> findByDistrictCollectionIDAndStatusNotInAndEnrolledProgramCodeIn(
            @Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID,
            @Param("statusCodes") List<String> statusCodes,
            @Param("enrolledProgramCodes") List<String> enrolledProgramCodes
    );

    @Query("SELECT DISTINCT s FROM SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity s " +
            "JOIN s.sdcSchoolCollectionEntity sc " +
            "JOIN s.sdcStudentEnrolledProgramEntities ep " +
            "WHERE sc.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
            "AND s.sdcSchoolCollectionStudentStatusCode <> :statusCode " +
            "AND ep.enrolledProgramCode IN :enrolledProgramCodes")
    List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> findBySchoolCollectionIDAndStatusNotAndEnrolledProgramCodeIn(
            @Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID,
            @Param("statusCode") String statusCode,
            @Param("enrolledProgramCodes") List<String> enrolledProgramCodes
    );

    @Query("SELECT DISTINCT s FROM SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity s " +
            "JOIN s.sdcSchoolCollectionEntity sc " +
            "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
            "WHERE sc.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
            "AND s.sdcSchoolCollectionStudentStatusCode NOT IN :statusCodes " +
            "AND (ep.enrolledProgramCode IN :enrolledProgramCodes " +
            "OR s.bandCode IS NOT NULL " +
            "OR s.nativeAncestryInd = 'Y') ")
    List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> findAllLightIndigenousDistrict(
            @Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID,
            @Param("statusCodes") List<String> statusCodes,
            @Param("enrolledProgramCodes") List<String> enrolledProgramCodes
    );

    @Query("SELECT DISTINCT s FROM SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity s " +
            "JOIN s.sdcSchoolCollectionEntity sc " +
            "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
            "WHERE sc.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
            "AND s.sdcSchoolCollectionStudentStatusCode <> :statusCode " +
            "AND (ep.enrolledProgramCode IN :enrolledProgramCodes " +
            "OR s.bandCode IS NOT NULL " +
            "OR s.nativeAncestryInd = 'Y') ")
    List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> findAllLightIndigenousSchool(
            @Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID,
            @Param("statusCode") String statusCode,
            @Param("enrolledProgramCodes") List<String> enrolledProgramCodes
    );
}
