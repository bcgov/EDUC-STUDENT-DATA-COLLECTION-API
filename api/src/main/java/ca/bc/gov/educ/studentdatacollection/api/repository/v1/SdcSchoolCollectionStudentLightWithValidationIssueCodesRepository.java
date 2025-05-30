package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SdcSchoolCollectionStudentLightWithValidationIssueCodesRepository extends JpaRepository<SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity> {

    @Query("SELECT DISTINCT s FROM SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity s " +
            "JOIN s.sdcSchoolCollectionEntity sc " +
            "LEFT JOIN s.sdcStudentValidationIssueEntities v " +
            "WHERE sc.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
            "AND s.schoolFundingCode = :schoolFundingCode " +
            "AND s.sdcSchoolCollectionStudentStatusCode NOT IN :statusCodes")
    List<SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity> findByDistrictCollectionIDAndStatusNotInAndSchoolFundingCodeEquals(
            @Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID,
            @Param("statusCodes") List<String> statusCodes,
            @Param("schoolFundingCode") String schoolFundingCode
    );

    @Query("SELECT DISTINCT s FROM SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity s " +
            "JOIN s.sdcSchoolCollectionEntity sc " +
            "LEFT JOIN s.sdcStudentValidationIssueEntities v " +
            "WHERE sc.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
            "AND s.schoolFundingCode = :schoolFundingCode " +
            "AND s.sdcSchoolCollectionStudentStatusCode <> :statusCode ")
    List<SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity> findBySchoolCollectionIDAndStatusNotAndSchoolFundingCodeEquals(
            @Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID,
            @Param("statusCode") String statusCode,
            @Param("schoolFundingCode") String schoolFundingCode
    );
}
