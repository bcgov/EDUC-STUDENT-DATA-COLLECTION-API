package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.IndependentSchoolFundingGroupSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IndependentSchoolFundingGroupSnapshotRepository extends JpaRepository<IndependentSchoolFundingGroupSnapshotEntity, UUID> {
    List<IndependentSchoolFundingGroupSnapshotEntity> findAllBySchoolIDAndAndCollectionID(UUID schoolID, UUID collectionID);

    List<IndependentSchoolFundingGroupSnapshotEntity> findAllByCollectionID(UUID collectionID);
}
