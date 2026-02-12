package ca.bc.gov.educ.studentdatacollection.api.repository.v1.impl;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Stream;

/**
 * Implementation of custom repository methods for streaming school collections.
 */
@Repository
public class SdcSchoolCollectionRepositoryImpl implements SdcSchoolCollectionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Stream<SdcSchoolCollectionEntity> streamAllBySdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
        TypedQuery<SdcSchoolCollectionEntity> query = entityManager.createQuery(
                "SELECT ssc FROM SdcSchoolCollectionEntity ssc WHERE ssc.sdcDistrictCollectionID = :districtCollectionID",
                SdcSchoolCollectionEntity.class
        );
        query.setParameter("districtCollectionID", sdcDistrictCollectionID);

        return query.getResultStream();
    }
}


