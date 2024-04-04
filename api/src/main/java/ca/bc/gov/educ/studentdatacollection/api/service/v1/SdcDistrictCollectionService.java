package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SdcDistrictCollectionService {

  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final CollectionRepository collectionRepository;

  @Autowired
  public SdcDistrictCollectionService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, CollectionRepository collectionRepository) {
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
    this.collectionRepository = collectionRepository;
  }

  public SdcDistrictCollectionEntity getSdcDistrictCollection(UUID sdcDistrictCollectionID) {
    Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntity =  sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
    if(sdcDistrictCollectionEntity.isPresent()) {
      return sdcDistrictCollectionEntity.get();
    } else {
      throw new EntityNotFoundException(SdcDistrictCollectionEntity.class, "SdcDistrictCollection for sdcDistrictCollectionID", sdcDistrictCollectionID.toString());
    }
  }

  public SdcDistrictCollectionEntity getActiveSdcDistrictCollectionByDistrictID(UUID districtID) {
    return sdcDistrictCollectionRepository.findByDistrictIDAndSdcDistrictCollectionStatusCodeNotIgnoreCase(districtID, SdcDistrictCollectionStatus.COMPLETED.getCode()).orElseThrow(() -> new EntityNotFoundException(SdcDistrictCollectionEntity.class, "Collection for district Id", districtID.toString()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcDistrictCollectionEntity createSdcDistrictCollectionByCollectionID(SdcDistrictCollectionEntity sdcDistrictCollectionEntity ,UUID collectionID) {
    Optional<CollectionEntity> collectionEntityOptional = collectionRepository.findById(collectionID);
    CollectionEntity collectionEntity = collectionEntityOptional.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "collectionID", collectionID.toString()));

    TransformUtil.uppercaseFields(sdcDistrictCollectionEntity);
    sdcDistrictCollectionEntity.setCollectionEntity(collectionEntity);
    return sdcDistrictCollectionRepository.save(sdcDistrictCollectionEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteSdcDistrictCollection(UUID sdcDistrictCollectionID) {
    Optional<SdcDistrictCollectionEntity> entityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
    SdcDistrictCollectionEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID", sdcDistrictCollectionID.toString()));
    sdcDistrictCollectionRepository.delete(entity);
  }
}
