package ca.bc.gov.educ.studentdatacollection.api.repository.v1.impl;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentPaginationRepositoryLight;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
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

        if (pageable.getSort().isSorted()) {
            query.orderBy(pageable.getSort().stream()
                    .map(order -> {
                        if (order.getProperty().equals("sdcSchoolCollection")) {
                            Join<Object, Object> schoolCollectionJoin = root.join("sdcSchoolCollection");
                            return order.isAscending() ? cb.asc(schoolCollectionJoin.get("sdcSchoolCollectionID"))
                                    : cb.desc(schoolCollectionJoin.get("sdcSchoolCollectionID"));
                        } else {
                            return order.isAscending() ? cb.asc(root.get(order.getProperty()))
                                    : cb.desc(root.get(order.getProperty()));
                        }
                    })
                    .toList());
        }

        TypedQuery<SdcSchoolCollectionStudentPaginationEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(pageable.getPageNumber() > 0 ? (int) pageable.getOffset() : 0);
        typedQuery.setMaxResults(pageable.getPageSize() + 1);

        List<SdcSchoolCollectionStudentPaginationEntity> content = typedQuery.getResultList();
        boolean hasNext = pageable.isPaged() && content.size() > pageable.getPageSize();

        return new SliceImpl<>(hasNext ? content.subList(0, pageable.getPageSize()) : content, pageable, hasNext);
    }
}
