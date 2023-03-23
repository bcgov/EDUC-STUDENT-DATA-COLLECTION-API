package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolBatchEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcSchoolStudentRepository extends JpaRepository<SdcSchoolStudentEntity, UUID>, JpaSpecificationExecutor<SdcSchoolStudentEntity> {
  List<SdcSchoolStudentEntity> findAllBySdcSchoolBatchEntity(SdcSchoolBatchEntity sdcSchoolBatchEntity);

  List<SdcSchoolStudentEntity> findAllBySdcSchoolBatchID(String sdcSchoolBatchID);

  long countByStatus(String status);

  @Query(value = "SELECT " +
    "COUNT(sdc_school_student_id) " +
    "FROM " +
    "STUDENT_DATA_COLLECTION_SCHOOL_STUDENT " +
    "WHERE " +
    "sdc_school_id = ?1 " +
    "GROUP BY " +
    "student_pen " +
    "HAVING " +
    "COUNT(sdc_school_student_id) > 1", nativeQuery = true)
  Long countForDuplicateStudentPENs(String sdcSchoolID);

  List<SdcSchoolStudentEntity> findTop100ByStatusOrderByCreateDate(String status);
}
