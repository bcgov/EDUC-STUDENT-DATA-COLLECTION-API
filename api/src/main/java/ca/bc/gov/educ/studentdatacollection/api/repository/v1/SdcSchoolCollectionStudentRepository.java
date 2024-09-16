package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ICountValidationIssuesBySeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ProgressCountsForDistrict;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionStudentRepository extends JpaRepository<SdcSchoolCollectionStudentEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentEntity> {

  List<SdcSchoolCollectionStudentEntity> findAllBySdcSchoolCollection_SdcSchoolCollectionID(UUID sdcSchoolCollectionID);

  List<SdcSchoolCollectionStudentEntity> findAllBySdcSchoolCollectionStudentIDIn(List<UUID> sdcSchoolCollectionStudentIDs);

  @Query(value = """  
    SELECT stud
    FROM SdcSchoolCollectionStudentEntity stud
    WHERE assignedStudentId IN (SELECT assignedStudentId
                FROM SdcSchoolCollectionStudentEntity
                where sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID
                and assignedStudentId is not null
                and sdcSchoolCollectionStudentStatusCode != 'DELETED'
                GROUP BY assignedStudentId
                HAVING COUNT(assignedStudentId) > 1)
    and sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID
    and sdcSchoolCollectionStudentStatusCode != 'DELETED'
    and assignedStudentId is not null
    """)
  List<SdcSchoolCollectionStudentEntity> findAllDuplicateStudentsInSdcSchoolCollection(UUID sdcSchoolCollectionID);

  @Query(value = """  
    SELECT validationIssue.sdcSchoolCollectionStudentEntity
    FROM SdcSchoolCollectionStudentValidationIssueEntity validationIssue
    WHERE validationIssue.sdcSchoolCollectionStudentEntity.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID
    and validationIssue.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentStatusCode != 'DELETED'
    """)
  List<SdcSchoolCollectionStudentEntity> findAllStudentsWithErrorsWarningInfoBySchoolCollectionID(UUID sdcSchoolCollectionID);

  @Query(value = """  
    SELECT validationIssue.sdcSchoolCollectionStudentEntity
    FROM SdcSchoolCollectionStudentValidationIssueEntity validationIssue
    WHERE validationIssue.sdcSchoolCollectionStudentEntity.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID
    and validationIssue.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentStatusCode != 'DELETED'
    """)
  List<SdcSchoolCollectionStudentEntity> findAllStudentsWithErrorsWarningInfoByDistrictCollectionID(UUID sdcDistrictCollectionID);

  long countBySdcSchoolCollection_SdcSchoolCollectionIDAndSdcSchoolCollectionStudentStatusCode(UUID sdcSchoolCollectionID, String sdcSchoolCollectionStatusCode);

  @Query("SELECT " +
          " sscs.sdcSchoolCollection.schoolID as schoolID, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' THEN 1 END) as kindHCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'KF' THEN 1 END) as kindFCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '01' THEN 1 END) as grade1Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '02' THEN 1 END) as grade2Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '03' THEN 1 END) as grade3Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '04' THEN 1 END) as grade4Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '05' THEN 1 END) as grade5Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '06' THEN 1 END) as grade6Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '07' THEN 1 END) as grade7Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '08' THEN 1 END) as grade8Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '09' THEN 1 END) as grade9Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '10' THEN 1 END) as grade10Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '11' THEN 1 END) as grade11Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '12' THEN 1 END) as grade12Count " +
          " FROM SdcSchoolCollectionStudentEntity sscs " +
          " WHERE sscs.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          " AND sscs.sdcSchoolCollection.collectionEntity.collectionID = :collectionID " +
          " GROUP BY sscs.sdcSchoolCollection.schoolID ")
  List<SchoolHeadcountResult> getAllEnrollmentHeadcountsByCollectionId(@Param("collectionID") UUID collectionID);

  @Query("SELECT " +
          " sscs.sdcSchoolCollection.schoolID as schoolID, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' THEN 1 END) as kindHCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'KF' THEN 1 END) as kindFCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '01' THEN 1 END) as grade1Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '02' THEN 1 END) as grade2Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '03' THEN 1 END) as grade3Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '04' THEN 1 END) as grade4Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '05' THEN 1 END) as grade5Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '06' THEN 1 END) as grade6Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '07' THEN 1 END) as grade7Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '08' THEN 1 END) as grade8Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '09' THEN 1 END) as grade9Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '10' THEN 1 END) as grade10Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '11' THEN 1 END) as grade11Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '12' THEN 1 END) as grade12Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'EU' THEN 1 END) as gradeEUCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'SU' THEN 1 END) as gradeSUCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'GA' THEN 1 END) as gradeGACount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'HS' THEN 1 END) as gradeHSCount " +
          " FROM SdcSchoolCollectionStudentEntity sscs " +
          " WHERE sscs.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          " AND sscs.sdcSchoolCollection.collectionEntity.collectionID = :collectionID " +
          " AND sscs.sdcSchoolCollection.sdcDistrictCollectionID is null " +
          " GROUP BY sscs.sdcSchoolCollection.schoolID ")
  List<IndySchoolHeadcountResult> getAllIndyEnrollmentHeadcountsByCollectionId(@Param("collectionID") UUID collectionID);

  @Query(value = """  
    SELECT stud
    FROM SdcSchoolCollectionStudentLightEntity stud, SdcSchoolCollectionEntity school, SdcDistrictCollectionEntity dist
    WHERE stud.assignedStudentId IN (SELECT innerStud.assignedStudentId
                FROM SdcSchoolCollectionStudentLightEntity innerStud, SdcSchoolCollectionEntity sdcSchool, SdcDistrictCollectionEntity sdcDist
                where sdcDist.sdcDistrictCollectionID = :sdcDistrictCollectionID
                and sdcDist.sdcDistrictCollectionID = sdcSchool.sdcDistrictCollectionID
                and sdcSchool.sdcSchoolCollectionID = innerStud.sdcSchoolCollectionEntity.sdcSchoolCollectionID
                and innerStud.sdcSchoolCollectionStudentStatusCode != 'DELETED'
                and innerStud.assignedStudentId is not null
                GROUP BY innerStud.assignedStudentId
                HAVING COUNT(innerStud.assignedStudentId) > 1)
    and dist.sdcDistrictCollectionID = :sdcDistrictCollectionID
    and dist.sdcDistrictCollectionID = school.sdcDistrictCollectionID
    and school.sdcSchoolCollectionID = stud.sdcSchoolCollectionEntity.sdcSchoolCollectionID
    and stud.sdcSchoolCollectionStudentStatusCode != 'DELETED'
    and stud.assignedStudentId is not null
    """)
  List<SdcSchoolCollectionStudentLightEntity> findAllInDistrictDuplicateStudentsInSdcDistrictCollection(UUID sdcDistrictCollectionID);

  @Query(value = """  
    SELECT stud
    FROM SdcSchoolCollectionStudentLightEntity stud, SdcSchoolCollectionEntity school
    WHERE stud.assignedStudentId IN (SELECT innerStud.assignedStudentId
                FROM SdcSchoolCollectionStudentLightEntity innerStud, SdcSchoolCollectionEntity sdcSchool
                where sdcSchool.collectionEntity.collectionID = :collectionID
                and sdcSchool.sdcSchoolCollectionID = innerStud.sdcSchoolCollectionEntity.sdcSchoolCollectionID
                and innerStud.sdcSchoolCollectionStudentStatusCode != 'DELETED'
                and innerStud.assignedStudentId is not null
                GROUP BY innerStud.assignedStudentId
                HAVING COUNT(innerStud.assignedStudentId) > 1)
    and school.sdcSchoolCollectionID = stud.sdcSchoolCollectionEntity.sdcSchoolCollectionID
    and school.collectionEntity.collectionID = :collectionID
    and stud.sdcSchoolCollectionStudentStatusCode != 'DELETED'
    and stud.assignedStudentId is not null
    """)
  List<SdcSchoolCollectionStudentLightEntity> findAllInProvinceDuplicateStudentsInCollection(UUID collectionID);

  long countBySdcSchoolCollectionStudentStatusCodeAndSdcSchoolCollection_SdcSchoolCollectionID(String sdcSchoolCollectionStudentStatusCode, UUID sdcSchoolCollectionID);

  @Query(value = """
    SELECT SUB.sevCode as severityCode, SUM(SUB.issueCode) as total
    FROM (SELECT S.SDC_SCHOOL_COLLECTION_STUDENT_ID as id, I.VALIDATION_ISSUE_SEVERITY_CODE as sevCode, COUNT(DISTINCT I.VALIDATION_ISSUE_CODE) as issueCode
    FROM SDC_SCHOOL_COLLECTION_STUDENT S, SDC_SCHOOL_COLLECTION_STUDENT_VALIDATION_ISSUE I
    WHERE S.SDC_SCHOOL_COLLECTION_STUDENT_ID = I.SDC_SCHOOL_COLLECTION_STUDENT_ID
    AND S.SDC_SCHOOL_COLLECTION_ID = :sdcSchoolCollectionID
    AND S.SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE != 'DELETED'
    GROUP BY S.SDC_SCHOOL_COLLECTION_STUDENT_ID, I.VALIDATION_ISSUE_SEVERITY_CODE) SUB
    GROUP BY SUB.sevCode
    """, nativeQuery = true)
  List<ICountValidationIssuesBySeverityCode> getCountByValidationIssueSeverityCodeAndSdcSchoolCollectionID(UUID sdcSchoolCollectionID);

  long countBySdcSchoolCollection_SdcSchoolCollectionID(UUID sdcSchoolCollectionID);


  @Query(value = """
     SELECT new ca.bc.gov.educ.studentdatacollection.api.struct.v1.ProgressCountsForDistrict(SSC.sdcSchoolCollectionID,
     SSC.uploadDate,
     SSC.uploadFileName,
     SSC.schoolID,
     COUNT(SSCS.sdcSchoolCollectionStudentID) as totalCount,
     SUM(CASE
       WHEN SSCS.sdcSchoolCollectionStudentStatusCode = 'LOADED' THEN 1
       ELSE 0
     END) AS loadedCount)
     FROM SdcSchoolCollectionStudentEntity SSCS, SdcSchoolCollectionEntity SSC
     WHERE SSC.sdcSchoolCollectionID = SSCS.sdcSchoolCollection.sdcSchoolCollectionID
     AND SSC.sdcDistrictCollectionID = :sdcDistrictCollectionID
     GROUP BY SSC.sdcSchoolCollectionID, SSC.uploadDate , SSC.uploadFileName , SSC.schoolID
    """)
  List<ProgressCountsForDistrict> getProgressCountsBySdcDistrictCollectionID(UUID sdcDistrictCollectionID);

  @Query(value = """
    SELECT COUNT(*) FROM SDC_SCHOOL_COLLECTION_STUDENT SSCS 
    WHERE SSCS.sdc_school_collection_id = :sdcSchoolID 
    AND SSCS.SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE != 'DELETED'
    AND SSCS.STUDENT_PEN=:studentPen""", nativeQuery = true)
  Long countForDuplicateStudentPENs(UUID sdcSchoolID, String studentPen);

  @Query(value="""
    SELECT stud FROM SdcSchoolCollectionStudentEntity stud WHERE stud.sdcSchoolCollectionStudentID
    NOT IN (SELECT saga.sdcSchoolCollectionStudentID FROM SdcSagaEntity saga WHERE saga.status != 'COMPLETED'
    AND saga.sdcSchoolCollectionStudentID IS NOT NULL)
    AND stud.sdcSchoolCollectionStudentStatusCode = 'LOADED'
    order by stud.createDate
    LIMIT :numberOfStudentsToProcess""")
  List<SdcSchoolCollectionStudentEntity> findTopLoadedStudentForProcessing(String numberOfStudentsToProcess);

  long countByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(List<UUID> assignedStudentId, List<UUID> sdcSchoolCollectionID, String numberOfCourses);

  @Query("""
       SELECT COUNT(s) FROM SdcSchoolCollectionStudentEntity s
       WHERE s.assignedStudentId IN :assignedStudentId
       AND s.sdcSchoolCollection.sdcSchoolCollectionID IN :sdcSchoolCollectionIDs
       """)
  long countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(
          @Param("assignedStudentId") List<UUID> assignedStudentId,
          @Param("sdcSchoolCollectionIDs") List<UUID> sdcSchoolCollectionIDs);

  @Query("""
       SELECT COUNT(s) FROM SdcSchoolCollectionStudentEntity s
       WHERE s.assignedStudentId IN :assignedStudentId
       AND s.sdcSchoolCollection.sdcSchoolCollectionID IN :sdcSchoolCollectionIDs
       AND s.enrolledGradeCode <> 'HS'
       """)
  long countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(
          @Param("assignedStudentId") List<UUID> assignedStudentId,
          @Param("sdcSchoolCollectionIDs") List<UUID> sdcSchoolCollectionIDs);

  @Query("""
       SELECT COUNT(s) FROM SdcSchoolCollectionStudentEntity s
       WHERE s.assignedStudentId IN :assignedStudentId
       AND s.sdcSchoolCollection.sdcSchoolCollectionID IN :sdcSchoolCollectionIDs
       AND s.enrolledGradeCode <> 'HS'
       AND s.fte > 0
       """)
  long countAllByAssignedStudentIdInAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(
          @Param("assignedStudentId") List<UUID> assignedStudentId,
          @Param("sdcSchoolCollectionIDs") List<UUID> sdcSchoolCollectionIDs);

  long countAllByAssignedStudentIdInAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(List<UUID> assignedStudentId, String enrolledGradeCode, List<UUID> sdcSchoolCollectionID);

  @Query("""
       SELECT COUNT(s) FROM SdcSchoolCollectionStudentEntity s
       WHERE s.assignedStudentId IN :assignedStudentId
       AND s.sdcSchoolCollection.sdcSchoolCollectionID IN :sdcSchoolCollectionIDs
       AND s.fte > 0
       """)
  long countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInWithNonZeroFTE(
          @Param("assignedStudentId") List<UUID> assignedStudentId,
          @Param("sdcSchoolCollectionIDs") List<UUID> sdcSchoolCollectionIDs);

  @Query("""
       SELECT COUNT(s) FROM SdcSchoolCollectionStudentEntity s
       WHERE s.assignedStudentId IN :assignedStudentId
       AND s.sdcSchoolCollection.sdcSchoolCollectionID IN :sdcSchoolCollectionIDs
       AND s.fte > 0
       AND s.enrolledGradeCode IN ('KH', 'KF', '01', '02', '03', '04', '05', '06', '07', '08', '09')
       """)
  long countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInWithNonZeroFTEAndInGradeKto9(
          @Param("assignedStudentId") List<UUID> assignedStudentId,
          @Param("sdcSchoolCollectionIDs") List<UUID> sdcSchoolCollectionIDs);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(CASE WHEN s.isSchoolAged = false AND s.isAdult = false THEN 1 END) AS underSchoolAgedHeadcount, " +
          "SUM(CASE WHEN s.isSchoolAged = false AND s.isAdult = false AND  s.fte > 0 THEN 1 ELSE 0 END) AS underSchoolAgedEligibleForFte, " +
          "SUM(CASE WHEN s.isSchoolAged = false AND s.isAdult = false THEN s.fte ELSE 0 END) AS underSchoolAgedFteTotal, " +
          "COUNT(CASE WHEN s.isSchoolAged = true THEN 1 END) AS schoolAgedHeadcount, " +
          "SUM(CASE WHEN s.isSchoolAged = true AND s.fte > 0 THEN 1 ELSE 0 END) AS schoolAgedEligibleForFte, " +
          "SUM(CASE WHEN s.isSchoolAged = true THEN s.fte ELSE 0 END) AS schoolAgedFteTotal, " +
          "COUNT(CASE WHEN s.isAdult = true THEN 1 END) AS adultHeadcount, " +
          "SUM(CASE WHEN s.isAdult = true AND s.fte > 0 THEN 1 ELSE 0 END) AS adultEligibleForFte, " +
          "SUM(CASE WHEN s.isAdult = true THEN s.fte ELSE 0 END) AS adultFteTotal, " +
          "COUNT(s) AS totalHeadcount, " +
          "SUM(CASE WHEN s.fte > 0 THEN 1 ELSE 0 END) AS totalEligibleForFte, " +
          "SUM(CASE WHEN s.fte IS NOT NULL THEN s.fte ELSE 0 END) AS totalFteTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<EnrollmentHeadcountResult> getEnrollmentHeadcountsBySdcSchoolCollectionId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '08' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '08' THEN s.sdcSchoolCollectionStudentID END) AS adultCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '08' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '11' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '11' THEN s.sdcSchoolCollectionStudentID END) AS adultEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '11' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '14' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedLateFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '14' THEN s.sdcSchoolCollectionStudentID END) AS adultLateFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '14' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalLateFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode IN ('08', '11', '14') THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedTotals, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode IN ('08', '11', '14') THEN s.sdcSchoolCollectionStudentID END) AS adultTotals, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode IN ('08', '11', '14') AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalTotals " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<FrenchHeadcountResult> getFrenchHeadcountsBySdcSchoolCollectionId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '05' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedFrancophone, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '05' THEN s.sdcSchoolCollectionStudentID END) AS adultFrancophone, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '05' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalFrancophone, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '08' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '08' THEN s.sdcSchoolCollectionStudentID END) AS adultCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '08' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '11' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '11' THEN s.sdcSchoolCollectionStudentID END) AS adultEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '11' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '14' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedLateFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '14' THEN s.sdcSchoolCollectionStudentID END) AS adultLateFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '14' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalLateFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode IN ('05', '08', '11', '14') THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedTotals, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode IN ('05', '08', '11', '14') THEN s.sdcSchoolCollectionStudentID END) AS adultTotals, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode IN ('05', '08', '11', '14') AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalTotals " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<FrenchCombinedHeadcountResult> getFrenchHeadcountsBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '05' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedFrancophone, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '05' THEN s.sdcSchoolCollectionStudentID END) AS adultFrancophone, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '05' AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalFrancophone, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '08' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '08' THEN s.sdcSchoolCollectionStudentID END) AS adultCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '08' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '11' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '11' THEN s.sdcSchoolCollectionStudentID END) AS adultEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '11' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '14' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedLateFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '14' THEN s.sdcSchoolCollectionStudentID END) AS adultLateFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '14' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalLateFrench, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode IN ('05', '08', '11', '14') THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedTotals, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode IN ('05', '08', '11', '14') THEN s.sdcSchoolCollectionStudentID END) AS adultTotals, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode IN ('05', '08', '11', '14') AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalTotals " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode " +
          "ORDER BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode")
  List<FrenchCombinedHeadcountResult> getFrenchHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(DISTINCT CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '05' THEN s.sdcSchoolCollectionStudentID END) AS schoolAgedFrancophone, " +
          "COUNT(DISTINCT CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '05' THEN s.sdcSchoolCollectionStudentID END) AS adultFrancophone, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '05' AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalFrancophone " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<CsfFrenchHeadcountResult> getCsfFrenchHeadcountsBySdcSchoolCollectionId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalEllStudents, " +
          "COUNT(DISTINCT CASE WHEN s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalEligibleEllStudents, " +
          "COUNT(DISTINCT CASE WHEN s.ellNonEligReasonCode IS NOT NULL AND ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalIneligibleEllStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN SdcStudentEllEntity ell " +
          "ON s.assignedStudentId = ell.studentID " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionId " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<EllHeadcountResult> getEllHeadcountsBySdcSchoolCollectionId(@Param("sdcSchoolCollectionId") UUID sdcSchoolCollectionId);

  @Query("SELECT " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' AND s.ellNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligibleStudents, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' AND s.ellNonEligReasonCode IS NOT NULL THEN s.sdcSchoolCollectionStudentID END) AS ineligibleStudents, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS reportedStudents, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' AND ell.yearsInEll < 6 THEN s.sdcSchoolCollectionStudentID END) AS oneToFiveYears, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' AND ell.yearsInEll > 5 THEN s.sdcSchoolCollectionStudentID END) AS sixPlusYears, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN SdcStudentEllEntity ell " +
          "ON s.assignedStudentId = ell.studentID " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionId " +
          "AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'")
  EllHeadcountHeaderResult getEllHeadersBySchoolId(@Param("sdcSchoolCollectionId") UUID sdcSchoolCollectionId);

  @Query(value="""
           SELECT SSCS.numberOfCourses FROM SdcSchoolCollectionEntity SSC, CollectionEntity C, SdcSchoolCollectionStudentEntity SSCS WHERE SSC.schoolID = :schoolID
            AND C.collectionID = SSC.collectionEntity.collectionID
            AND SSC.sdcSchoolCollectionID = SSCS.sdcSchoolCollection.sdcSchoolCollectionID
            AND SSCS.assignedStudentId IN :assignedStudentID
            AND C.openDate < :currentOpenDate
            AND C.openDate >= (SELECT C.openDate FROM CollectionEntity C WHERE C.collectionTypeCode = :collectionTypeCode AND EXTRACT(YEAR FROM C.openDate) = :targetYear)
            """)
  List<String> getCollectionHistory(UUID schoolID, List<UUID> assignedStudentID, LocalDateTime currentOpenDate, String collectionTypeCode, Integer targetYear);

  @Query("SELECT " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '08' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '08' THEN s.sdcSchoolCollectionStudentID END) AS reportedCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '11' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '11' THEN s.sdcSchoolCollectionStudentID END) AS reportedEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '14' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalLateFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '14' THEN s.sdcSchoolCollectionStudentID END) AS reportedLateFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '05' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalFrancophone, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '05' THEN s.sdcSchoolCollectionStudentID END) AS reportedFrancophone, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'")
  FrenchHeadcountHeaderResult getFrenchHeadersBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '08' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '08' THEN s.sdcSchoolCollectionStudentID END) AS reportedCoreFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '11' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '11' THEN s.sdcSchoolCollectionStudentID END) AS reportedEarlyFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '14' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalLateFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '14' THEN s.sdcSchoolCollectionStudentID END) AS reportedLateFrench, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '05' AND s.frenchProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS totalFrancophone, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '05' THEN s.sdcSchoolCollectionStudentID END) AS reportedFrancophone, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'")
  FrenchCombinedHeadcountHeaderResult getFrenchHeadersByDistrictId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '40' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligCareerPrep, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS reportedCareerPrep, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '41' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligCoopEduc, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS reportedCoopEduc, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '42' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligApprentice, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS reportedApprentice, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '43' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligTechOrYouth, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS reportedTechOrYouth, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'")
  CareerHeadcountHeaderResult getCareerHeadersBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXA, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXB, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXC, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXD, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXE, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXF, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXG, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXH, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXA, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXB, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXC, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXD, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXE, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXF, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXG, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXH, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXA, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXB, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXC, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXD, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXE, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXF, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXG, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXH, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXA, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXB, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXC, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXD, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXE, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXF, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXG, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXH, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXA, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXB, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXC, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXD, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXE, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXF, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXG, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXH, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<CareerHeadcountResult> getCareerHeadcountsBySdcSchoolCollectionId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '29' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS indigenousLanguageTotal, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '33' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS indigenousSupportTotal, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '36' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS otherProgramTotal, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode in ('29', '33', '36') AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allSupportProgramTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<IndigenousHeadcountResult> getIndigenousHeadcountsBySdcSchoolCollectionId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '29' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS indigenousLanguageTotal, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '33' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS indigenousSupportTotal, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '36' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS otherProgramTotal, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode in ('29', '33', '36') AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allSupportProgramTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<IndigenousHeadcountResult> getIndigenousHeadcountsBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '29' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS indigenousLanguageTotal, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '33' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS indigenousSupportTotal, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '36' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS otherProgramTotal, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode in ('29', '33', '36') AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allSupportProgramTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode " +
          "ORDER BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode")
  List<IndigenousHeadcountResult> getIndigenousHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query(value = """
    SELECT
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '29' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligIndigenousLanguage,
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '29' THEN s.sdcSchoolCollectionStudentID END) AS reportedIndigenousLanguage,
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '33' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligIndigenousSupport,
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '33' THEN s.sdcSchoolCollectionStudentID END) AS reportedIndigenousSupport,
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '36' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligOtherProgram,
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '36' THEN s.sdcSchoolCollectionStudentID END) AS reportedOtherProgram,
    COUNT(DISTINCT CASE WHEN s.nativeAncestryInd = 'Y' THEN s.sdcSchoolCollectionStudentID END) AS studentsWithIndigenousAncestry,
    COUNT(DISTINCT CASE WHEN s.schoolFundingCode ='20' THEN s.sdcSchoolCollectionStudentID END) AS studentsWithFundingCode20,
    COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents
    FROM SdcSchoolCollectionStudentEntity s LEFT JOIN s.sdcStudentEnrolledProgramEntities ep
    WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID
    AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'
    """)
  IndigenousHeadcountHeaderResult getIndigenousHeadersBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query(value = """
    SELECT
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '29' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligIndigenousLanguage,
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '29' THEN s.sdcSchoolCollectionStudentID END) AS reportedIndigenousLanguage,
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '33' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligIndigenousSupport,
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '33' THEN s.sdcSchoolCollectionStudentID END) AS reportedIndigenousSupport,
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '36' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligOtherProgram,
    COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '36' THEN s.sdcSchoolCollectionStudentID END) AS reportedOtherProgram,
    COUNT(DISTINCT CASE WHEN s.nativeAncestryInd = 'Y' THEN s.sdcSchoolCollectionStudentID END) AS studentsWithIndigenousAncestry,
    COUNT(DISTINCT CASE WHEN s.schoolFundingCode ='20' THEN s.sdcSchoolCollectionStudentID END) AS studentsWithFundingCode20,
    COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents
    FROM SdcSchoolCollectionStudentEntity s LEFT JOIN s.sdcStudentEnrolledProgramEntities ep
    WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID
    AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'
    """)
  IndigenousHeadcountHeaderResult getIndigenousHeadersByDistrictId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleA, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' THEN 1 END) AS reportedA, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleB, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' THEN 1 END) AS reportedB, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleC, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' THEN 1 END) AS reportedC, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleD, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' THEN 1 END) AS reportedD, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleE, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' THEN 1 END) AS reportedE, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleF, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' THEN 1 END) AS reportedF, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleG, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' THEN 1 END) AS reportedG, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleH, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' THEN 1 END) AS reportedH, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleK, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' THEN 1 END) AS reportedK, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleP, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' THEN 1 END) AS reportedP, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleQ, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' THEN 1 END) AS reportedQ, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleR, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' THEN 1 END) AS reportedR " +
    "FROM SdcSchoolCollectionStudentEntity s " +
    "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
    "AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'")
  SpecialEdHeadcountHeaderResult getSpecialEdHeadersBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
    "s.enrolledGradeCode AS enrolledGradeCode, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B') THEN 1 END) AS levelOnes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' THEN 1 END) AS specialEdACodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' THEN 1 END) AS specialEdBCodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('C', 'D', 'E', 'F', 'G') THEN 1 END) AS levelTwos, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' THEN 1 END) AS specialEdCCodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' THEN 1 END) AS specialEdDCodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' THEN 1 END) AS specialEdECodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' THEN 1 END) AS specialEdFCodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' THEN 1 END) AS specialEdGCodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('H') THEN 1 END) AS levelThrees, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' THEN 1 END) AS specialEdHCodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('K', 'P', 'Q', 'R') THEN 1 END) AS otherLevels, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' THEN 1 END) AS specialEdKCodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' THEN 1 END) AS specialEdPCodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' THEN 1 END) AS specialEdQCodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' THEN 1 END) AS specialEdRCodes, " +
    "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'P', 'Q', 'R') THEN 1 END) AS allLevels " +
    "FROM SdcSchoolCollectionStudentEntity s " +
    "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
    "AND s.specialEducationNonEligReasonCode IS NULL " +
    "GROUP BY s.enrolledGradeCode " +
    "ORDER BY s.enrolledGradeCode")
  List<SpecialEdHeadcountResult> getSpecialEdHeadcountsBySdcSchoolCollectionId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.bandCode AS bandCode, " +
          "SUM(s.fte) AS fteTotal, " +
          "COUNT(*) AS headcount " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "AND s.bandCode IS NOT NULL " +
          "GROUP BY s.bandCode " +
          "ORDER BY s.bandCode")
  List<BandResidenceHeadcountResult> getBandResidenceHeadcountsBySdcSchoolCollectionId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.bandCode AS bandCode, " +
          "SUM(s.fte) AS fteTotal, " +
          "COUNT(*) AS headcount " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "AND s.bandCode IS NOT NULL " +
          "GROUP BY s.bandCode " +
          "ORDER BY s.bandCode")
  List<BandResidenceHeadcountResult> getBandResidenceHeadcountsBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.bandCode AS bandCode, " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "SUM(s.fte) AS fteTotal, " +
          "COUNT(*) AS headcount " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "AND s.bandCode IS NOT NULL " +
          "GROUP BY s.sdcSchoolCollection.schoolID, s.bandCode " +
          "ORDER BY s.sdcSchoolCollection.schoolID, s.bandCode")
  List<BandResidenceHeadcountResult> getBandResidenceHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(CASE WHEN s.isSchoolAged = false AND s.isAdult = false THEN 1 END) AS underSchoolAgedHeadcount, " +
          "SUM(CASE WHEN s.isSchoolAged = false AND s.isAdult = false AND  s.fte > 0 THEN 1 ELSE 0 END) AS underSchoolAgedEligibleForFte, " +
          "SUM(CASE WHEN s.isSchoolAged = false AND s.isAdult = false THEN s.fte ELSE 0 END) AS underSchoolAgedFteTotal, " +
          "COUNT(CASE WHEN s.isSchoolAged = true THEN 1 END) AS schoolAgedHeadcount, " +
          "SUM(CASE WHEN s.isSchoolAged = true AND s.fte > 0 THEN 1 ELSE 0 END) AS schoolAgedEligibleForFte, " +
          "SUM(CASE WHEN s.isSchoolAged = true THEN s.fte ELSE 0 END) AS schoolAgedFteTotal, " +
          "COUNT(CASE WHEN s.isAdult = true THEN 1 END) AS adultHeadcount, " +
          "SUM(CASE WHEN s.isAdult = true AND s.fte > 0 THEN 1 ELSE 0 END) AS adultEligibleForFte, " +
          "SUM(CASE WHEN s.isAdult = true THEN s.fte ELSE 0 END) AS adultFteTotal, " +
          "COUNT(s) AS totalHeadcount, " +
          "SUM(CASE WHEN s.fte > 0 THEN 1 ELSE 0 END) AS totalEligibleForFte, " +
          "SUM(CASE WHEN s.fte IS NOT NULL THEN s.fte ELSE 0 END) AS totalFteTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<EnrollmentHeadcountResult> getEnrollmentHeadcountsBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B') THEN 1 END) AS levelOnes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' THEN 1 END) AS specialEdACodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' THEN 1 END) AS specialEdBCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('C', 'D', 'E', 'F', 'G') THEN 1 END) AS levelTwos, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' THEN 1 END) AS specialEdCCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' THEN 1 END) AS specialEdDCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' THEN 1 END) AS specialEdECodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' THEN 1 END) AS specialEdFCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' THEN 1 END) AS specialEdGCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('H') THEN 1 END) AS levelThrees, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' THEN 1 END) AS specialEdHCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('K', 'P', 'Q', 'R') THEN 1 END) AS otherLevels, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' THEN 1 END) AS specialEdKCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' THEN 1 END) AS specialEdPCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' THEN 1 END) AS specialEdQCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' THEN 1 END) AS specialEdRCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'P', 'Q', 'R') THEN 1 END) AS allLevels " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "AND s.specialEducationNonEligReasonCode IS NULL " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<SpecialEdHeadcountResult> getSpecialEdHeadcountsBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleA, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' THEN 1 END) AS reportedA, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleB, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' THEN 1 END) AS reportedB, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleC, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' THEN 1 END) AS reportedC, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleD, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' THEN 1 END) AS reportedD, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleE, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' THEN 1 END) AS reportedE, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleF, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' THEN 1 END) AS reportedF, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleG, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' THEN 1 END) AS reportedG, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleH, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' THEN 1 END) AS reportedH, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleK, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' THEN 1 END) AS reportedK, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleP, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' THEN 1 END) AS reportedP, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleQ, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' THEN 1 END) AS reportedQ, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' AND s.specialEducationNonEligReasonCode IS NULL THEN 1 END) AS totalEligibleR, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' THEN 1 END) AS reportedR " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'")
  SpecialEdHeadcountHeaderResult getSpecialEdHeadersByDistrictId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B') THEN 1 END) AS levelOnes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' THEN 1 END) AS specialEdACodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' THEN 1 END) AS specialEdBCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('C', 'D', 'E', 'F', 'G') THEN 1 END) AS levelTwos, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' THEN 1 END) AS specialEdCCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' THEN 1 END) AS specialEdDCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' THEN 1 END) AS specialEdECodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' THEN 1 END) AS specialEdFCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' THEN 1 END) AS specialEdGCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('H') THEN 1 END) AS levelThrees, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' THEN 1 END) AS specialEdHCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('K', 'P', 'Q', 'R') THEN 1 END) AS otherLevels, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' THEN 1 END) AS specialEdKCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' THEN 1 END) AS specialEdPCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' THEN 1 END) AS specialEdQCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' THEN 1 END) AS specialEdRCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'P', 'Q', 'R') THEN 1 END) AS allLevels " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "AND s.specialEducationNonEligReasonCode IS NULL " +
          "GROUP BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode " +
          "ORDER BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode")
  List<SpecialEdHeadcountResult> getSpecialEdHeadcountsBySchoolIdAndBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' THEN 1 END) AS specialEdACodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdA, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' THEN 1 END) AS specialEdBCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdB, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' THEN 1 END) AS specialEdCCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdC, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' THEN 1 END) AS specialEdDCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdD, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' THEN 1 END) AS specialEdECodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdE, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' THEN 1 END) AS specialEdFCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdF, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' THEN 1 END) AS specialEdGCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdG, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' THEN 1 END) AS specialEdHCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdH, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' THEN 1 END) AS specialEdKCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdK, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' THEN 1 END) AS specialEdPCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdP, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' THEN 1 END) AS specialEdQCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdQ, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' THEN 1 END) AS specialEdRCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdR, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'P', 'Q', 'R') THEN 1 END) AS allLevels " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "AND s.specialEducationNonEligReasonCode IS NULL " +
          "GROUP BY s.sdcSchoolCollection.schoolID " +
          "ORDER BY s.sdcSchoolCollection.schoolID")
  List<SpecialEdHeadcountResult> getSpecialEdCategoryBySchoolIdAndSdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(CASE WHEN s.isSchoolAged = false AND s.isAdult = false THEN 1 END) AS underSchoolAgedHeadcount, " +
          "SUM(CASE WHEN s.isSchoolAged = false AND s.isAdult = false AND  s.fte > 0 THEN 1 ELSE 0 END) AS underSchoolAgedEligibleForFte, " +
          "SUM(CASE WHEN s.isSchoolAged = false AND s.isAdult = false THEN s.fte ELSE 0 END) AS underSchoolAgedFteTotal, " +
          "COUNT(CASE WHEN s.isSchoolAged = true THEN 1 END) AS schoolAgedHeadcount, " +
          "SUM(CASE WHEN s.isSchoolAged = true AND s.fte > 0 THEN 1 ELSE 0 END) AS schoolAgedEligibleForFte, " +
          "SUM(CASE WHEN s.isSchoolAged = true THEN s.fte ELSE 0 END) AS schoolAgedFteTotal, " +
          "COUNT(CASE WHEN s.isAdult = true THEN 1 END) AS adultHeadcount, " +
          "SUM(CASE WHEN s.isAdult = true AND s.fte > 0 THEN 1 ELSE 0 END) AS adultEligibleForFte, " +
          "SUM(CASE WHEN s.isAdult = true THEN s.fte ELSE 0 END) AS adultFteTotal, " +
          "COUNT(s) AS totalHeadcount, " +
          "SUM(CASE WHEN s.fte > 0 THEN 1 ELSE 0 END) AS totalEligibleForFte, " +
          "SUM(CASE WHEN s.fte IS NOT NULL THEN s.fte ELSE 0 END) AS totalFteTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode " +
          "ORDER BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode")
  List<EnrollmentHeadcountResult> getEnrollmentHeadcountsBySchoolIdAndBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXA, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXB, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXC, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXD, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXE, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXF, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXG, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationXH, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXA, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXB, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXC, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXD, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXE, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXF, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXG, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopXH, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXA, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXB, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXC, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXD, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXE, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXF, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXG, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeXH, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXA, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXB, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXC, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXD, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXE, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXF, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXG, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthXH, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXA, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXB, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXC, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXD, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXE, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXF, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXG, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allXH, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<CareerHeadcountResult> getCareerHeadcountsBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '40' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligCareerPrep, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS reportedCareerPrep, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '41' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligCoopEduc, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS reportedCoopEduc, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '42' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligApprentice, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS reportedApprentice, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '43' AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligTechOrYouth, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS reportedTechOrYouth, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'")
  CareerHeadcountHeaderResult getCareerHeadersBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode IN ('40', '41', '42', '43') THEN s.sdcSchoolCollectionStudentID END) AS allTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode " +
          "ORDER BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode")
  List<CareerHeadcountResult> getCareerHeadcountsBySchoolIdAndBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalEllStudents, " +
          "COUNT(DISTINCT CASE WHEN s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalEligibleEllStudents, " +
          "COUNT(DISTINCT CASE WHEN s.ellNonEligReasonCode IS NOT NULL AND ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalIneligibleEllStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<EllHeadcountResult> getEllHeadcountsBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS reportedStudents, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' AND s.ellNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligibleStudents, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' AND s.ellNonEligReasonCode IS NOT NULL THEN s.sdcSchoolCollectionStudentID END) AS ineligibleStudents, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED')")
  EllHeadcountHeaderResult getEllHeadersBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalEllStudents, " +
          "COUNT(DISTINCT CASE WHEN s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalEligibleEllStudents, " +
          "COUNT(DISTINCT CASE WHEN s.ellNonEligReasonCode IS NOT NULL AND ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalIneligibleEllStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode " +
          "ORDER BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode")
  List<EllHeadcountResult> getEllHeadcountsByBySchoolIdAndSdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query(value = """  
    SELECT stud
    FROM SdcSchoolCollectionStudentEntity stud
    WHERE stud.assignedStudentId IN (:matchedAssignedIDs)
    and stud.sdcSchoolCollection.collectionEntity.collectionID = :collectionID
    and stud.sdcSchoolCollectionStudentStatusCode != 'DELETED'
    """)
  List<SdcSchoolCollectionStudentEntity> findAllDuplicateStudentsByCollectionID(UUID collectionID, List<UUID> matchedAssignedIDs);

  @Query("SELECT " +
          "COUNT(CASE WHEN s.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = s AND (si.validationIssueCode = 'REFUGEEINPREVCOL' OR si.validationIssueCode = 'REFUGEEISADULT')) THEN s.sdcSchoolCollectionStudentID ELSE NULL END) AS eligibleStudents, " +
          "COUNT(CASE WHEN s.schoolFundingCode = '16' THEN s.sdcSchoolCollectionStudentID ELSE NULL END) AS reportedStudents, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'")
  RefugeeHeadcountHeaderResult getRefugeeHeadersBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "COUNT(CASE WHEN s.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = s AND (si.validationIssueCode = 'REFUGEEINPREVCOL' OR si.validationIssueCode = 'REFUGEEISADULT')) THEN s.sdcSchoolCollectionStudentID ELSE NULL END) AS eligibleStudents, " +
          "COUNT(CASE WHEN s.schoolFundingCode = '16' THEN s.sdcSchoolCollectionStudentID ELSE NULL END) AS reportedStudents, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionId " +
          "AND s.sdcSchoolCollectionStudentStatusCode <> 'DELETED'")
  RefugeeHeadcountHeaderResult getRefugeeHeadersBySchoolId(@Param("sdcSchoolCollectionId") UUID sdcSchoolCollectionId);

  @Query("SELECT s.sdcSchoolCollection.schoolID AS schoolID, " +
          "SUM(CASE WHEN (s.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = s AND (si.validationIssueCode = 'REFUGEEINPREVCOL' OR si.validationIssueCode = 'REFUGEEISADULT'))) THEN s.fte ELSE 0 END) AS fteTotal, " +
          "COUNT(CASE WHEN (s.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = s AND (si.validationIssueCode = 'REFUGEEINPREVCOL' OR si.validationIssueCode = 'REFUGEEISADULT'))) THEN s.sdcSchoolCollectionStudentID ELSE NULL END) AS headcount, " +
          "COUNT(DISTINCT CASE WHEN (s.schoolFundingCode = '16' AND s.ellNonEligReasonCode IS NULL AND EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentEnrolledProgramEntity ep WHERE ep.sdcSchoolCollectionStudentEntity = s AND ep.enrolledProgramCode = '17') AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = s AND (si.validationIssueCode = 'REFUGEEINPREVCOL' OR si.validationIssueCode = 'REFUGEEISADULT'))) THEN s.sdcSchoolCollectionStudentID END) AS ell " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.sdcSchoolCollection.schoolID " +
          "ORDER BY s.sdcSchoolCollection.schoolID")
  List<RefugeeHeadcountResult> getRefugeeHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Modifying
  @Query(value = """
           UPDATE SDC_SCHOOL_COLLECTION_STUDENT
           SET sdc_school_collection_student_status_code = 'DEMOG_UPD', update_user = 'STUDENT_DATA_COLLECTION_API', update_date = CURRENT_TIMESTAMP
           WHERE sdc_school_collection_id IN 
           (SELECT sdc_school_collection_id FROM SDC_SCHOOL_COLLECTION WHERE collection_id = :collectionID)
           """, nativeQuery = true)
  void updateAllSdcSchoolCollectionStudentStatus(UUID collectionID);

  @Query(value="""
    SELECT stud FROM SdcSchoolCollectionStudentEntity stud WHERE stud.sdcSchoolCollectionStudentID
    NOT IN (SELECT saga.sdcSchoolCollectionStudentID FROM SdcSagaEntity saga WHERE saga.status != 'COMPLETED'
    AND saga.sdcSchoolCollectionStudentID IS NOT NULL)
    AND stud.sdcSchoolCollectionStudentStatusCode = 'DEMOG_UPD'
    order by stud.createDate
    LIMIT :numberOfStudentsToProcess""")
  List<SdcSchoolCollectionStudentEntity> findStudentForDownstreamUpdate(String numberOfStudentsToProcess);

  @Query(value="""
         SELECT SSCS FROM SdcSchoolCollectionEntity SSC, CollectionEntity C, SdcSchoolCollectionStudentEntity SSCS, SdcDistrictCollectionEntity SDC
          WHERE SDC.districtID = :districtID
          AND C.collectionID = SDC.collectionEntity.collectionID
          AND C.collectionID = SSC.collectionEntity.collectionID
          AND SDC.sdcDistrictCollectionID = SSC.sdcDistrictCollectionID
          AND SSC.sdcSchoolCollectionID = SSCS.sdcSchoolCollection.sdcSchoolCollectionID
          AND SSCS.assignedStudentId = :assignedStudentID
          AND SSCS.fte > 0
          AND C.collectionID IN
                  (SELECT CE.collectionID FROM CollectionEntity CE WHERE CE.collectionStatusCode = 'COMPLETED' ORDER BY CE.snapshotDate DESC LIMIT :noOfCollections)
          """)
  List<SdcSchoolCollectionStudentEntity> findStudentInCurrentFiscalWithInSameDistrict(UUID districtID, UUID assignedStudentID, String noOfCollections);

  @Query(value="""
         SELECT SSCS FROM SdcSchoolCollectionEntity SSC, CollectionEntity C, SdcSchoolCollectionStudentEntity SSCS, SdcDistrictCollectionEntity SDC
          WHERE C.collectionID = SDC.collectionEntity.collectionID
          AND C.collectionID = SSC.collectionEntity.collectionID
          AND SDC.sdcDistrictCollectionID = SSC.sdcDistrictCollectionID
          AND SSC.sdcSchoolCollectionID = SSCS.sdcSchoolCollection.sdcSchoolCollectionID
          AND SSCS.assignedStudentId = :assignedStudentID
          AND C.collectionID IN
                  (SELECT CE.collectionID FROM CollectionEntity CE WHERE CE.collectionStatusCode = 'COMPLETED' ORDER BY CE.snapshotDate DESC LIMIT :noOfCollections)
          """)
  List<SdcSchoolCollectionStudentEntity> findStudentInCurrentFiscalInAllDistrict(UUID assignedStudentID, String noOfCollections);

  @Query(value="""
           SELECT SSCS FROM SdcSchoolCollectionEntity SSC, CollectionEntity C, SdcSchoolCollectionStudentEntity SSCS, SdcDistrictCollectionEntity SDC
            WHERE SDC.districtID != :districtID
            AND C.collectionID = SDC.collectionEntity.collectionID
            AND C.collectionID = SSC.collectionEntity.collectionID
            AND SDC.sdcDistrictCollectionID = SSC.sdcDistrictCollectionID
            AND SSC.sdcSchoolCollectionID = SSCS.sdcSchoolCollection.sdcSchoolCollectionID
            AND SSCS.assignedStudentId = :assignedStudentID
            AND SSCS.enrolledGradeCode NOT IN ('08', '09')
            AND SSCS.fte > 0
            AND C.collectionID IN
                  (SELECT CE.collectionID FROM CollectionEntity CE WHERE CE.collectionStatusCode = 'COMPLETED' ORDER BY CE.snapshotDate DESC LIMIT :noOfCollections)
            """)
  List<SdcSchoolCollectionStudentEntity> findStudentInCurrentFiscalInOtherDistricts(UUID districtID, UUID assignedStudentID, String noOfCollections);

  List<SdcSchoolCollectionStudentEntity> findAllBySdcSchoolCollection_CollectionEntity_CollectionIDAndEnrolledGradeCodeIn(UUID collectionID, List<String> enrolledGradeCode);

  @Query("SELECT " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B') THEN 1 END) AS levelOnes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('C', 'D', 'E', 'F', 'G') THEN 1 END) AS levelTwos, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('H') THEN 1 END) AS levelThrees, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('K', 'P', 'Q', 'R') THEN 1 END) AS otherLevels, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' THEN 1 END) AS specialEdACodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdA, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' THEN 1 END) AS specialEdBCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdB, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' THEN 1 END) AS specialEdCCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdC, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' THEN 1 END) AS specialEdDCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdD, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' THEN 1 END) AS specialEdECodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdE, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' THEN 1 END) AS specialEdFCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdF, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' THEN 1 END) AS specialEdGCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdG, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' THEN 1 END) AS specialEdHCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdH, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' THEN 1 END) AS specialEdKCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdK, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' THEN 1 END) AS specialEdPCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdP, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' THEN 1 END) AS specialEdQCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdQ, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' THEN 1 END) AS specialEdRCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' AND s.isAdult = true THEN 1 END) > 0 AS adultsInSpecialEdR, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'P', 'Q', 'R') THEN 1 END) AS allLevels " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.collectionEntity.collectionID = :collectionID " +
          "AND s.sdcSchoolCollection.sdcDistrictCollectionID is null " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "AND s.specialEducationNonEligReasonCode IS NULL " +
          "GROUP BY s.sdcSchoolCollection.schoolID " +
          "ORDER BY s.sdcSchoolCollection.schoolID")
  List<IndySpecialEdAdultHeadcountResult> getSpecialEdCategoryForIndiesAndOffshoreByCollectionId(UUID collectionID);

  @Query("SELECT " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B') THEN 1 END) AS levelOnes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'A' THEN 1 END) AS specialEdACodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'B' THEN 1 END) AS specialEdBCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('C', 'D', 'E', 'F', 'G') THEN 1 END) AS levelTwos, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'C' THEN 1 END) AS specialEdCCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'D' THEN 1 END) AS specialEdDCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'E' THEN 1 END) AS specialEdECodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'F' THEN 1 END) AS specialEdFCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'G' THEN 1 END) AS specialEdGCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('H') THEN 1 END) AS levelThrees, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'H' THEN 1 END) AS specialEdHCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('K', 'P', 'Q', 'R') THEN 1 END) AS otherLevels, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'K' THEN 1 END) AS specialEdKCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'P' THEN 1 END) AS specialEdPCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'Q' THEN 1 END) AS specialEdQCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode = 'R' THEN 1 END) AS specialEdRCodes, " +
          "COUNT(CASE WHEN s.specialEducationCategoryCode IN ('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'P', 'Q', 'R') THEN 1 END) AS allLevels " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.collectionEntity.collectionID = :collectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "AND s.sdcSchoolCollection.sdcDistrictCollectionID is null " +
          "AND s.specialEducationNonEligReasonCode IS NULL " +
          "GROUP BY s.sdcSchoolCollection.schoolID " +
          "ORDER BY s.sdcSchoolCollection.schoolID")
  List<SpecialEdHeadcountResult> getSpecialEdHeadcountsByCollectionId(@Param("collectionID") UUID collectionID);

  @Query(value = """
        SELECT sscs.sdcSchoolCollection.schoolID as schoolID,
        sscs.homeLanguageSpokenCode as spokenLanguageCode,
        COUNT(sscs.homeLanguageSpokenCode) as headcount
        FROM SdcSchoolCollectionStudentEntity sscs
        WHERE sscs.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED')
        AND sscs.sdcSchoolCollection.collectionEntity.collectionID = :collectionID
        AND sscs.sdcSchoolCollection.sdcDistrictCollectionID is NULL
        AND sscs.homeLanguageSpokenCode is NOT NULL
        GROUP BY sscs.sdcSchoolCollection.schoolID, sscs.homeLanguageSpokenCode
  """)
  List<SpokenLanguageHeadcountResult> getAllHomeLanguageSpokenCodesForIndiesAndOffshoreInCollection(@Param("collectionID") UUID collectionID);


  @Query("SELECT " +
          "CASE WHEN s.fteZeroReasonCode is not null then s.fteZeroReasonCode ELSE 'Other' END AS fteZeroReasonCode, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = 'KF' THEN 1 END) AS gradeKF, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '01' THEN 1 END) AS grade01, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '02' THEN 1 END) AS grade02, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '03' THEN 1 END) AS grade03, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '04' THEN 1 END) AS grade04, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '05' THEN 1 END) AS grade05, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '06' THEN 1 END) AS grade06, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '07' THEN 1 END) AS grade07, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = 'EU' THEN 1 END) AS gradeEU, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '08' THEN 1 END) AS grade08, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '09' THEN 1 END) AS grade09, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '10' THEN 1 END) AS grade10, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '11' THEN 1 END) AS grade11, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = '12' THEN 1 END) AS grade12, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = 'SU' THEN 1 END) AS gradeSU, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = 'GA' THEN 1 END) AS gradeGA, " +
          "COUNT(CASE WHEN s.enrolledGradeCode = 'HS' THEN 1 END) AS gradeHS, " +
          "COUNT(CASE WHEN s.enrolledGradeCode is not null THEN 1 END) AS allLevels "+
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "and s.fte = 0 " +
          "GROUP BY s.fteZeroReasonCode " +
          "ORDER BY s.fteZeroReasonCode")
  List<ZeroFTEHeadcountResult> getZeroFTEHeadcountsBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query(value = """
          SELECT
          sscs.sdcSchoolCollection.schoolID as schoolID,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' THEN 1 END) as khTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as khLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as khLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as khLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'KH' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS khEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'KH' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS khIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'KH' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS khCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'KH' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS khEarlyFrenchCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '01' THEN 1 END) as gradeOneTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeOneLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeOneLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeOneLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '01' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeOneEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '01' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeOneIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '01' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeOneCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '01' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeOneEarlyFrenchCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '02' THEN 1 END) as gradeTwoTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeTwoLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeTwoLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeTwoLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '02' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwoEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '02' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwoIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '02' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwoCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '02' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwoEarlyFrenchCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '03' THEN 1 END) as gradeThreeTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeThreeLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeThreeLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeThreeLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '03' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeThreeEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '03' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeThreeIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '03' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeThreeCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '03' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeThreeEarlyFrenchCount,    
          COUNT(CASE WHEN sscs.enrolledGradeCode = '04' THEN 1 END) as gradeFourTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeFourLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeFourLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeFourLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '04' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFourEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '04' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFourIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '04' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFourCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '04' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFourEarlyFrenchCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '05' THEN 1 END) as gradeFiveTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeFiveLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeFiveLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeFiveLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '05' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFiveEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '05' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFiveIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '05' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFiveCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '05' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFiveEarlyFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '05' AND ep.enrolledProgramCode = '14' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFiveLateFrenchCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '06' THEN 1 END) as gradeSixTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeSixLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeSixLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeSixLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '06' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSixEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '06' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSixIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '06' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSixCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '06' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSixEarlyFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '06' AND ep.enrolledProgramCode = '14' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSixLateFrenchCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '07' THEN 1 END) as gradeSevenTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeSevenLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeSevenLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeSevenLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '07' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSevenEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '07' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSevenIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '07' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSevenCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '07' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSevenEarlyFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '07' AND ep.enrolledProgramCode = '14' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSevenLateFrenchCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '08' THEN 1 END) as gradeEightTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeEightLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeEightLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeEightLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '08' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEightEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '08' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEightIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '08' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEightCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '08' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEightEarlyFrenchCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '09' THEN 1 END) as gradeNineTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeNineLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeNineLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeNineLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '09' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeNineEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '09' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeNineIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '09' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeNineCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '09' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeNineEarlyFrenchCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '10' THEN 1 END) as gradeTenTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeTenLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeTenLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeTenLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '10' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTenEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '10' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTenIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '10' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTenCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '10' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTenEarlyFrenchCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '11' THEN 1 END) as gradeElevenTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeElevenLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeElevenLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeElevenLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '11' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeElevenEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '11' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeElevenIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '11' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeElevenCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '11' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeElevenEarlyFrenchCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '12' THEN 1 END) as gradeTwelveTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeTwelveLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeTwelveLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeTwelveLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '12' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwelveEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '12' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwelveIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '12' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwelveCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '12' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwelveEarlyFrenchCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'EU' THEN 1 END) as gradeEuTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeEuLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeEuLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeEuLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'EU' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEuEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'EU' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEuIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'EU' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEueCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'EU' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEuEarlyFrenchCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'SU' THEN 1 END) as gradeSuTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeSuLevelOneCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeSuLevelTwoCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as gradeSuLevelThreeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'SU' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSuEllCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'SU' AND ep.enrolledProgramCode IN ('29', '33', '36') AND sscs.indigenousSupportProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSuIndigenousCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'SU' AND ep.enrolledProgramCode = '08' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSuCoreFrenchCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'SU' AND ep.enrolledProgramCode = '11' AND sscs.frenchProgramNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSuEarlyFrenchCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'HS' THEN 1 END) as gradeHSCount,
          COUNT(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = true THEN 1 END) as gradAdultCount,
          COUNT(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = false THEN 1 END) as nonGradAdultCount,
          COUNT(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = false AND sscs.specialEducationCategoryCode in ('A', 'B') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as nonGradAdultLevelOneCount,
          COUNT(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = false AND sscs.specialEducationCategoryCode in ('C', 'D', 'E', 'F', 'G') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as nonGradAdultLevelTwoCount,
          COUNT(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = false AND sscs.specialEducationCategoryCode in ('H') AND sscs.specialEducationNonEligReasonCode IS NULL THEN 1 END) as nonGradAdultLevelThreeCount,
          
          SUM(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as khTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeOneTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeTwoTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeThreeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeFourTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeFiveTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeSixTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeSevenTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeEightTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeNineTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeTenTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeElevenTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeTwelveTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeEuTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeSuTotalFte,
          SUM(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = true AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradAdultTotalFte,
          SUM(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = false AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as nonGradAdultTotalFte
          FROM SdcSchoolCollectionStudentEntity sscs
          LEFT JOIN sscs.sdcStudentEnrolledProgramEntities ep
          WHERE sscs.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED')
          AND sscs.sdcSchoolCollection.collectionEntity.collectionID = :collectionID
          AND sscs.sdcSchoolCollection.sdcDistrictCollectionID is null
          GROUP BY sscs.sdcSchoolCollection.schoolID """)
  List<EnrolmentHeadcountFteResult> getEnrolmentHeadcountsAndFteByCollectionId(@Param("collectionID") UUID collectionID);


  @Query(value = """
          SELECT
          sscs.sdcSchoolCollection.schoolID as schoolID,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' THEN 1 END) as khTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS khRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'KH' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS khEllCount,
         
          COUNT(CASE WHEN sscs.enrolledGradeCode = '01' THEN 1 END) as gradeOneTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeOneRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '01' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeOneEllCount,
         
          COUNT(CASE WHEN sscs.enrolledGradeCode = '02' THEN 1 END) as gradeTwoTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeTwoRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '02' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwoEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '03' THEN 1 END) as gradeThreeTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeThreeRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '03' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeThreeEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '04' THEN 1 END) as gradeFourTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeFourRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '04' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFourEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '05' THEN 1 END) as gradeFiveTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeFiveRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '05' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFiveEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '06' THEN 1 END) as gradeSixTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeSixRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '06' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSixEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '07' THEN 1 END) as gradeSevenTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeSevenRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '07' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSevenEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '08' THEN 1 END) as gradeEightTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeEightRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '08' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEightEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '09' THEN 1 END) as gradeNineTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeNineRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '09' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeNineEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '10' THEN 1 END) as gradeTenTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeTenRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '10' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTenEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '11' THEN 1 END) as gradeElevenTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeElevenRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '11' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeElevenEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '12' THEN 1 END) as gradeTwelveTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeTwelveRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '12' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwelveEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'EU' THEN 1 END) as gradeEuTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeEuRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'EU' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEuEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'SU' THEN 1 END) as gradeSuTotalCount,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.schoolFundingCode = '16' THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeSuRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'SU' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSuEllCount,
          

          COUNT(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = true THEN 1 END) as gradAdultCount,
          COUNT(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = false THEN 1 END) as nonGradAdultCount,
          
          SUM(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as khTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as khRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeOneTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeOneRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeTwoTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeTwoRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeThreeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeThreeRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeFourTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeFourRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeFiveTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeFiveRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeSixTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeSixRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeSevenTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeSevenRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeEightTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeEightRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeNineTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeNineRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeTenTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeTenRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeElevenTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeElevenRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeTwelveTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeTwelveRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeEuTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeEuRefugeeTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeSuTotalFte,
          SUM(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradeSuRefugeeTotalFte,
          SUM(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = true AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as gradAdultTotalFte,
          SUM(CASE WHEN sscs.isAdult = true AND sscs.isGraduated = false AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as nonGradAdultTotalFte
          FROM SdcSchoolCollectionStudentEntity sscs
          LEFT JOIN sscs.sdcStudentEnrolledProgramEntities ep
          WHERE sscs.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED')
          AND sscs.sdcSchoolCollection.collectionEntity.collectionID = :collectionID
          AND sscs.sdcSchoolCollection.sdcDistrictCollectionID is null
          GROUP BY sscs.sdcSchoolCollection.schoolID""")
  List<EnrolmentHeadcountFteResult> getEnrolmentHeadcountsAndFteWithRefugeeByCollectionId(@Param("collectionID") UUID collectionID);

  @Query("SELECT " +
          " sscs.sdcSchoolCollection.schoolID as schoolID, " +

          // Headcount for each grade
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' THEN 1 END) as kindHCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'KF' THEN 1 END) as kindFCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '01' THEN 1 END) as grade1Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '02' THEN 1 END) as grade2Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '03' THEN 1 END) as grade3Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '04' THEN 1 END) as grade4Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '05' THEN 1 END) as grade5Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '06' THEN 1 END) as grade6Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '07' THEN 1 END) as grade7Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'EU' THEN 1 END) as gradeEUCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '08' THEN 1 END) as grade8Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '09' THEN 1 END) as grade9Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '10' THEN 1 END) as grade10Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '11' THEN 1 END) as grade11Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '12' THEN 1 END) as grade12Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'SU' THEN 1 END) as gradeSUCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'GA' THEN 1 END) as gradeGACount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'HS' THEN 1 END) as gradeHSCount, " +

          // Total headcount
          " SUM(CASE WHEN sscs.enrolledGradeCode IN ('KH', 'KF', '01', '02', '03', '04', '05', '06', '07', 'EU', '08', '09', '10', '11', '12', 'SU', 'GA', 'HS') THEN 1 ELSE 0 END) as totalCount, " +

          // FTE counts for each grade
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END ) as kindHFTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'KF' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as kindFFTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade1FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade2FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade3FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade4FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade5FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade6FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade7FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as gradeEUFTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade8FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade9FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade10FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade11FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as grade12FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as gradeSUFTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'GA' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as gradeGAFTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'HS' AND sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as gradeHSFTE, " +

          // Total FTE
          " SUM(CASE WHEN sscs.fte IS NOT NULL THEN sscs.fte ELSE 0 END) as totalFTE " +

          " FROM SdcSchoolCollectionStudentEntity sscs " +
          " WHERE sscs.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          " AND sscs.sdcSchoolCollection.collectionEntity.collectionID = :collectionID " +
          " AND sscs.sdcSchoolCollection.sdcDistrictCollectionID is null " +
          " GROUP BY sscs.sdcSchoolCollection.schoolID ")
  List<IndyFundingResult> getIndyFundingHeadcountsByCollectionId(@Param("collectionID") UUID collectionID);

  @Query("SELECT " +
          " sscs.sdcSchoolCollection.schoolID as schoolID, " +

          // Headcount for each grade
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as kindHCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'KF' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as kindFCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade1Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade2Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade3Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade4Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade5Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade6Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade7Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as gradeEUCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade8Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade9Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade10Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade11Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as grade12Count, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as gradeSUCount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'GA' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as gradeGACount, " +
          " COUNT(CASE WHEN sscs.enrolledGradeCode = 'HS' AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 END) as gradeHSCount, " +

          // Total headcount
          " SUM(CASE WHEN sscs.enrolledGradeCode IN ('KH', 'KF', '01', '02', '03', '04', '05', '06', '07', 'EU', '08', '09', '10', '11', '12', 'SU', 'GA', 'HS') AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN 1 ELSE 0 END) as totalCount, " +

          // FTE counts for each grade
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END ) as kindHFTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'KF' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as kindFFTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade1FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade2FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade3FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade4FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade5FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade6FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade7FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as gradeEUFTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade8FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade9FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade10FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade11FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as grade12FTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as gradeSUFTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'GA' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as gradeGAFTE, " +
          " SUM(CASE WHEN sscs.enrolledGradeCode = 'HS' AND sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as gradeHSFTE, " +

          // Total FTE
          " SUM(CASE WHEN sscs.fte IS NOT NULL AND sscs.isAdult = TRUE AND sscs.isGraduated = FALSE THEN sscs.fte ELSE 0 END) as totalFTE " +

          " FROM SdcSchoolCollectionStudentEntity sscs " +
          " WHERE sscs.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          " AND sscs.sdcSchoolCollection.collectionEntity.collectionID = :collectionID " +
          " AND sscs.sdcSchoolCollection.sdcDistrictCollectionID is null " +
          " GROUP BY sscs.sdcSchoolCollection.schoolID ")
  List<IndyFundingResult> getIndyFundingHeadcountsNonGraduatedAdultByCollectionId(@Param("collectionID") UUID collectionID);

  @Query(value = """
          SELECT
          sscs.sdcSchoolCollection.schoolID as schoolID,
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS khRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS khEllCount,
         
          COUNT(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeOneRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeOneEllCount,
         
          COUNT(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeTwoRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwoEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeThreeRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeThreeEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeFourRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFourEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeFiveRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeFiveEllCount,
         
          COUNT(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeSixRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSixEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeSevenRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSevenEllCount,

          COUNT(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeEightRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEightEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeNineRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeNineEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeTenRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTenEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeElevenRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeElevenEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeTwelveRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeTwelveEllCount,
          
          COUNT(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeEuRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL  AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeEuEllCount,

          COUNT(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID ELSE NULL END) AS gradeSuRefugeeCount,
          COUNT(DISTINCT CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.schoolFundingCode = '16' AND ep.enrolledProgramCode = '17' AND sscs.ellNonEligReasonCode IS NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode
          IN ('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.sdcSchoolCollectionStudentID END) AS gradeSuEllCount,
          
          SUM(CASE WHEN sscs.enrolledGradeCode = 'KH' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as khRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '01' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeOneRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '02' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeTwoRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '03' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeThreeRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '04' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeFourRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '05' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeFiveRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '06' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeSixRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '07' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeSevenRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '08' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeEightRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '09' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeNineRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '10' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeTenRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '11' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeElevenRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = '12' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeTwelveRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = 'EU' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeEuRefugeeTotalFte,

          SUM(CASE WHEN sscs.enrolledGradeCode = 'SU' AND sscs.schoolFundingCode = '16' AND sscs.fte IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE 
          si.sdcSchoolCollectionStudentEntity = sscs AND si.validationIssueCode IN('REFUGEEINPREVCOL', 'REFUGEEISADULT')) THEN sscs.fte ELSE 0 END ) as gradeSuRefugeeTotalFte
          
          FROM SdcSchoolCollectionStudentEntity sscs
          LEFT JOIN sscs.sdcStudentEnrolledProgramEntities ep
          WHERE sscs.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED')
          AND sscs.sdcSchoolCollection.collectionEntity.collectionID = :collectionID
          AND sscs.sdcSchoolCollection.sdcDistrictCollectionID is null
          GROUP BY sscs.sdcSchoolCollection.schoolID""")
  List<EnrolmentHeadcountFteResult> getNewRefugeeEnrolmentHeadcountsAndFteWithByCollectionId(@Param("collectionID") UUID collectionID);
}
