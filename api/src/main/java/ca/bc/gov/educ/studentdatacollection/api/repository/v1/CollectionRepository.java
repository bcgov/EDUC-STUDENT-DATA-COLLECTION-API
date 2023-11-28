package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionRepository extends JpaRepository<CollectionEntity, UUID> {

    List<CollectionEntity> findAllByCreateUser(String createUser);

    @Query(value="SELECT C FROM CollectionEntity C WHERE C.openDate <= CURRENT_TIMESTAMP AND C.closeDate >= CURRENT_TIMESTAMP")
    Optional<CollectionEntity> findActiveCollection();

}
