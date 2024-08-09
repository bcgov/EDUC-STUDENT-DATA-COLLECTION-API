package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;

public interface SdcSchoolCollectionStudentPaginationRepositoryLight {
    Slice<SdcSchoolCollectionStudentPaginationEntity> findAllWithoutCount(Specification<SdcSchoolCollectionStudentPaginationEntity> spec, Pageable pageable);
}
