package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionHistoryRepository extends JpaRepository<SdcSchoolCollectionHistoryEntity, UUID> {

    @Modifying
    @Query(value = """
           UPDATE SDC_SCHOOL_COLLECTION
           SET SDC_SCHOOL_COLLECTION_STATUS_CODE = :schoolStatus, update_user = 'STUDENT_DATA_COLLECTION_API', update_date = CURRENT_TIMESTAMP
           WHERE sdc_school_collection_id IN 
           (:sdcSchoolCollectionID)
           """, nativeQuery = true)
    void updateSchoolCollectionStatus(List<UUID> sdcSchoolCollectionID, String schoolStatus);
}
