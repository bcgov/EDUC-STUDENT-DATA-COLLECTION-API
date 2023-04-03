package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Service
public class CollectionService {

  @Getter(AccessLevel.PRIVATE)
  private final CollectionRepository collectionRepository;

  @Getter(AccessLevel.PRIVATE)
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Autowired
  public CollectionService(CollectionRepository collectionRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository1) {
    this.collectionRepository = collectionRepository;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository1;
  }

  public List<CollectionEntity> getAllCollectionsList() {
    return collectionRepository.findAll();
  }

  public CollectionEntity getCollectionBySchoolId(UUID schoolID) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntity =  sdcSchoolCollectionRepository.findCollectionBySchoolId(schoolID);
    if(sdcSchoolCollectionEntity.isPresent()) {
      return sdcSchoolCollectionEntity.get().getCollectionEntity();
    } else {
      throw new EntityNotFoundException(CollectionEntity.class, "Collection for school Id", schoolID.toString());
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CollectionEntity createCollection(Collection collection) {
    CollectionEntity collectionEntity = CollectionMapper.mapper.toModel(collection);
    TransformUtil.uppercaseFields(collectionEntity);
    collectionRepository.save(collectionEntity);

    return collectionEntity;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteCollection(UUID collectionID) {
    Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
    CollectionEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "collectionID", collectionID.toString()));
    collectionRepository.delete(entity);
  }

}
