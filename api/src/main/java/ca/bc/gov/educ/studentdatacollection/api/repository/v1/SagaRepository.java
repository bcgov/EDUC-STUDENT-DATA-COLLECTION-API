package ca.bc.gov.educ.studentdatacollection.api.repository.v1;


import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Saga repository.
 */
@Repository
public interface SagaRepository extends JpaRepository<SdcSagaEntity, UUID>, JpaSpecificationExecutor<SdcSagaEntity> {

  Optional<SdcSagaEntity> findBySdcSchoolCollectionStudentIDAndSagaNameAndStatusNot(UUID sdcSchoolCollectionStudentID, String sagaName, String status);

  List<SdcSagaEntity> findTop100ByStatusInOrderByCreateDate(List<String> statuses);

  long countAllByStatusIn(List<String> statuses);

}
