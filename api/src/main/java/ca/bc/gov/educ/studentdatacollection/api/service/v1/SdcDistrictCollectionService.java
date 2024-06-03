package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateLevelCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateResolutionCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class SdcDistrictCollectionService {

  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final CollectionRepository collectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final RestUtils restUtils;
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;
  private final SdcDuplicateRepository sdcDuplicateRepository;
  private final SdcDuplicatesService sdcDuplicatesService;
  private static final String SDC_DISTRICT_COLLECTION_ID_KEY = "sdcDistrictCollectionID";
  private static final String SDC_DUPLICATE_ID_KEY = "sdcDuplicateID";
  private static final SdcSchoolCollectionStudentMapper studentMapper = SdcSchoolCollectionStudentMapper.mapper;

  @Autowired
  public SdcDistrictCollectionService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, CollectionRepository collectionRepository, RestUtils restUtils, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService, SdcDuplicateRepository sdcDuplicateRepository, SdcDuplicatesService sdcDuplicatesService) {
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.collectionRepository = collectionRepository;
    this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
    this.restUtils = restUtils;
      this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
      this.sdcDuplicateRepository = sdcDuplicateRepository;
      this.sdcDuplicatesService = sdcDuplicatesService;
  }

  public SdcDistrictCollectionEntity getSdcDistrictCollection(UUID sdcDistrictCollectionID) {
    Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntity = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
    if (sdcDistrictCollectionEntity.isPresent()) {
      return sdcDistrictCollectionEntity.get();
    } else {
      throw new EntityNotFoundException(SdcDistrictCollectionEntity.class, "SdcDistrictCollection for sdcDistrictCollectionID", sdcDistrictCollectionID.toString());
    }
  }

  public SdcDistrictCollectionEntity getActiveSdcDistrictCollectionByDistrictID(UUID districtID) {
    Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntity = sdcDistrictCollectionRepository.findActiveCollectionByDistrictId(districtID);
    if (sdcDistrictCollectionEntity.isPresent()) {
      return sdcDistrictCollectionEntity.get();
    } else {
      throw new EntityNotFoundException(SdcDistrictCollectionEntity.class, "Collection for district Id", districtID.toString());
    }
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

  public List<SdcSchoolFileSummary> getSchoolCollectionsInProgress(UUID sdcDistrictCollectionID) {
    List<SdcSchoolCollectionEntity> schoolCollectionRecords = sdcSchoolCollectionRepository.findAllBySdcDistrictCollectionID(sdcDistrictCollectionID);
    List<SdcSchoolFileSummary> fileSummaries = new ArrayList<>();
    for (SdcSchoolCollectionEntity schoolCollectionRecord : schoolCollectionRecords) {
      UUID schoolCollectionID = schoolCollectionRecord.getSdcSchoolCollectionID();

      long totalCount = sdcSchoolCollectionStudentRepository.countBySdcSchoolCollection_SdcSchoolCollectionID(schoolCollectionID);
      long loadedCount = sdcSchoolCollectionStudentRepository.countBySdcSchoolCollectionStudentStatusCodeAndSdcSchoolCollection_SdcSchoolCollectionID(SdcSchoolStudentStatus.LOADED.getCode(), schoolCollectionID);
      var totalProcessed = totalCount - loadedCount;
      int percentageStudentsProcessed = (int) Math.floor((double) totalProcessed / totalCount * 100);
      long positionInQueue = 0;
      if (totalProcessed == 0) {
        positionInQueue = sdcSchoolCollectionRepository.findSdcSchoolCollectionsPositionInQueue(schoolCollectionRecord.getUploadDate());
      }

      UUID schoolID = schoolCollectionRecord.getSchoolID();
      Optional<School> school = restUtils.getSchoolBySchoolID(String.valueOf(schoolID));
      String schoolName = school.map(School::getMincode).orElse(null) + " - " + school.map(School::getDisplayName).orElse(null);

      SdcSchoolFileSummary collectionSummary = new SdcSchoolFileSummary(schoolCollectionID, schoolID, schoolName, schoolCollectionRecord.getUploadFileName(), schoolCollectionRecord.getUploadDate(), String.valueOf(percentageStudentsProcessed), String.valueOf(positionInQueue));
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
      monitorSdcSchoolCollection.setSchoolId(school.getSchoolId());
      monitorSdcSchoolCollection.setSdcSchoolCollectionId(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionId());
      monitorSdcSchoolCollection.setUploadDate(monitorSdcSchoolCollectionQueryResponse.getUploadDate());

      monitorSdcSchoolCollection.setErrors(monitorSdcSchoolCollectionQueryResponse.getErrors());
      monitorSdcSchoolCollection.setInfoWarnings(monitorSdcSchoolCollectionQueryResponse.getInfoWarnings());
      monitorSdcSchoolCollection.setFundingWarnings(monitorSdcSchoolCollectionQueryResponse.getFundingWarnings());

      monitorSdcSchoolCollection.setSchoolStatus(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionStatusCode());
      monitorSdcSchoolCollection.setSubmittedToDistrict(isStatusConfirmed(monitorSdcSchoolCollectionQueryResponse.getSdcSchoolCollectionStatusCode(), SdcSchoolCollectionStatus.SUBMITTED.getCode(), SdcSchoolCollectionStatus.COMPLETED.getCode()));

      monitorSdcSchoolCollections.add(monitorSdcSchoolCollection);
    });
    MonitorSdcSchoolCollectionsResponse response = new MonitorSdcSchoolCollectionsResponse();
    response.setMonitorSdcSchoolCollections(monitorSdcSchoolCollections);

    response.setTotalFundingWarnings(monitorSdcSchoolCollections.stream().mapToLong(MonitorSdcSchoolCollection::getFundingWarnings).sum());
    response.setTotalErrors(monitorSdcSchoolCollections.stream().mapToLong(MonitorSdcSchoolCollection::getErrors).sum());
    response.setTotalInfoWarnings(monitorSdcSchoolCollections.stream().mapToLong(MonitorSdcSchoolCollection::getInfoWarnings).sum());

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

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcDuplicateEntity updateStudentAndResolveDistrictDuplicates(UUID sdcDistrictCollectionID, UUID sdcDuplicateID, List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudent) {
    final Optional<SdcDuplicateEntity> curSdcDuplicateEntity = sdcDuplicateRepository.findBySdcDuplicateID(sdcDuplicateID);
    List<SdcSchoolCollectionStudentEntity> updatedStudents = new ArrayList<>();
    if (curSdcDuplicateEntity.isPresent()) {
      SdcDuplicateEntity curGetSdcDuplicateEntity = curSdcDuplicateEntity.get();
      // update student
      sdcSchoolCollectionStudent.forEach(student -> {
        RequestUtil.setAuditColumnsForUpdate(student);
        updatedStudents.add(sdcSchoolCollectionStudentService.updateSdcSchoolCollectionStudent(studentMapper.toSdcSchoolStudentEntity(student)));
      });

      if (updatedStudents.stream().noneMatch(student -> student.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString()))) {
        //re-run duplicates
        List<SdcDuplicateEntity> listOfDuplicates = sdcDuplicatesService.runDuplicatesCheck(DuplicateLevelCode.IN_DIST, updatedStudents.get(0), updatedStudents.get(1));
        if(listOfDuplicates.stream().map(SdcDuplicateEntity::getUniqueObjectHash).noneMatch(duplicateHash -> duplicateHash == curGetSdcDuplicateEntity.getUniqueObjectHash())) {
          //resolve
          curGetSdcDuplicateEntity.setDuplicateResolutionCode(DuplicateResolutionCode.RESOLVED.getCode());
          curGetSdcDuplicateEntity.setUpdateUser(updatedStudents.get(0).getUpdateUser());
          curGetSdcDuplicateEntity.setUpdateDate(LocalDateTime.now());
          TransformUtil.uppercaseFields(curGetSdcDuplicateEntity);
          return sdcDuplicateRepository.save(curGetSdcDuplicateEntity);
        }
      }
      return curGetSdcDuplicateEntity;
    } else {
      throw new EntityNotFoundException(SdcDuplicateEntity.class, SDC_DUPLICATE_ID_KEY, sdcDuplicateID.toString());
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcDuplicateEntity softDeleteEnrollmentDuplicate(UUID sdcDistrictCollectionID, UUID sdcDuplicateID, SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
    final Optional<SdcDuplicateEntity> curSdcDuplicateEntity = sdcDuplicateRepository.findBySdcDuplicateID(sdcDuplicateID);

    if (curSdcDuplicateEntity.isPresent()) {
      SdcDuplicateEntity curGetSdcDuplicateEntity = curSdcDuplicateEntity.get();
      final SdcDuplicateStudentEntity retainedStudent =
              curGetSdcDuplicateEntity.getSdcDuplicateStudentEntities().stream()
                      .filter(student -> !student.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().toString()
                              .equalsIgnoreCase(sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID())).findFirst().orElseThrow(() ->
                              new EntityNotFoundException(SdcDuplicateStudentEntity.class, "Duplicate Student entity", sdcDuplicateID.toString()));
      // update student
      sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudent(UUID.fromString(sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID()));
      // update duplicate entity
      curGetSdcDuplicateEntity.setRetainedSdcSchoolCollectionStudentEntity(retainedStudent.getSdcSchoolCollectionStudentEntity());
      curGetSdcDuplicateEntity.setDuplicateResolutionCode(DuplicateResolutionCode.RELEASED.getCode());
      curGetSdcDuplicateEntity.setUpdateUser(sdcSchoolCollectionStudent.getUpdateUser());
      curGetSdcDuplicateEntity.setUpdateDate(LocalDateTime.now());
      TransformUtil.uppercaseFields(curGetSdcDuplicateEntity);
      return sdcDuplicateRepository.save(curGetSdcDuplicateEntity);
    } else {
      throw new EntityNotFoundException(SdcDuplicateEntity.class, SDC_DUPLICATE_ID_KEY, sdcDuplicateID.toString());
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcDuplicateEntity changeGrade(UUID sdcDistrictCollectionID, UUID sdcDuplicateID, SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
    final Optional<SdcDuplicateEntity> curSdcDuplicateEntity = sdcDuplicateRepository.findBySdcDuplicateID(sdcDuplicateID);

    if (curSdcDuplicateEntity.isPresent()) {
      SdcDuplicateEntity curGetSdcDuplicateEntity = curSdcDuplicateEntity.get();

      // update student
      RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollectionStudent);
      SdcSchoolCollectionStudentEntity updatedStudent = sdcSchoolCollectionStudentService.updateSdcSchoolCollectionStudent(studentMapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent));

      if (!updatedStudent.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString())) {
          //resolve
          curGetSdcDuplicateEntity.setDuplicateResolutionCode(DuplicateResolutionCode.GRADE_CHNG.getCode());
          curGetSdcDuplicateEntity.setRetainedSdcSchoolCollectionStudentEntity(updatedStudent);
          curGetSdcDuplicateEntity.setUpdateUser(sdcSchoolCollectionStudent.getUpdateUser());
          curGetSdcDuplicateEntity.setUpdateDate(LocalDateTime.now());
          TransformUtil.uppercaseFields(curGetSdcDuplicateEntity);
          return sdcDuplicateRepository.save(curGetSdcDuplicateEntity);
      }
      return curGetSdcDuplicateEntity;
    } else {
      throw new EntityNotFoundException(SdcDuplicateEntity.class, SDC_DUPLICATE_ID_KEY, sdcDuplicateID.toString());
    }
  }
}