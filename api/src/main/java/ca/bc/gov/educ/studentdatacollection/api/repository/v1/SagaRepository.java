package ca.bc.gov.educ.studentdatacollection.api.repository.v1;


import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSaga;
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
public interface SagaRepository extends JpaRepository<SdcSaga, UUID>, JpaSpecificationExecutor<SdcSaga> {

  Optional<SdcSaga> findBySdcSchoolCollectionStudentIDAndSagaName(UUID nominalRollStudentID, String sagaName);

  List<SdcSaga> findAllByCreateDateBefore(LocalDateTime createDateToCompare);

  List<SdcSaga> findTop100ByStatusInOrderByCreateDate(List<String> statuses);

  long countAllByStatusIn(List<String> statuses);

  List<SdcSaga> findAllBySdcSchoolCollectionStudentIDAndStatusIn(String schoolID, List<String> statuses);
}
