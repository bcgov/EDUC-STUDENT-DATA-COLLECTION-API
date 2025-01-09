package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionRepository extends JpaRepository<CollectionEntity, UUID>, JpaSpecificationExecutor<CollectionEntity> {

    List<CollectionEntity> findAllByCreateUser(String createUser);

    @Query(value="SELECT C FROM CollectionEntity C WHERE C.collectionStatusCode != 'COMPLETED'")
    Optional<CollectionEntity> findActiveCollection();

    @Modifying
    @Query(value = "UPDATE CollectionEntity C SET C.collectionStatusCode = :collectionStatusCode WHERE C.collectionID = :collectionID")
    void updateCollectionStatus(UUID collectionID, String collectionStatusCode);

    @Query(value="""
            SELECT C FROM CollectionEntity C 
            ORDER BY C.snapshotDate desc
            LIMIT 1""")
    Optional<CollectionEntity> findLastCollection();
}
