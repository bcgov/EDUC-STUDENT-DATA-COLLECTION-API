package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationShallowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SdcSchoolCollectionStudentPaginationShallowRepository extends JpaRepository<SdcSchoolCollectionStudentPaginationShallowEntity, UUID>, JpaSpecificationExecutor<SdcSchoolCollectionStudentPaginationShallowEntity> {

}
