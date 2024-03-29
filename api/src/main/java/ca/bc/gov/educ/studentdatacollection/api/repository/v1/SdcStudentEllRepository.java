package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SdcStudentEllRepository extends JpaRepository<SdcStudentEllEntity, UUID> {

    Optional<SdcStudentEllEntity> findByStudentID(UUID studentID);

}
