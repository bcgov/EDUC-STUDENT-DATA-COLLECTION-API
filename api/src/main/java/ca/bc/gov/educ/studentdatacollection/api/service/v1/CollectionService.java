package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.MonitorIndySdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.MonitorIndySdcSchoolCollectionsResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.MonitorSdcDistrictCollection;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CollectionService {

  @Getter(AccessLevel.PRIVATE)
  private final CollectionRepository collectionRepository;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  @Autowired
  public CollectionService(CollectionRepository collectionRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
    this.collectionRepository = collectionRepository;
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
  }

  public Optional<CollectionEntity> getCollection(UUID collectionID) {
    return collectionRepository.findById(collectionID);
  }

  public List<CollectionEntity> getCollections(String createUser) {
    return collectionRepository.findAllByCreateUser(createUser);
  }

  public CollectionEntity getActiveCollection(){
    Optional<CollectionEntity> collectionEntity =  collectionRepository.findActiveCollection();
    if(collectionEntity.isPresent()) {
      return collectionEntity.get();
    } else {
      throw new EntityNotFoundException(CollectionEntity.class, "Active Collection", null);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CollectionEntity createCollection(Collection collection) {
    CollectionEntity collectionEntity = CollectionMapper.mapper.toModel(collection);
    TransformUtil.uppercaseFields(collectionEntity);

    return collectionRepository.save(collectionEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteCollection(UUID collectionID) {
    Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
    CollectionEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "collectionID", collectionID.toString()));
    collectionRepository.delete(entity);
  }

  public List<MonitorSdcDistrictCollection> getMonitorSdcDistrictCollectionResponse(UUID collectionID) {
    Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
    if (entityOptional.isEmpty()) {
      throw new EntityNotFoundException(CollectionEntity.class, "CollectionID", collectionID.toString());
    }
    return new ArrayList<>();
  }

  public MonitorIndySdcSchoolCollectionsResponse getMonitorIndySdcSchoolCollectionResponse(UUID collectionID) {
    Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
    if (entityOptional.isEmpty()) {
      throw new EntityNotFoundException(CollectionEntity.class, "CollectionID", collectionID.toString());
    }
    List<MonitorIndySdcSchoolCollection> monitorSdcSchoolCollections = new ArrayList<>();

    MonitorIndySdcSchoolCollectionsResponse response = new MonitorIndySdcSchoolCollectionsResponse();
    response.setMonitorSdcSchoolCollections(monitorSdcSchoolCollections);

    response.setTotalFundingWarnings(monitorSdcSchoolCollections.stream().mapToLong(MonitorIndySdcSchoolCollection::getFundingWarnings).sum());
    response.setTotalErrors(monitorSdcSchoolCollections.stream().mapToLong(MonitorIndySdcSchoolCollection::getErrors).sum());
    response.setTotalInfoWarnings(monitorSdcSchoolCollections.stream().mapToLong(MonitorIndySdcSchoolCollection::getInfoWarnings).sum());
    response.setSchoolsSubmitted(monitorSdcSchoolCollections.stream().filter(MonitorIndySdcSchoolCollection::isSubmittedToDistrict).count());
    response.setSchoolsWithData(monitorSdcSchoolCollections.stream().filter(collection -> collection.getUploadDate() != null).count());
    response.setTotalSchools(monitorSdcSchoolCollections.size());
    return response;
  }

  public List<String> findDuplicatesInCollection(UUID collectionID, List<String> matchedAssignedIDs) {
    List<UUID> matchedAssignedUUIDs = matchedAssignedIDs.stream().map(UUID::fromString).toList();
    List<SdcSchoolCollectionStudentEntity> duplicateStudents = sdcSchoolCollectionStudentRepository.findAllDuplicateStudentsByCollectionID(collectionID, matchedAssignedUUIDs);
    return duplicateStudents.stream().map(s -> s.getAssignedStudentId().toString()).toList();
  }

  public List<SdcSchoolCollectionEntity> getSchoolCollectionsInCollection(UUID collectionID){
    return sdcSchoolCollectionRepository.findAllByCollectionEntityCollectionID(collectionID);
  }

  public List<SdcDistrictCollectionEntity> getDistrictCollectionsInCollection(UUID collectionID){
    return sdcDistrictCollectionRepository.findAllByCollectionEntityCollectionID(collectionID);
  }
}
