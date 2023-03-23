package ca.bc.gov.educ.studentdatacollection.api.repository.v1;


import ca.bc.gov.educ.studentdatacollection.api.model.v1.Saga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Saga repository.
 */
@Repository
public interface SagaRepository extends JpaRepository<Saga, UUID>, JpaSpecificationExecutor<Saga> {

  Optional<Saga> findBySdcStudentIDAndSagaName(UUID nominalRollStudentID, String sagaName);

  List<Saga> findAllByCreateDateBefore(LocalDateTime createDateToCompare);

  List<Saga> findTop100ByStatusInOrderByCreateDate(List<String> statuses);

  long countAllByStatusIn(List<String> statuses);

  List<Saga> findAllByProcessingYearAndStatusIn(String processingYear, List<String> statuses);
}
