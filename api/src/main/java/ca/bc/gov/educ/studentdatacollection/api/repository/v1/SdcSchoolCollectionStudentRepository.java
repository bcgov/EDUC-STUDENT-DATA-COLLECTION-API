package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EnrollmentHeadcountResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SdcSchoolCollectionStudentRepository extends JpaRepository<SdcSchoolCollectionStudentEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentEntity> {
  List<SdcSchoolCollectionStudentEntity> findAllBySdcSchoolCollection_SdcSchoolCollectionID(UUID sdcSchoolCollectionID);

  long countBySdcSchoolCollectionStudentStatusCodeAndSdcSchoolCollection_SdcSchoolCollectionID(String sdcSchoolCollectionStudentStatusCode, UUID sdcSchoolCollectionID);

  @Query(value = """
    SELECT COUNT(*) FROM (SELECT I.SDC_SCHOOL_COLLECTION_STUDENT_ID, COUNT(I.VALIDATION_ISSUE_SEVERITY_CODE), I.VALIDATION_ISSUE_CODE
    FROM SDC_SCHOOL_COLLECTION_STUDENT S, SDC_SCHOOL_COLLECTION_STUDENT_VALIDATION_ISSUE I
    WHERE S.SDC_SCHOOL_COLLECTION_STUDENT_ID = I.SDC_SCHOOL_COLLECTION_STUDENT_ID
    AND S.SDC_SCHOOL_COLLECTION_ID = :sdcSchoolCollectionID
    AND I.VALIDATION_ISSUE_SEVERITY_CODE = :validationIssueSeverityCode
    AND S.SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE NOT IN ('DELETED')
    GROUP BY I.SDC_SCHOOL_COLLECTION_STUDENT_ID, I.VALIDATION_ISSUE_CODE) as SUBQUERY
    """, nativeQuery= true)
  long getCountByValidationIssueSeverityCodeAndSdcSchoolCollectionID(String validationIssueSeverityCode, UUID sdcSchoolCollectionID);

  long countBySdcSchoolCollection_SdcSchoolCollectionID(UUID sdcSchoolCollectionID);

  @Query(value = """
    SELECT COUNT(*) FROM SDC_SCHOOL_COLLECTION_STUDENT SSCS 
    WHERE SSCS.sdc_school_collection_id = :sdcSchoolID 
    AND SSCS.STUDENT_PEN=:studentPen""", nativeQuery = true)
  Long countForDuplicateStudentPENs(UUID sdcSchoolID, String studentPen);

  @Query(value="""
    SELECT stud FROM SdcSchoolCollectionStudentEntity stud WHERE stud.sdcSchoolCollectionStudentID 
    NOT IN (SELECT saga.sdcSchoolCollectionStudentID FROM SdcSagaEntity saga WHERE saga.status != 'COMPLETED') 
    AND stud.sdcSchoolCollectionStudentStatusCode = 'LOADED' 
    order by stud.createDate 
    LIMIT :numberOfStudentsToProcess""")
  List<SdcSchoolCollectionStudentEntity> findTopLoadedStudentForProcessing(String numberOfStudentsToProcess);

  List<SdcSchoolCollectionStudentEntity> findTop100BySdcSchoolCollectionStudentStatusCodeOrderByCreateDate(String sdcSchoolCollectionStudentStatusCode);

  Optional<SdcSchoolCollectionStudentEntity> findBySdcSchoolCollectionStudentIDAndSdcSchoolCollection_SdcSchoolCollectionID(UUID sdcSchoolCollectionStudentID, UUID sdcSchoolCollectionID);

  long countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(UUID assignedStudentId, List<UUID> sdcSchoolCollectionID, String numberOfCourses);

  long countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(UUID assignedStudentId, List<UUID> sdcSchoolCollectionID);

  long countAllByAssignedStudentIdAndEnrolledGradeCodeAndCreateDateBetween(UUID assignedStudentId, String enrolledGradeCode, LocalDateTime startDate, LocalDateTime endDate);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(CASE WHEN s.isSchoolAged = true THEN 1 END) AS schoolAgedHeadcount, " +
          "SUM(CASE WHEN s.isSchoolAged = true AND s.fte > 0 THEN 1 ELSE 0 END) AS schoolAgedEligibleForFte, " +
          "SUM(CASE WHEN s.isSchoolAged = true THEN s.fte ELSE 0 END) AS schoolAgedFteTotal, " +
          "COUNT(CASE WHEN s.isAdult = true THEN 1 END) AS adultHeadcount, " +
          "SUM(CASE WHEN s.isAdult = true AND s.fte > 0 THEN 1 ELSE 0 END) AS adultEligibleForFte, " +
          "SUM(CASE WHEN s.isAdult = true THEN s.fte ELSE 0 END) AS adultFteTotal, " +
          "COUNT(s) AS totalHeadcount, " +
          "SUM(CASE WHEN s.fte > 0 THEN 1 ELSE 0 END) AS totalEligibleForFte, " +
          "SUM(s.fte) AS totalFteTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<EnrollmentHeadcountResult> getSectionHeadcountsBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);
}
