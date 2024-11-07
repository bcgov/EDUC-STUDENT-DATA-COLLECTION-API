package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SdcDuplicateRepository extends JpaRepository<SdcDuplicateEntity, UUID> {
    void deleteAllBySdcDuplicateStudentEntities_SdcSchoolCollectionID(UUID sdcSchoolCollectionID);
}
