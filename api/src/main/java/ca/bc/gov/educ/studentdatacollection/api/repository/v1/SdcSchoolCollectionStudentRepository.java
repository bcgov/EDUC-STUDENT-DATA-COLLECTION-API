package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionStudentRepository extends JpaRepository<SdcSchoolCollectionStudentEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentEntity> {
  List<SdcSchoolCollectionStudentEntity> findAllBySdcSchoolCollectionEntity(SdcSchoolCollectionEntity sdcSchoolBatchEntity);

  List<SdcSchoolCollectionStudentEntity> findAllBySdcSchoolCollectionID(String sdcSchoolCollectionID);

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

  @Query(value = "SELECT stud.*\n" +
    "FROM SDC_SCHOOL_COLLECTION_STUDENT stud\n" +
    "WHERE stud.SDC_SCHOOL_COLLECTION_STUDENT_ID NOT IN\n" +
    "    (SELECT SDC_SCHOOL_COLLECTION_STUDENT_ID \n" +
    "     FROM SDC_SAGA saga)\n" +
    "AND\n" +
    "stud.SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE = 'LOADED'\n" +
    "order by CREATE_DATE asc\n" +
    "LIMIT 100;\n",
    nativeQuery = true)
  List<SdcSchoolCollectionStudentEntity> findTop100LoadedStudentForProcessing();

  List<SdcSchoolCollectionStudentEntity> findTop100BySdcSchoolCollectionStudentStatusCodeOrderByCreateDate(String sdcSchoolCollectionStudentStatusCode);
}
