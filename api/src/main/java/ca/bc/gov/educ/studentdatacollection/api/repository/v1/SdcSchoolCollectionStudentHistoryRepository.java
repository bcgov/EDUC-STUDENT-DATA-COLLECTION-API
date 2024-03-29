package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionStudentHistoryRepository extends JpaRepository<SdcSchoolCollectionStudentHistoryEntity, UUID> {
    List<SdcSchoolCollectionStudentHistoryEntity> findAllBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);

    @Modifying
    @Query(value = "DELETE FROM SdcSchoolCollectionStudentHistoryEntity WHERE sdcSchoolCollectionStudentID in (:sdcSchoolCollectionStudentIDs)")
    void deleteBySdcSchoolCollectionStudentIDs(List<UUID> sdcSchoolCollectionStudentIDs);
}
