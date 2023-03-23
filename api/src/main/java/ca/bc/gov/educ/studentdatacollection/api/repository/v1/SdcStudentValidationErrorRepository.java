package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentValidationIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcStudentValidationErrorRepository extends JpaRepository<SdcStudentValidationIssueEntity, UUID>, JpaSpecificationExecutor<SdcStudentValidationIssueEntity> {

  List<SdcStudentValidationIssueEntity> findAllByFieldName(String fieldName);

  @Modifying
  @Query(value = "DELETE FROM STUDENT_DATA_COLLECTION_STUDENT_VALIDATION_ISSUE where SDC_SCHOOL_STUDENT_ID = :sdcStudentId", nativeQuery = true)
  void deleteSdcStudentValidationErrors(@Param("sdcStudentId") UUID sdcStudentId);
}
