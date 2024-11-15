package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CollectionService {

  @Getter(AccessLevel.PRIVATE)
  private final CollectionRepository collectionRepository;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final SdcDuplicatesService sdcDuplicatesService;
  private final RestUtils restUtils;

  @Autowired
  public CollectionService(CollectionRepository collectionRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcDuplicatesService sdcDuplicatesService, RestUtils restUtils) {
    this.collectionRepository = collectionRepository;
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
      this.sdcDuplicatesService = sdcDuplicatesService;
      this.restUtils = restUtils;
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
    List<MonitorSdcDistrictCollectionQueryResponse> monitorSdcDistrictCollectionsQueryResponses = sdcDistrictCollectionRepository.findAllSdcDistrictCollectionMonitoringByCollectionID(collectionID);
    var duplicates = sdcDuplicatesService.getAllProvincialDuplicatesByCollectionID(collectionID);
    var countMap = getDistrictDuplicatesCountMap(duplicates);

    List<MonitorSdcDistrictCollection> monitorSdcDistrictCollections = new ArrayList<>();
    monitorSdcDistrictCollectionsQueryResponses.forEach(monitorSdcDistrictCollectionQueryResponse -> {
      District district = this.restUtils.getDistrictByDistrictID(monitorSdcDistrictCollectionQueryResponse.getDistrictID().toString()).orElseThrow(() -> new StudentDataCollectionAPIRuntimeException("SdcSchoolCollection :: " + monitorSdcDistrictCollectionQueryResponse.getSdcDistrictCollectionID() + " has invalid districtId :: " + monitorSdcDistrictCollectionQueryResponse.getDistrictID()));

      MonitorSdcDistrictCollection monitorSdcDistrictCollection = new MonitorSdcDistrictCollection();
      monitorSdcDistrictCollection.setDistrictID(UUID.fromString(district.getDistrictId()));
      monitorSdcDistrictCollection.setDistrictTitle(district.getDistrictNumber() + " - " + district.getDisplayName());
      monitorSdcDistrictCollection.setSdcDistrictCollectionStatusCode(monitorSdcDistrictCollectionQueryResponse.getSdcDistrictCollectionStatusCode());
      monitorSdcDistrictCollection.setSdcDistrictCollectionId(monitorSdcDistrictCollectionQueryResponse.getSdcDistrictCollectionID());
      monitorSdcDistrictCollection.setNumSubmittedSchools(monitorSdcDistrictCollectionQueryResponse.getSubmittedSchools() + "/" + monitorSdcDistrictCollectionQueryResponse.getTotalSchools());
      monitorSdcDistrictCollection.setUnresolvedDuplicates(getDuplicatesCount(monitorSdcDistrictCollectionQueryResponse.getSdcDistrictCollectionID(), countMap));

      monitorSdcDistrictCollections.add(monitorSdcDistrictCollection);
    });
    return monitorSdcDistrictCollections;
  }

  private int getDuplicatesCount(UUID sdcSchoolOrDistrictCollectionID, HashMap<UUID, Integer> countMap){
    if(countMap.containsKey(sdcSchoolOrDistrictCollectionID)){
      return countMap.get(sdcSchoolOrDistrictCollectionID);
    }
    return 0;
  }

  private HashMap<UUID, Integer> getSchoolDuplicatesCountMap(List<SdcDuplicateEntity> dupes){
    var countMap = new HashMap<UUID, Integer>();

    dupes.forEach(sdcDuplicateEntity -> {
      var studs = sdcDuplicateEntity.getSdcDuplicateStudentEntities();
      var schoolFoundSet = new HashSet<UUID>();
      studs.forEach(sdcDuplicateStudentEntity -> {
        if(!schoolFoundSet.contains(sdcDuplicateStudentEntity.getSdcSchoolCollectionID())) {
          if (!countMap.containsKey(sdcDuplicateStudentEntity.getSdcSchoolCollectionID())) {
            countMap.put(sdcDuplicateStudentEntity.getSdcSchoolCollectionID(), 1);
          } else {
            countMap.replace(sdcDuplicateStudentEntity.getSdcSchoolCollectionID(), countMap.get(sdcDuplicateStudentEntity.getSdcSchoolCollectionID()) + 1);
          }
          schoolFoundSet.add(sdcDuplicateStudentEntity.getSdcSchoolCollectionID());
        }
      });
    });
    return countMap;
  }

  private HashMap<UUID, Integer> getDistrictDuplicatesCountMap(List<SdcDuplicateEntity> dupes){
    var countMap = new HashMap<UUID, Integer>();

    dupes.forEach(sdcDuplicateEntity -> {
      var studs = sdcDuplicateEntity.getSdcDuplicateStudentEntities();
      var districtFoundSet = new HashSet<UUID>();
      studs.forEach(sdcDuplicateStudentEntity -> {
        if(!districtFoundSet.contains(sdcDuplicateStudentEntity.getSdcDistrictCollectionID())) {
          if (!countMap.containsKey(sdcDuplicateStudentEntity.getSdcDistrictCollectionID())) {
            countMap.put(sdcDuplicateStudentEntity.getSdcDistrictCollectionID(), 1);
          } else {
            countMap.replace(sdcDuplicateStudentEntity.getSdcDistrictCollectionID(), countMap.get(sdcDuplicateStudentEntity.getSdcDistrictCollectionID()) + 1);
          }
          districtFoundSet.add(sdcDuplicateStudentEntity.getSdcDistrictCollectionID());
        }
      });
    });
    return countMap;
  }

  public MonitorIndySdcSchoolCollectionsResponse getMonitorIndySdcSchoolCollectionResponse(UUID collectionID) {
    Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
    if (entityOptional.isEmpty()) {
      throw new EntityNotFoundException(CollectionEntity.class, "CollectionID", collectionID.toString());
    }
    List<MonitorIndySdcSchoolCollectionQueryResponse> monitorSdcSchoolCollectionQueryResponses = sdcSchoolCollectionRepository.findAllIndySdcSchoolCollectionMonitoringBySdcCollectionId(collectionID);
    List<MonitorIndySdcSchoolCollection> monitorSdcSchoolCollections = new ArrayList<>();
    var duplicates = sdcDuplicatesService.getAllProvincialDuplicatesByCollectionID(collectionID);
    var countMap = getSchoolDuplicatesCountMap(duplicates);

    monitorSdcSchoolCollectionQueryResponses.forEach(monitorSdcSchoolCollectionQueryResponse -> {
      SchoolTombstone schoolTombstone = this.restUtils.getSchoolBySchoolID(monitorSdcSchoolCollectionQueryResponse.getSchoolId().toString()).orElseThrow(() -> new StudentDataCollectionAPIRuntimeException("SdcSchoolCollection :: " + monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionId() + " has invalid schoolId :: " + monitorSdcSchoolCollectionQueryResponse.getSchoolId()));
      MonitorIndySdcSchoolCollection monitorSdcSchoolCollection = new MonitorIndySdcSchoolCollection();

      monitorSdcSchoolCollection.setSchoolTitle(schoolTombstone.getMincode() + " - " + schoolTombstone.getDisplayName());
      monitorSdcSchoolCollection.setSchoolId(schoolTombstone.getSchoolId());
      monitorSdcSchoolCollection.setSdcSchoolCollectionId(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionId());
      monitorSdcSchoolCollection.setUploadDate(monitorSdcSchoolCollectionQueryResponse.getUploadDate());
      monitorSdcSchoolCollection.setUploadReportDate(monitorSdcSchoolCollectionQueryResponse.getUploadReportDate());
      monitorSdcSchoolCollection.setHeadcount(monitorSdcSchoolCollectionQueryResponse.getHeadcount());
      monitorSdcSchoolCollection.setErrors(monitorSdcSchoolCollectionQueryResponse.getErrors());
      monitorSdcSchoolCollection.setInfoWarnings(monitorSdcSchoolCollectionQueryResponse.getInfoWarnings());
      monitorSdcSchoolCollection.setFundingWarnings(monitorSdcSchoolCollectionQueryResponse.getFundingWarnings());

      monitorSdcSchoolCollection.setSchoolStatus(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionStatusCode());
      monitorSdcSchoolCollection.setSubmittedToDistrict(isStatusConfirmed(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionStatusCode(), SdcSchoolCollectionStatus.SUBMITTED.getCode(), SdcSchoolCollectionStatus.COMPLETED.getCode()));
      monitorSdcSchoolCollection.setUnresolvedDuplicates(getDuplicatesCount(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionId(), countMap));

      monitorSdcSchoolCollections.add(monitorSdcSchoolCollection);
    });
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

  private boolean isStatusConfirmed(String statusCode, String... confirmedStatuses) {
    return Arrays.asList(confirmedStatuses).contains(statusCode);
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
