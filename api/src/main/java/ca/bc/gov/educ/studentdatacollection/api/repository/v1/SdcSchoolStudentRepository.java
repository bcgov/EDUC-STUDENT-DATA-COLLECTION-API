package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolBatchEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SdcSchoolStudentRepository extends JpaRepository<SdcSchoolStudentEntity, UUID>, JpaSpecificationExecutor<SdcSchoolStudentEntity> {
  List<SdcSchoolStudentEntity> findAllBySdcSchoolBatchEntity(SdcSchoolBatchEntity sdcSchoolBatchEntity);
}
