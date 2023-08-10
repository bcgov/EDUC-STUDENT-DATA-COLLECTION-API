package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SdcSchoolCollectionStudentRepository extends JpaRepository<SdcSchoolCollectionStudentEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentEntity> {
  List<SdcSchoolCollectionStudentEntity> findAllBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);

  long countBySdcSchoolCollectionStudentStatusCode(String sdcSchoolCollectionStudentStatusCode);
  long countBySdcSchoolCollectionStudentStatusCodeAndSdcSchoolCollectionID(String sdcSchoolCollectionStudentStatusCode, UUID sdcSchoolCollectionID);

@Query(value = """
    SELECT COUNT(*) FROM (SELECT I.SDC_SCHOOL_COLLECTION_STUDENT_ID, COUNT(I.VALIDATION_ISSUE_SEVERITY_CODE), I.VALIDATION_ISSUE_CODE
    FROM SDC_SCHOOL_COLLECTION_STUDENT S, SDC_SCHOOL_COLLECTION_STUDENT_VALIDATION_ISSUE I
    WHERE S.SDC_SCHOOL_COLLECTION_STUDENT_ID = I.SDC_SCHOOL_COLLECTION_STUDENT_ID
    AND S.SDC_SCHOOL_COLLECTION_ID = :sdcSchoolCollectionID
    AND I.VALIDATION_ISSUE_SEVERITY_CODE = :validationIssueSeverityCode
    GROUP BY I.SDC_SCHOOL_COLLECTION_STUDENT_ID, I.VALIDATION_ISSUE_CODE) as SUBQUERY
    """, nativeQuery= true)
  long getCountByValidationIssueSeverityCodeAndSdcSchoolCollectionID(String validationIssueSeverityCode, UUID sdcSchoolCollectionID);

  long countBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);

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

  @Query(value = """
    UPDATE SDC_SCHOOL_COLLECTION_STUDENT
    SET FRENCH_PROGRAM_NON_ELIG_REASON_CODE = NULL,
        ELL_NON_ELIG_REASON_CODE = NULL,
        INDIGENOUS_SUPPORT_PROGRAM_NON_ELIG_REASON_CODE = NULL,
        CAREER_PROGRAM_NON_ELIG_REASON_CODE = NULL,
        SPECIAL_EDUCATION_NON_ELIG_REASON_CODE = NULL
    WHERE SDC_SCHOOL_COLLECTION_STUDENT_ID = :sdcSchoolCollectionID;
    """)
  void deleteSdcStudentEligibilityErrorReasons(UUID sdcSchoolCollectionStudentID);

  List<SdcSchoolCollectionStudentEntity> findTop100BySdcSchoolCollectionStudentStatusCodeOrderByCreateDate(String sdcSchoolCollectionStudentStatusCode);

  Optional<SdcSchoolCollectionStudentEntity> findBySdcSchoolCollectionStudentIDAndSdcSchoolCollectionID(UUID sdcSchoolCollectionStudentID, UUID sdcSchoolCollectionID);

  long countByAssignedStudentIdAndSdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(UUID assignedStudentId, List<UUID> sdcSchoolCollectionID, String numberOfCourses);

  long countAllByAssignedStudentIdAndSdcSchoolCollectionIDIn(UUID assignedStudentId, List<UUID> sdcSchoolCollectionID);

  long countAllByAssignedStudentIdAndEnrolledGradeCodeAndCreateDateBetween(UUID assignedStudentId, String enrolledGradeCode, LocalDateTime startDate, LocalDateTime endDate);
}
