package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
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
  private final RestUtils restUtils;

  @Autowired
  public CollectionService(CollectionRepository collectionRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
    this.collectionRepository = collectionRepository;
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.restUtils = restUtils;
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
    List<MonitorSdcDistrictCollectionQueryResponse> monitorSdcDistrictCollectionsQueryResponses = sdcDistrictCollectionRepository.findAllSdcDistrictCollectionMonitoringByCollectionID(collectionID);

    List<MonitorSdcDistrictCollection> monitorSdcDistrictCollections = new ArrayList<>();
    monitorSdcDistrictCollectionsQueryResponses.forEach(monitorSdcDistrictCollectionQueryResponse -> {
      District district = this.restUtils.getDistrictByDistrictID(monitorSdcDistrictCollectionQueryResponse.getDistrictID().toString()).orElseThrow(() -> new StudentDataCollectionAPIRuntimeException("SdcSchoolCollection :: " + monitorSdcDistrictCollectionQueryResponse.getSdcDistrictCollectionID() + " has invalid districtId :: " + monitorSdcDistrictCollectionQueryResponse.getDistrictID()));

      MonitorSdcDistrictCollection monitorSdcDistrictCollection = new MonitorSdcDistrictCollection();
      monitorSdcDistrictCollection.setDistrictID(UUID.fromString(district.getDistrictId()));
      monitorSdcDistrictCollection.setDistrictTitle(district.getDistrictNumber() + " - " + district.getDisplayName());
      monitorSdcDistrictCollection.setSdcDistrictCollectionStatusCode(monitorSdcDistrictCollectionQueryResponse.getSdcDistrictCollectionStatusCode());
      monitorSdcDistrictCollection.setSdcDistrictCollectionId(monitorSdcDistrictCollectionQueryResponse.getSdcDistrictCollectionID());
      monitorSdcDistrictCollection.setNumSubmittedSchools(monitorSdcDistrictCollectionQueryResponse.getSubmittedSchools() + "/" + monitorSdcDistrictCollectionQueryResponse.getTotalSchools());
      monitorSdcDistrictCollection.setUnresolvedProgramDuplicates(monitorSdcDistrictCollectionQueryResponse.getUnresolvedProgramDuplicates());
      monitorSdcDistrictCollection.setUnresolvedEnrollmentDuplicates(monitorSdcDistrictCollectionQueryResponse.getUnresolvedEnrollmentDuplicates());

      monitorSdcDistrictCollections.add(monitorSdcDistrictCollection);
    });
    return monitorSdcDistrictCollections;
  }

  public MonitorIndySdcSchoolCollectionsResponse getMonitorIndySdcSchoolCollectionResponse(UUID collectionID) {
    Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
    if (entityOptional.isEmpty()) {
      throw new EntityNotFoundException(CollectionEntity.class, "CollectionID", collectionID.toString());
    }
    List<MonitorIndySdcSchoolCollectionQueryResponse> monitorSdcSchoolCollectionQueryResponses = sdcSchoolCollectionRepository.findAllIndySdcSchoolCollectionMonitoringBySdcCollectionId(collectionID);
    List<MonitorIndySdcSchoolCollection> monitorSdcSchoolCollections = new ArrayList<>();
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
      monitorSdcSchoolCollection.setUnresolvedProgramDuplicates(monitorSdcSchoolCollectionQueryResponse.getUnresolvedProgramDuplicates());
      monitorSdcSchoolCollection.setUnresolvedEnrollmentDuplicates(monitorSdcSchoolCollectionQueryResponse.getUnresolvedEnrollmentDuplicates());

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

  public List<MonitorSdcSchoolCollection> getMonitorSdcSchoolCollectionResponse(UUID collectionID) {
    Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
    if (entityOptional.isEmpty()) {
      throw new EntityNotFoundException(CollectionEntity.class, "CollectionID", collectionID.toString());
    }
    List<MonitorSdcSchoolCollectionQueryResponse> monitorSdcSchoolCollectionQueryResponses = sdcSchoolCollectionRepository.findAllSdcSchoolCollectionMonitoringBySdcCollectionId(collectionID);
    List<MonitorSdcSchoolCollection> monitorSdcSchoolCollections = new ArrayList<>();
    monitorSdcSchoolCollectionQueryResponses.forEach(monitorSdcSchoolCollectionQueryResponse -> {
      SchoolTombstone schoolTombstone = this.restUtils.getSchoolBySchoolID(monitorSdcSchoolCollectionQueryResponse.getSchoolId().toString()).orElseThrow(() -> new StudentDataCollectionAPIRuntimeException("SdcSchoolCollection :: " + monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionId() + " has invalid schoolId :: " + monitorSdcSchoolCollectionQueryResponse.getSchoolId()));
      MonitorSdcSchoolCollection monitorSdcSchoolCollection = new MonitorSdcSchoolCollection();

      monitorSdcSchoolCollection.setSchoolTitle(schoolTombstone.getMincode() + " - " + schoolTombstone.getDisplayName());
      monitorSdcSchoolCollection.setSchoolId(schoolTombstone.getSchoolId());
      monitorSdcSchoolCollection.setSdcSchoolCollectionId(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionId());
      monitorSdcSchoolCollection.setUploadDate(monitorSdcSchoolCollectionQueryResponse.getUploadDate());
      monitorSdcSchoolCollection.setErrors(monitorSdcSchoolCollectionQueryResponse.getErrors());
      monitorSdcSchoolCollection.setInfoWarnings(monitorSdcSchoolCollectionQueryResponse.getInfoWarnings());
      monitorSdcSchoolCollection.setFundingWarnings(monitorSdcSchoolCollectionQueryResponse.getFundingWarnings());

      monitorSdcSchoolCollection.setSchoolStatus(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionStatusCode());
      monitorSdcSchoolCollection.setSubmittedToDistrict(isStatusConfirmed(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionStatusCode(), SdcSchoolCollectionStatus.SUBMITTED.getCode(), SdcSchoolCollectionStatus.COMPLETED.getCode()));

      monitorSdcSchoolCollections.add(monitorSdcSchoolCollection);
    });
    return monitorSdcSchoolCollections;
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
