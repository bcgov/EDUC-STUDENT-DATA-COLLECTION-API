package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EllHeadcountHeaderResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EllHeadcountResult;
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

  long countBySdcSchoolCollectionStudentStatusCodeAndSdcSchoolCollection_SdcSchoolCollectionID(String sdcSchoolCollectionStudentStatusCode, UUID sdcSchoolCollectionID);

  @Query(value = """
    SELECT COUNT(*)
    FROM SDC_SCHOOL_COLLECTION_STUDENT S, SDC_SCHOOL_COLLECTION_STUDENT_VALIDATION_ISSUE I
    WHERE S.SDC_SCHOOL_COLLECTION_STUDENT_ID = I.SDC_SCHOOL_COLLECTION_STUDENT_ID
    AND S.SDC_SCHOOL_COLLECTION_ID = :sdcSchoolCollectionID
    AND I.VALIDATION_ISSUE_SEVERITY_CODE = :validationIssueSeverityCode
    AND S.SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE != 'DELETED'
    """, nativeQuery = true)
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

  long countByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDInAndNumberOfCoursesGreaterThan(UUID assignedStudentId, List<UUID> sdcSchoolCollectionID, String numberOfCourses);

  long countAllByAssignedStudentIdAndSdcSchoolCollection_SdcSchoolCollectionIDIn(UUID assignedStudentId, List<UUID> sdcSchoolCollectionID);

  long countAllByAssignedStudentIdAndEnrolledGradeCodeAndSdcSchoolCollection_SdcSchoolCollectionIDIn(UUID assignedStudentId, String enrolledGradeCode, List<UUID> sdcSchoolCollectionID);

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
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID AND s.sdcSchoolCollectionStudentStatusCode != 'ERROR' " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<EnrollmentHeadcountResult> getEnrollmentHeadcountsBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '08' THEN 1 END) AS schoolAgedCoreFrench, " +
          "COUNT(CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '08' THEN 1 END) AS adultCoreFrench, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '08' AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalCoreFrench, " +
          "COUNT(CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '11' THEN 1 END) AS schoolAgedEarlyFrench, " +
          "COUNT(CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '11' THEN 1 END) AS adultEarlyFrench, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '11' AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalEarlyFrench, " +
          "COUNT(CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '14' THEN 1 END) AS schoolAgedLateFrench, " +
          "COUNT(CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '14' THEN 1 END) AS adultLateFrench, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '14' AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalLateFrench, " +
          "COUNT(CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode IN ('05', '08', '11', '14') THEN 1 END) AS schoolAgedTotals, " +
          "COUNT(CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode IN ('05', '08', '11', '14') THEN 1 END) AS adultTotals, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode IN ('05', '08', '11', '14') AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalTotals " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID AND s.sdcSchoolCollectionStudentStatusCode != 'ERROR' " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<FrenchHeadcountResult> getFrenchHeadcountsBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);
  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(CASE WHEN s.isSchoolAged = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '05' THEN 1 END) AS schoolAgedFrancophone, " +
          "COUNT(CASE WHEN s.isAdult = true AND s.frenchProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '05' THEN 1 END) AS adultFrancophone, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '05' AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalFrancophone " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID AND s.sdcSchoolCollectionStudentStatusCode != 'ERROR' " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<CsfFrenchHeadcountResult> getCsfFrenchHeadcountsBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(CASE WHEN s.isSchoolAged AND s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' AND ell.yearsInEll < 6 THEN 1 END) AS schoolAgedOneThroughFive, " +
          "COUNT(CASE WHEN s.isSchoolAged AND s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' AND ell.yearsInEll > 5 THEN 1 END) AS schoolAgedSixPlus, " +
          "COUNT(CASE WHEN s.isSchoolAged AND s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' THEN 1 END) AS schoolAgedTotals, " +
          "COUNT(CASE WHEN s.isAdult AND s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' AND ell.yearsInEll < 6 THEN 1 END) AS adultOneThroughFive, " +
          "COUNT(CASE WHEN s.isAdult AND s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' AND ell.yearsInEll > 5 THEN 1 END) AS adultSixPlus, " +
          "COUNT(CASE WHEN s.isAdult AND s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' THEN 1 END) AS adultTotals, " +
          "COUNT(CASE WHEN s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' AND ell.yearsInEll < 6 THEN 1 END) AS allOneThroughFive, " +
          "COUNT(CASE WHEN s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' AND ell.yearsInEll > 5 THEN 1 END) AS allSixPlus, " +
          "COUNT(CASE WHEN s.ellNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '17' THEN 1 END) AS totalEllStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN SdcStudentEllEntity ell " +
          "ON s.assignedStudentId = ell.studentID " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionId AND s.sdcSchoolCollectionStudentStatusCode != 'ERROR' " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<EllHeadcountResult> getEllHeadcountsBySchoolId(@Param("sdcSchoolCollectionId") UUID sdcSchoolCollectionId);

  @Query("SELECT " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '17' AND s.ellNonEligReasonCode IS NULL THEN 1 END) AS eligibleStudents, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '17' THEN 1 END) AS reportedStudents, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '17' AND ell.yearsInEll < 6 THEN 1 END) AS oneToFiveYears, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '17' AND ell.yearsInEll > 5 THEN 1 END) AS sixPlusYears, " +
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
          "COUNT(CASE WHEN ep.enrolledProgramCode = '08' AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalCoreFrench, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '08' THEN 1 END) AS reportedCoreFrench, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '11' AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalEarlyFrench, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '11' THEN 1 END) AS reportedEarlyFrench, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '14' AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalLateFrench, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '14' THEN 1 END) AS reportedLateFrench, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '05' AND s.frenchProgramNonEligReasonCode IS NULL THEN 1 END) AS totalFrancophone, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '05' THEN 1 END) AS reportedFrancophone, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID")
  FrenchHeadcountHeaderResult getFrenchHeadersBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '40' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS eligCareerPrep, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '40' THEN 1 END) AS reportedCareerPrep, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '41' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS eligCoopEduc, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '41' THEN 1 END) AS reportedCoopEduc, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '42' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS eligApprentice, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '42' THEN 1 END) AS reportedApprentice, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '43' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS eligTechOrYouth, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '43' THEN 1 END) AS reportedTechOrYouth, " +
          "COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID")
  CareerHeadcountHeaderResult getCareerHeadersBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN 1 END) AS preparationXA, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN 1 END) AS preparationXB, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN 1 END) AS preparationXC, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN 1 END) AS preparationXD, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN 1 END) AS preparationXE, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN 1 END) AS preparationXF, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN 1 END) AS preparationXG, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN 1 END) AS preparationXH, " +
          "COUNT(CASE WHEN s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '40' THEN 1 END) AS preparationTotal, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN 1 END) AS coopXA, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN 1 END) AS coopXB, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN 1 END) AS coopXC, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN 1 END) AS coopXD, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN 1 END) AS coopXE, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN 1 END) AS coopXF, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN 1 END) AS coopXG, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN 1 END) AS coopXH, " +
          "COUNT(CASE WHEN s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '41' THEN 1 END) AS coopTotal, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN 1 END) AS apprenticeXA, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN 1 END) AS apprenticeXB, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN 1 END) AS apprenticeXC, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN 1 END) AS apprenticeXD, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN 1 END) AS apprenticeXE, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN 1 END) AS apprenticeXF, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN 1 END) AS apprenticeXG, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN 1 END) AS apprenticeXH, " +
          "COUNT(CASE WHEN s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '42' THEN 1 END) AS apprenticeTotal, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN 1 END) AS techYouthXA, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN 1 END) AS techYouthXB, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN 1 END) AS techYouthXC, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN 1 END) AS techYouthXD, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN 1 END) AS techYouthXE, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN 1 END) AS techYouthXF, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN 1 END) AS techYouthXG, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN 1 END) AS techYouthXH, " +
          "COUNT(CASE WHEN s.careerProgramNonEligReasonCode IS NULL AND ep.enrolledProgramCode = '43' THEN 1 END) AS techYouthTotal, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XA' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS allXA, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XB' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS allXB, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XC' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS allXC, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XD' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS allXD, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XE' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS allXE, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XF' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS allXF, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XG' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS allXG, " +
          "COUNT(CASE WHEN s.careerProgramCode = 'XH' AND s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS allXH, " +
          "COUNT(CASE WHEN s.careerProgramNonEligReasonCode IS NULL THEN 1 END) AS allTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID AND s.sdcSchoolCollectionStudentStatusCode != 'ERROR' " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<CareerHeadcountResult> getCareerHeadcountsBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query("SELECT " +
          "s.enrolledGradeCode AS enrolledGradeCode, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '29' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN 1 END) AS indigenousLanguageTotal, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '33' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN 1 END) AS indigenousSupportTotal, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode = '36' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN 1 END) AS otherProgramTotal, " +
          "COUNT(CASE WHEN ep.enrolledProgramCode in ('29', '33', '36') AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN 1 END) AS allSupportProgramTotal " +
          "FROM SdcSchoolCollectionStudentEntity s " +
          "LEFT JOIN s.sdcStudentEnrolledProgramEntities ep " +
          "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID AND s.sdcSchoolCollectionStudentStatusCode != 'ERROR' " +
          "GROUP BY s.enrolledGradeCode " +
          "ORDER BY s.enrolledGradeCode")
  List<IndigenousHeadcountResult> getIndigenousHeadcountsBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Query(value = """
    SELECT
    COUNT(CASE WHEN ep.enrolledProgramCode = '29' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN 1 END) AS eligIndigenousLanguage,
    COUNT(CASE WHEN ep.enrolledProgramCode = '29' THEN 1 END) AS reportedIndigenousLanguage,
    COUNT(CASE WHEN ep.enrolledProgramCode = '33' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN 1 END) AS eligIndigenousSupport,
    COUNT(CASE WHEN ep.enrolledProgramCode = '33' THEN 1 END) AS reportedIndigenousSupport,
    COUNT(CASE WHEN ep.enrolledProgramCode = '36' AND s.indigenousSupportProgramNonEligReasonCode IS NULL THEN 1 END) AS eligOtherProgram,
    COUNT(CASE WHEN ep.enrolledProgramCode = '36' THEN 1 END) AS reportedOtherProgram,
    COUNT(DISTINCT CASE WHEN s.nativeAncestryInd = 'Y' THEN s.sdcSchoolCollectionStudentID END) AS studentsWithIndigenousAncestry,
    COUNT(DISTINCT CASE WHEN s.schoolFundingCode ='20' THEN s.sdcSchoolCollectionStudentID END) AS studentsWithFundingCode20,
    COUNT(DISTINCT s.sdcSchoolCollectionStudentID) AS allStudents
    FROM SdcSchoolCollectionStudentEntity s LEFT JOIN s.sdcStudentEnrolledProgramEntities ep
    WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID
    """)
  IndigenousHeadcountHeaderResult getIndigenousHeadersBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

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
    "WHERE s.sdcSchoolCollection.sdcSchoolCollectionID = :sdcSchoolCollectionID " +
    "GROUP BY s.enrolledGradeCode " +
    "ORDER BY s.enrolledGradeCode")
  List<SpecialEdHeadcountResult> getSpecialEdHeadcountsBySchoolId(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

  @Modifying
  @Query(value = "DELETE FROM SDC_SCHOOL_COLLECTION_STUDENT WHERE SDC_SCHOOL_COLLECTION_ID  = :sdcSchoolCollectionID", nativeQuery = true)
  void deleteAllBySdcSchoolCollectionID(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);
}
