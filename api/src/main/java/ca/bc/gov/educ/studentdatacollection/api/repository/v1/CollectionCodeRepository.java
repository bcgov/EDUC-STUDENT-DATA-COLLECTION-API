package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionCodeRepository extends JpaRepository<CollectionCodeEntity, String> {

  List<CollectionCodeEntity> findAllByOpenDateBefore(LocalDateTime dateTime);

}
