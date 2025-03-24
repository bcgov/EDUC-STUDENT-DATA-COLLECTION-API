package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentFsaReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionStudentFsaReportRepository extends JpaRepository<SdcSchoolCollectionStudentFsaReportEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentFsaReportEntity> {
    List<SdcSchoolCollectionStudentFsaReportEntity> findAllBySdcSchoolCollection_CollectionIDAndEnrolledGradeCodeInAndSdcSchoolCollectionStudentStatusCodeIsNot(UUID collectionID, List<String> enrolledGradeCode, String sdcSchoolCollectionStudentStatusCode);
    Page<SdcSchoolCollectionStudentFsaReportEntity> findAllBySdcSchoolCollection_CollectionIDAndEnrolledGradeCodeInAndSdcSchoolCollectionStudentStatusCodeIsNot(UUID collectionID, List<String> enrolledGradeCode, String sdcSchoolCollectionStudentStatusCode, Pageable pageable);
}
