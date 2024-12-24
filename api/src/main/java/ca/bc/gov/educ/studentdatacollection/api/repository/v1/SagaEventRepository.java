package ca.bc.gov.educ.studentdatacollection.api.repository.v1;


import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Saga event repository.
 */
@Repository
public interface SagaEventRepository extends JpaRepository<SagaEventStatesEntity, UUID> {
  /**
   * Find by saga list.
   *
   * @param saga the saga
   * @return the list
   */
  List<SagaEventStatesEntity> findBySaga(SdcSagaEntity saga);

  /**
   * Find by saga and saga event outcome and saga event state and saga step number optional.
   *
   * @param saga         the saga
   * @param eventOutcome the event outcome
   * @param eventState   the event state
   * @param stepNumber   the step number
   * @return the optional
   */
  Optional<SagaEventStatesEntity> findBySagaAndSagaEventOutcomeAndSagaEventStateAndSagaStepNumber(SdcSagaEntity saga, String eventOutcome, String eventState, int stepNumber);

  @Transactional
  @Modifying
  @Query(value = "delete from SDC_SAGA_EVENT_STATES e where e.SAGA_ID in (select s.SAGA_ID from SDC_SAGA s where s.STATUS in :cleanupStatus and s.CREATE_DATE <= :createDate and s.SAGA_ID in :sagaIDsToDelete)", nativeQuery = true)
  void deleteByStatusAndCreateDateBefore(List<String> cleanupStatus, LocalDateTime createDate, List<UUID> sagaIDsToDelete);

}
