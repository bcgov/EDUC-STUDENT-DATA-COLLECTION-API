package ca.bc.gov.educ.studentdatacollection.api.repository.v1.impl;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentPaginationRepositoryLight;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SdcSchoolCollectionStudentPaginationRepositoryLightImpl implements SdcSchoolCollectionStudentPaginationRepositoryLight {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Slice<SdcSchoolCollectionStudentPaginationEntity> findAllWithoutCount(Specification<SdcSchoolCollectionStudentPaginationEntity> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SdcSchoolCollectionStudentPaginationEntity> query = cb.createQuery(SdcSchoolCollectionStudentPaginationEntity.class);
        Root<SdcSchoolCollectionStudentPaginationEntity> root = query.from(SdcSchoolCollectionStudentPaginationEntity.class);

        query.where(spec.toPredicate(root, query, cb));

        TypedQuery<SdcSchoolCollectionStudentPaginationEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<SdcSchoolCollectionStudentPaginationEntity> content = typedQuery.getResultList();
        boolean hasNext = content.size() == pageable.getPageSize();

        return new SliceImpl<>(content, pageable, hasNext);
    }
}
