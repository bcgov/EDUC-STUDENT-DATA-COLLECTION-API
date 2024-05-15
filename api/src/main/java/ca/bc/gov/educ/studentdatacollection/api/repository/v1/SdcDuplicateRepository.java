package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcDuplicateRepository extends JpaRepository<SdcDuplicateEntity, UUID> {
    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcDistrictCollectionID(UUID sdcDistrictCollectionID);

    List<SdcDuplicateEntity> findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionStudentEntity_SdcSchoolCollectionStudentIDIn(List<UUID> removedStudents);

}
