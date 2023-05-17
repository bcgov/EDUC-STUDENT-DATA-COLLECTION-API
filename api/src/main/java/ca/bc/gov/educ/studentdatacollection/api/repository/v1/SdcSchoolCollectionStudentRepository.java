package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.IValidationIssueSeverityCodeCount;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SdcSchoolCollectionStudentRepository extends JpaRepository<SdcSchoolCollectionStudentEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentEntity> {
  List<SdcSchoolCollectionStudentEntity> findAllBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);

  long countBySdcSchoolCollectionStudentStatusCode(String sdcSchoolCollectionStudentStatusCode);

  @Query(value = """
  SELECT
  	COUNT(i.validationIssueSeverityCode) AS validationIssueSeverityCodeCount, i.validationIssueSeverityCode AS validationIssueSeverityCode
  FROM SdcSchoolCollectionStudentEntity s, SdcSchoolCollectionStudentValidationIssueEntity i
  WHERE
  	s.sdcSchoolCollectionStudentID = i.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID
  	AND s.sdcSchoolCollectionID = :sdcSchoolCollectionID
  	AND i.validationIssueSeverityCode IN ('ERROR', 'WARNING')
  GROUP BY i.validationIssueSeverityCode
""")
  List<IValidationIssueSeverityCodeCount> errorAndWarningCountBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);

  long countBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);

  @Query(value = """
    SELECT COUNT(*) FROM SDC_SCHOOL_COLLECTION_STUDENT SSCS 
    WHERE SSCS.sdc_school_collection_id = :sdcSchoolID 
    AND SSCS.STUDENT_PEN=:studentPen""", nativeQuery = true)
  Long countForDuplicateStudentPENs(UUID sdcSchoolID, String studentPen);

  @Query(value="""
    SELECT stud FROM SdcSchoolCollectionStudentEntity stud WHERE stud.sdcSchoolCollectionStudentID 
    NOT IN (SELECT saga.sdcSchoolCollectionStudentID FROM SdcSagaEntity saga) 
    AND stud.sdcSchoolCollectionStudentStatusCode = 'LOADED' 
    order by stud.createDate 
    LIMIT :numberOfStudentsToProcess""")
  List<SdcSchoolCollectionStudentEntity> findTopLoadedStudentForProcessing(String numberOfStudentsToProcess);

  List<SdcSchoolCollectionStudentEntity> findTop100BySdcSchoolCollectionStudentStatusCodeOrderByCreateDate(String sdcSchoolCollectionStudentStatusCode);
}
