package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcRepository;
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
  private final SdcRepository collectionRepository;

  @Autowired
  public CollectionService(SdcRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

  public List<SdcEntity> getAllCollectionsList() {
    return collectionRepository.findAll();
  }

  public Optional<SdcEntity> getCollection(UUID collectionID) {
    return collectionRepository.findById(collectionID);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcEntity createCollection(Collection collection) {
    SdcEntity collectionEntity = CollectionMapper.mapper.toModel(collection);
    TransformUtil.uppercaseFields(collectionEntity);
    collectionRepository.save(collectionEntity);

    return collectionEntity;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteCollection(UUID collectionID) {
    Optional<SdcEntity> entityOptional = collectionRepository.findById(collectionID);
    SdcEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(SdcEntity.class, "collectionID", collectionID.toString()));
    collectionRepository.delete(entity);
  }

}
