package ca.bc.gov.educ.studentdatacollection.api.repository.v1;


import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Saga repository.
 */
@Repository
public interface SagaRepository extends JpaRepository<SdcSagaEntity, UUID>, JpaSpecificationExecutor<SdcSagaEntity> {

  Optional<SdcSagaEntity> findBySdcSchoolCollectionStudentIDAndSagaNameAndStatusNot(UUID sdcSchoolCollectionStudentID, String sagaName, String status);

  Optional<SdcSagaEntity> findBySdcSchoolCollectionIDAndSagaNameAndStatusEquals(UUID sdcSchoolCollectionID, String sagaName, String status);

  List<SdcSagaEntity> findTop500ByStatusInOrderByCreateDate(List<String> statuses);

  Optional<SdcSagaEntity> findByCollectionIDAndSagaNameAndStatusNot(UUID collectionID, String sagaName, String status);

  long countAllByStatusIn(List<String> statuses);

  long countAllByStatusInAndCreateDateBefore(List<String> statuses, LocalDateTime createDate);

  @Query(value = "SELECT s.SAGA_ID FROM SDC_SAGA s WHERE s.STATUS in :cleanupStatus and s.CREATE_DATE <= :createDate LIMIT :batchSize", nativeQuery = true)
  List<UUID> findByStatusInAndCreateDateBefore(List<String> cleanupStatus, LocalDateTime createDate, int batchSize);

  @Transactional
  @Modifying
  @Query(value = "DELETE FROM SDC_SAGA saga where saga.STATUS in :cleanupStatus and saga.CREATE_DATE <= :createDate AND saga.SAGA_ID in :sagaIDsToDelete", nativeQuery = true)
  void deleteByStatusAndCreateDateBefore(List<String> cleanupStatus, LocalDateTime createDate, List<UUID> sagaIDsToDelete);

  @Transactional
  @Modifying
  @Query(value = "DELETE FROM SDC_SAGA saga " +
          "  WHERE saga.STATUS = 'COMPLETED' AND saga.SAGA_NAME = 'STUDENT_DATA_COLLECTION_STUDENT_MIGRATION_SAGA'", nativeQuery = true)
  void deleteCompletedMigrationSagas();

}
