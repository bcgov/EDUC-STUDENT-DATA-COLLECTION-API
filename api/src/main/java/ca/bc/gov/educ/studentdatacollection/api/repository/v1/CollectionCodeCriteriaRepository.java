package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeCriteriaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionCodeCriteriaRepository extends JpaRepository<CollectionCodeCriteriaEntity, UUID> {

List<CollectionCodeCriteriaEntity> findAllByCollectionTypeCodeEntityEquals(
    CollectionTypeCodeEntity collectionCodeEntity);
}
