package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;

import java.util.UUID;
import java.util.stream.Stream;

/**
 * Custom repository interface for streaming school collections.
 */
public interface SdcSchoolCollectionRepositoryCustom {
    /**
     * @param sdcDistrictCollectionID the district collection ID
     * @return a Stream of SdcSchoolCollectionEntity
     */
    Stream<SdcSchoolCollectionEntity> streamAllBySdcDistrictCollectionID(UUID sdcDistrictCollectionID);
}

