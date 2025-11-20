package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryPaginationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SdcSchoolCollectionStudentHistoryPaginationRepository extends JpaRepository<SdcSchoolCollectionStudentHistoryPaginationEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentHistoryPaginationEntity> {
}
