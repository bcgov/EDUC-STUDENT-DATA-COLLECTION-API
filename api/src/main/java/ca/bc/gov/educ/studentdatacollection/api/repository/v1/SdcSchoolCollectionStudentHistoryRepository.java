package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcSchoolCollectionStudentHistoryRepository extends JpaRepository<SdcSchoolCollectionStudentHistoryEntity, UUID> {
    List<SdcSchoolCollectionStudentHistoryEntity> findAllBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);

    void deleteAllBySdcSchoolCollectionID(UUID sdcSchoolCollectionID);
}
