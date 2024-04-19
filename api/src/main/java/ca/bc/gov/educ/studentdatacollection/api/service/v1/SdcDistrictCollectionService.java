package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class SdcDistrictCollectionService {

  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final CollectionRepository collectionRepository;
  private final SdcSchoolCollectionService sdcSchoolCollectionService;
  private final RestUtils restUtils;

  private static final String SDC_DISTRICT_COLLECTION_ID_KEY = "sdcDistrictCollectionID";

  @Autowired
  public SdcDistrictCollectionService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, CollectionRepository collectionRepository, RestUtils restUtils, SdcSchoolCollectionService sdcSchoolCollectionService) {
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.collectionRepository = collectionRepository;
    this.sdcSchoolCollectionService = sdcSchoolCollectionService;
    this.restUtils = restUtils;
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
  public SdcDistrictCollectionEntity createSdcDistrictCollectionByCollectionID(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, UUID collectionID) {
    Optional<CollectionEntity> collectionEntityOptional = collectionRepository.findById(collectionID);
    CollectionEntity collectionEntity = collectionEntityOptional.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "collectionID", collectionID.toString()));

    TransformUtil.uppercaseFields(sdcDistrictCollectionEntity);
    sdcDistrictCollectionEntity.setCollectionEntity(collectionEntity);
    return sdcDistrictCollectionRepository.save(sdcDistrictCollectionEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteSdcDistrictCollection(UUID sdcDistrictCollectionID) {
    Optional<SdcDistrictCollectionEntity> entityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
    SdcDistrictCollectionEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(SdcDistrictCollectionEntity.class, SDC_DISTRICT_COLLECTION_ID_KEY, sdcDistrictCollectionID.toString()));
    sdcDistrictCollectionRepository.delete(entity);
  }

  public List<HashMap<Object, Object>> getSchoolCollectionsInProgress(UUID sdcDistrictCollectionID) {
    List<SdcSchoolCollectionEntity> schoolCollectionRecords = sdcSchoolCollectionRepository.getListOfCollectionsInProgress(sdcDistrictCollectionID);
    List<HashMap<Object, Object>> fileSummaries = new ArrayList<>();
    for (SdcSchoolCollectionEntity schoolCollectionRecord:schoolCollectionRecords) {
      HashMap<Object, Object> collectionSummary = new HashMap<>();
      UUID schoolCollectionID = schoolCollectionRecord.getSdcSchoolCollectionID();
      UUID schoolID = schoolCollectionRecord.getSchoolID();
      Optional<School> school = restUtils.getSchoolBySchoolID(String.valueOf(schoolID));

      collectionSummary.put("sdcSchoolCollectionID", schoolCollectionID);
      collectionSummary.put("schoolID", schoolID);
      school.ifPresent(value -> collectionSummary.put("displayName", value.getDisplayName()));

      SdcFileSummary fileSummary = sdcSchoolCollectionService.isSdcSchoolCollectionBeingProcessed(schoolCollectionID);
      collectionSummary.put("fileSummary", fileSummary);
      fileSummaries.add(collectionSummary);
    }
    return fileSummaries;
  }

  public MonitorSdcSchoolCollectionsResponse getMonitorSdcSchoolCollectionResponse(UUID sdcDistrictCollectionId) {
    Optional<SdcDistrictCollectionEntity> entityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionId);
    if (entityOptional.isEmpty()) {
      throw new EntityNotFoundException(SdcDistrictCollectionEntity.class, SDC_DISTRICT_COLLECTION_ID_KEY, sdcDistrictCollectionId.toString());
    }
    List<MonitorSdcSchoolCollectionQueryResponse> monitorSdcSchoolCollectionQueryResponses = sdcSchoolCollectionRepository.findAllSdcSchoolCollectionMonitoringBySdcDistrictCollectionId(sdcDistrictCollectionId);
    List<MonitorSdcSchoolCollection> monitorSdcSchoolCollections = new ArrayList<>();
    monitorSdcSchoolCollectionQueryResponses.forEach(monitorSdcSchoolCollectionQueryResponse -> {
      School school = this.restUtils.getSchoolBySchoolID(monitorSdcSchoolCollectionQueryResponse.getSchoolId().toString()).orElseThrow(() -> new StudentDataCollectionAPIRuntimeException("SdcSchoolCollection :: " + monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionId() + " has invalid schoolId :: " + monitorSdcSchoolCollectionQueryResponse.getSchoolId()));
      MonitorSdcSchoolCollection monitorSdcSchoolCollection = new MonitorSdcSchoolCollection();

      monitorSdcSchoolCollection.setSchoolTitle(school.getMincode() + " - " + school.getDisplayName());
      monitorSdcSchoolCollection.setSdcSchoolCollectionId(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionId());
      monitorSdcSchoolCollection.setUploadDate(monitorSdcSchoolCollectionQueryResponse.getUploadDate());

      monitorSdcSchoolCollection.setErrors(monitorSdcSchoolCollectionQueryResponse.getErrors());
      monitorSdcSchoolCollection.setInfoWarnings(monitorSdcSchoolCollectionQueryResponse.getInfoWarnings());
      monitorSdcSchoolCollection.setFundingWarnings(monitorSdcSchoolCollectionQueryResponse.getFundingWarnings());

      monitorSdcSchoolCollection.setDetailsConfirmed(isStatusConfirmed(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionStatusCode(), SdcSchoolCollectionStatus.SCH_D_VRFD.getCode(), SdcSchoolCollectionStatus.SCH_C_VRFD.getCode(), SdcSchoolCollectionStatus.SUBMITTED.getCode()));
      monitorSdcSchoolCollection.setContactsConfirmed(isStatusConfirmed(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionStatusCode(), SdcSchoolCollectionStatus.SCH_C_VRFD.getCode(), SdcSchoolCollectionStatus.SUBMITTED.getCode()));
      monitorSdcSchoolCollection.setSubmittedToDistrict(isStatusConfirmed(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionStatusCode(), SdcSchoolCollectionStatus.SUBMITTED.getCode()));

      monitorSdcSchoolCollections.add(monitorSdcSchoolCollection);
    });
    MonitorSdcSchoolCollectionsResponse response = new MonitorSdcSchoolCollectionsResponse();
    response.setMonitorSdcSchoolCollections(monitorSdcSchoolCollections);

    response.setTotalFundingWarnings(monitorSdcSchoolCollections.stream().mapToLong(MonitorSdcSchoolCollection::getFundingWarnings).sum());
    response.setTotalErrors(monitorSdcSchoolCollections.stream().mapToLong(MonitorSdcSchoolCollection::getErrors).sum());
    response.setTotalInfoWarnings(monitorSdcSchoolCollections.stream().mapToLong(MonitorSdcSchoolCollection::getInfoWarnings).sum());

    response.setSchoolsDetailsConfirmed(monitorSdcSchoolCollections.stream().filter(MonitorSdcSchoolCollection::isDetailsConfirmed).count());
    response.setSchoolsContactsConfirmed(monitorSdcSchoolCollections.stream().filter(MonitorSdcSchoolCollection::isContactsConfirmed).count());
    response.setSchoolsSubmitted(monitorSdcSchoolCollections.stream().filter(MonitorSdcSchoolCollection::isSubmittedToDistrict).count());

    response.setSchoolsWithData(monitorSdcSchoolCollections.stream().filter(collection -> collection.getUploadDate() != null).count());
    response.setTotalSchools(monitorSdcSchoolCollections.size());
    return response;
  }

  private boolean isStatusConfirmed(String statusCode, String... confirmedStatuses) {
    return Arrays.asList(confirmedStatuses).contains(statusCode);
  }

  public SdcDistrictCollectionEntity updateSdcDistrictCollection(SdcDistrictCollectionEntity sdcDistrictCollectionEntity) {
    final Optional<SdcDistrictCollectionEntity> curSdcDistrictCollection = this.sdcDistrictCollectionRepository.findById(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
    if (curSdcDistrictCollection.isPresent()) {
      SdcDistrictCollectionEntity curGetSdcDistrictCollection = curSdcDistrictCollection.get();
      BeanUtils.copyProperties(sdcDistrictCollectionEntity, curGetSdcDistrictCollection, "districtID", "collectionID", SDC_DISTRICT_COLLECTION_ID_KEY);
      TransformUtil.uppercaseFields(curGetSdcDistrictCollection);
      curGetSdcDistrictCollection = this.sdcDistrictCollectionRepository.save(curGetSdcDistrictCollection);
      return curGetSdcDistrictCollection;
    } else {
      throw new EntityNotFoundException(SdcDistrictCollectionEntity.class, SDC_DISTRICT_COLLECTION_ID_KEY, sdcDistrictCollectionEntity.getSdcDistrictCollectionID().toString());
    }
  }
}
