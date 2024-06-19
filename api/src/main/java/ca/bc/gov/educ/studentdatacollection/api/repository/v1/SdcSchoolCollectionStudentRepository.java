package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ICountValidationIssuesBySeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
    SELECT stud
    FROM SdcSchoolCollectionStudentEntity stud, SdcSchoolCollectionEntity school, SdcDistrictCollectionEntity dist
    WHERE stud.assignedStudentId IN (SELECT innerStud.assignedStudentId
                FROM SdcSchoolCollectionStudentEntity innerStud, SdcSchoolCollectionEntity sdcSchool, SdcDistrictCollectionEntity sdcDist
                where sdcDist.sdcDistrictCollectionID = :sdcDistrictCollectionID
                and sdcDist.sdcDistrictCollectionID = sdcSchool.sdcDistrictCollectionID
                and sdcSchool.sdcSchoolCollectionID = innerStud.sdcSchoolCollection.sdcSchoolCollectionID
                and innerStud.sdcSchoolCollectionStudentStatusCode != 'DELETED'
                and innerStud.assignedStudentId is not null
                GROUP BY innerStud.assignedStudentId
                HAVING COUNT(innerStud.assignedStudentId) > 1)
    and dist.sdcDistrictCollectionID = :sdcDistrictCollectionID
    and dist.sdcDistrictCollectionID = school.sdcDistrictCollectionID
    and school.sdcSchoolCollectionID = stud.sdcSchoolCollection.sdcSchoolCollectionID
    and stud.sdcSchoolCollectionStudentStatusCode != 'DELETED'
    and stud.assignedStudentId is not null
    """)
  List<SdcSchoolCollectionStudentEntity> findAllInDistrictDuplicateStudentsInSdcDistrictCollection(UUID sdcDistrictCollectionID);

  @Query(value = """  
    SELECT stud
    FROM SdcSchoolCollectionStudentEntity stud, SdcSchoolCollectionEntity school
    WHERE stud.assignedStudentId IN (SELECT innerStud.assignedStudentId
                FROM SdcSchoolCollectionStudentEntity innerStud, SdcSchoolCollectionEntity sdcSchool
                where sdcSchool.collectionEntity.collectionID = :collectionID
                and sdcSchool.sdcSchoolCollectionID = innerStud.sdcSchoolCollection.sdcSchoolCollectionID
                and innerStud.sdcSchoolCollectionStudentStatusCode != 'DELETED'
                and innerStud.assignedStudentId is not null
                GROUP BY innerStud.assignedStudentId
                HAVING COUNT(innerStud.assignedStudentId) > 1)
    and school.sdcSchoolCollectionID = stud.sdcSchoolCollection.sdcSchoolCollectionID
    and school.collectionEntity.collectionID = :collectionID
    and stud.sdcSchoolCollectionStudentStatusCode != 'DELETED'
    and stud.assignedStudentId is not null
    """)
  List<SdcSchoolCollectionStudentEntity> findAllInProvinceDuplicateStudentsInCollection(UUID collectionID);

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
    SELECT COUNT(*) FROM SDC_SCHOOL_COLLECTION_STUDENT SSCS 
    WHERE SSCS.sdc_school_collection_id = :sdcSchoolID 
    AND SSCS.SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE != 'DELETED'
    AND SSCS.STUDENT_PEN=:studentPen""", nativeQuery = true)
  Long countForDuplicateStudentPENs(UUID sdcSchoolID, String studentPen);

  @Query(value="""
    SELECT stud FROM SdcSchoolCollectionStudentEntity stud WHERE stud.sdcSchoolCollectionStudentID
    NOT IN (SELECT saga.sdcSchoolCollectionStudentID FROM SdcSagaEntity saga WHERE saga.status != 'COMPLETED')
    AND stud.sdcSchoolCollectionStudentStatusCode = 'LOADED'
    order by stud.createDate
    LIMIT :numberOfStudentsToProcess""")
  List<SdcSchoolCollectionStudentEntity> findTopLoadedStudentForProcessing(String numberOfStudentsToProcess);

  long countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(UUID assignedStudentId, List<UUID> sdcSchoolCollectionID, String numberOfCourses);

  @Query("""
       SELECT COUNT(s) FROM SdcSchoolCollectionStudentEntity s
       WHERE s.assignedStudentId = :assignedStudentId
       AND s.sdcSchoolCollection.sdcSchoolCollectionID IN :sdcSchoolCollectionIDs
       """)
  long countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(
          @Param("assignedStudentId") UUID assignedStudentId,
          @Param("sdcSchoolCollectionIDs") List<UUID> sdcSchoolCollectionIDs);

  @Query("""
       SELECT COUNT(s) FROM SdcSchoolCollectionStudentEntity s
       WHERE s.assignedStudentId = :assignedStudentId
       AND s.sdcSchoolCollection.sdcSchoolCollectionID IN :sdcSchoolCollectionIDs
       AND s.enrolledGradeCode <> 'HS'
       """)
  long countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschool(
          @Param("assignedStudentId") UUID assignedStudentId,
          @Param("sdcSchoolCollectionIDs") List<UUID> sdcSchoolCollectionIDs);

  @Query("""
       SELECT COUNT(s) FROM SdcSchoolCollectionStudentEntity s
       WHERE s.assignedStudentId = :assignedStudentId
       AND s.sdcSchoolCollection.sdcSchoolCollectionID IN :sdcSchoolCollectionIDs
       AND s.enrolledGradeCode <> 'HS'
       AND s.fte > 0
       """)
  long countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInExcludingHomeschoolWithNonZeroFTE(
          @Param("assignedStudentId") UUID assignedStudentId,
          @Param("sdcSchoolCollectionIDs") List<UUID> sdcSchoolCollectionIDs);

  long countAllByAssignedStudentIdAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(UUID assignedStudentId, String enrolledGradeCode, List<UUID> sdcSchoolCollectionID);

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
          "COUNT(DISTINCT CASE WHEN s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalEllStudents " +
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
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS reportedStudents, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' AND ell.yearsInEll < 6 THEN s.sdcSchoolCollectionStudentID END) AS oneToFiveYears, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' AND ell.yearsInEll > 5 THEN s.sdcSchoolCollectionStudentID END) AS sixPlusYears, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN SdcStudentEllEntity ell " +
          "ON s.assignedStudentId = ell.studentID " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionId")
  EllHeadcountHeaderResult getEllHeadersBySchoolId(@Param("sdcSchoolCollectionId") UUID sdcSchoolCollectionId);

  @Query(value="""
           SELECT SSCS.numberOfCourses FROM SdcSchoolCollectionEntity SSC, CollectionEntity C, SdcSchoolCollectionStudentEntity SSCS WHERE SSC.schoolID = :schoolID
            AND C.collectionID = SSC.collectionEntity.collectionID
            AND SSC.sdcSchoolCollectionID = SSCS.sdcSchoolCollection.sdcSchoolCollectionID
            AND SSCS.studentPen = :pen
            AND C.openDate < :currentOpenDate
            AND C.openDate >= (SELECT C.openDate FROM CollectionEntity C WHERE C.collectionTypeCode = :collectionTypeCode AND EXTRACT(YEAR FROM C.openDate) = :targetYear)
            """)
  List<String> getCollectionHistory(UUID schoolID, String pen, LocalDateTime currentOpenDate, String collectionTypeCode, Integer targetYear);

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
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID")
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
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID")
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
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID")
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
    "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID")
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
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID")
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
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID")
  CareerHeadcountHeaderResult getCareerHeadersBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN s.sdcSchoolCollectionStudentID END) AS preparationTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN s.sdcSchoolCollectionStudentID END) AS coopTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN s.sdcSchoolCollectionStudentID END) AS apprenticeTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN s.sdcSchoolCollectionStudentID END) AS techYouthTotal, " +
          "COUNT(DISTINCT CASE WHEN s.careerProgramCode in ('XA', 'XB', 'XC', 'XD', 'XE', 'XF', 'XG', 'XH') AND s.careerProgramNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS allTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode " +
          "ORDER BY s.sdcSchoolCollection.schoolID, s.enrolledGradeCode")
  List<CareerHeadcountResult> getCareerHeadcountsBySchoolIdAndBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(DISTINCT CASE WHEN s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalEllStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID " +
          "AND s.sdcSchoolCollectionStudentStatusCode NOT IN ('ERROR', 'DELETED') " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<EllHeadcountResult> getEllHeadcountsBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' AND s.ellNonEligReasonCode IS NULL THEN s.sdcSchoolCollectionStudentID END) AS eligibleStudents, " +
          "COUNT(DISTINCT CASE WHEN ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS reportedStudents, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID")
  EllHeadcountHeaderResult getEllHeadersBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "s.sdcSchoolCollection.schoolID AS schoolID, " +
          "COUNT(DISTINCT CASE WHEN s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' THEN s.sdcSchoolCollectionStudentID END) AS totalEllStudents " +
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
          "COUNT(CASE WHEN s.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID = s.sdcSchoolCollectionStudentID AND (si.validationIssueCode = 'REFUGEEINPREVCOL' OR si.validationIssueCode = 'REFUGEEISADULT')) THEN s.sdcSchoolCollectionStudentID ELSE NULL END) AS eligibleStudents, " +
          "COUNT(CASE WHEN s.schoolFundingCode = '16' THEN s.sdcSchoolCollectionStudentID ELSE NULL END) AS reportedStudents, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcDistrictCollectionID = :sdcDistrictCollectionID")
  RefugeeHeadcountHeaderResult getRefugeeHeadersBySdcDistrictCollectionId(@Param("sdcDistrictCollectionID") UUID sdcDistrictCollectionID);

  @Query("SELECT " +
          "COUNT(CASE WHEN s.schoolFundingCode = '16' AND NOT EXISTS (SELECT 1 FROM SdcSchoolCollectionStudentValidationIssueEntity si WHERE si.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID = s.sdcSchoolCollectionStudentID AND (si.validationIssueCode = 'REFUGEEINPREVCOL' OR si.validationIssueCode = 'REFUGEEISADULT')) THEN s.sdcSchoolCollectionStudentID ELSE NULL END) AS eligibleStudents, " +
          "COUNT(CASE WHEN s.schoolFundingCode = '16' THEN s.sdcSchoolCollectionStudentID ELSE NULL END) AS reportedStudents, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionId")
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

}
