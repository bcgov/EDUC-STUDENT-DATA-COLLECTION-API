package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionSubmissionSignatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SdcDistrictCollectionSubmissionSignatureRepository extends JpaRepository<SdcDistrictCollectionSubmissionSignatureEntity, UUID> {
}
