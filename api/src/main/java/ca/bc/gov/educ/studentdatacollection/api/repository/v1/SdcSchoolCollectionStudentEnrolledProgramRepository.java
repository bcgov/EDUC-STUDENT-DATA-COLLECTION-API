package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SdcSchoolCollectionStudentEnrolledProgramRepository extends JpaRepository<SdcSchoolCollectionStudentEnrolledProgramEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentEnrolledProgramEntity> {

  @Modifying
  @Query(value = "DELETE FROM SDC_SCHOOL_COLLECTION_STUDENT_ENROLLED_PROGRAM WHERE SDC_SCHOOL_COLLECTION_STUDENT_ID IN(" +
          " SELECT SDC_SCHOOL_COLLECTION_STUDENT_ID FROM SDC_SCHOOL_COLLECTION_STUDENT WHERE SDC_SCHOOL_COLLECTION_ID  = :sdcSchoolCollectionID)", nativeQuery = true)
  void deleteAllBySdcSchoolCollectionID(@Param("sdcSchoolCollectionID") UUID sdcSchoolCollectionID);

}
