package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionStudentRepository extends JpaRepository<SdcSchoolCollectionStudentEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentEntity> {
  List<SdcSchoolCollectionStudentEntity> findAllBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);

  long countBySdcSchoolCollectionStudentStatusCode(String sdcSchoolCollectionStudentStatusCode);

  @Query(value = "SELECT " +
    "COUNT(SDC_SCHOOL_COLLECTION_STUDENT_ID) " +
    "FROM " +
    "SDC_SCHOOL_COLLECTION_STUDENT " +
    "WHERE " +
    "sdc_school_collection_id = ?1 " +
    "GROUP BY " +
    "student_pen " +
    "HAVING " +
    "COUNT(SDC_SCHOOL_COLLECTION_STUDENT_ID) > 1", nativeQuery = true)
  Long countForDuplicateStudentPENs(String sdcSchoolID);

  @Query(value="""
    SELECT stud FROM SdcSchoolCollectionStudentEntity stud WHERE stud.sdcSchoolCollectionStudentID 
    NOT IN (SELECT saga.sdcSchoolCollectionStudentID FROM SdcSagaEntity saga) 
    AND stud.sdcSchoolCollectionStudentStatusCode = 'LOADED' 
    order by stud.createDate 
    LIMIT :numberOfStudentsToProcess""")
  List<SdcSchoolCollectionStudentEntity> findTopLoadedStudentForProcessing(String numberOfStudentsToProcess);

  List<SdcSchoolCollectionStudentEntity> findTop100BySdcSchoolCollectionStudentStatusCodeOrderByCreateDate(String sdcSchoolCollectionStudentStatusCode);
}
