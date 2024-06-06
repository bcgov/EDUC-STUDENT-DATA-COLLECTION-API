package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionStatusCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SdcDistrictCollectionStatusCodeRepository extends JpaRepository<SdcDistrictCollectionStatusCodeEntity, String> {
}