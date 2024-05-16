package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileSummary;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SdcSchoolCollectionService {

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  private final SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService;

  private final SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;

  private final SdcDuplicateRepository sdcDuplicateRepository;

  private final SdcSchoolCollectionStudentHistoryService sdcSchoolCollectionStudentHistoryService;

  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

  private final CollectionRepository collectionRepository;

  @Autowired
  public SdcSchoolCollectionService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService, SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository, SdcDuplicateRepository sdcDuplicateRepository, SdcSchoolCollectionStudentHistoryService sdcSchoolCollectionStudentHistoryService, CollectionRepository collectionRepository, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService) {
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
    this.sdcSchoolCollectionHistoryService = sdcSchoolCollectionHistoryService;
    this.sdcSchoolCollectionStudentHistoryRepository = sdcSchoolCollectionStudentHistoryRepository;
    this.sdcDuplicateRepository = sdcDuplicateRepository;
    this.sdcSchoolCollectionStudentHistoryService = sdcSchoolCollectionStudentHistoryService;
    this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
    this.collectionRepository = collectionRepository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public SdcSchoolCollectionEntity saveSdcSchoolCollection(SdcSchoolCollectionEntity curSDCSchoolEntity, List<SdcSchoolCollectionStudentEntity> finalStudents, List<UUID> removedStudents) {
    log.debug("Removing duplicate records by sdcSchoolCollectionStudentIDs: {}", removedStudents);
    var duplicatesToDelete = this.sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionStudentEntity_SdcSchoolCollectionStudentIDIn(removedStudents);
    this.sdcDuplicateRepository.deleteAll(duplicatesToDelete);

    List<SdcSchoolCollectionStudentEntity> newStudents = finalStudents.stream().filter(sdcSchoolCollectionStudentEntity -> sdcSchoolCollectionStudentEntity.getSdcSchoolCollectionStudentID() == null).toList();
    curSDCSchoolEntity.getSDCSchoolStudentEntities().clear();
    curSDCSchoolEntity.getSDCSchoolStudentEntities().addAll(finalStudents);
    curSDCSchoolEntity.getSdcSchoolCollectionHistoryEntities().add(sdcSchoolCollectionHistoryService.createSDCSchoolHistory(curSDCSchoolEntity, curSDCSchoolEntity.getUpdateUser()));

    log.debug("Removing student history records by sdcSchoolCollectionStudentIDs: {}", removedStudents);
    this.sdcSchoolCollectionStudentHistoryRepository.deleteBySdcSchoolCollectionStudentIDs(removedStudents);
    log.debug("About to save school file data for collection: {}", curSDCSchoolEntity.getSdcSchoolCollectionID());
    var returnedEntities = this.sdcSchoolCollectionRepository.save(curSDCSchoolEntity);

    log.debug("About to persist history records for students: {}", curSDCSchoolEntity.getSdcSchoolCollectionID());
    List<SdcSchoolCollectionStudentHistoryEntity> newHistoryEntities = new ArrayList<>();
    newStudents.stream().forEach(sdcSchoolCollectionStudentEntity -> newHistoryEntities.add(this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(sdcSchoolCollectionStudentEntity, curSDCSchoolEntity.getUpdateUser())));
    this.sdcSchoolCollectionStudentHistoryRepository.saveAll(newHistoryEntities);

    return returnedEntities;
  }

  public SdcSchoolCollectionEntity getActiveSdcSchoolCollectionBySchoolID(UUID schoolID) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntity =  sdcSchoolCollectionRepository.findActiveCollectionBySchoolId(schoolID);
    if(sdcSchoolCollectionEntity.isPresent()) {
      return sdcSchoolCollectionEntity.get();
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection for school Id", schoolID.toString());
    }
  }

  public List<SdcSchoolCollectionEntity> getAllSchoolCollections(UUID schoolID, UUID sdcDistrictCollectionID) {
    if (schoolID != null){
      return sdcSchoolCollectionRepository.findAllBySchoolID(schoolID);
    } else if (sdcDistrictCollectionID != null){
      return sdcSchoolCollectionRepository.findAllBySdcDistrictCollectionID(sdcDistrictCollectionID);
    } else {
      throw new IllegalArgumentException("Invalid query param");
    }
  }

  public List<SdcSchoolCollectionStudentEntity> getAllSchoolCollectionDuplicates(UUID sdcSchoolCollectionID) {
    return sdcSchoolCollectionStudentRepository.findAllDuplicateStudentsInSdcSchoolCollection(sdcSchoolCollectionID);
  }

  public SdcSchoolCollectionEntity getSdcSchoolCollection(UUID sdcSchoolCollectionID) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntity =  sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID);
    if(sdcSchoolCollectionEntity.isPresent()) {
      return sdcSchoolCollectionEntity.get();
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "SdcSchoolCollection for sdcSchoolCollectionID", sdcSchoolCollectionID.toString());
    }
  }

  public SdcSchoolCollectionEntity updateSdcSchoolCollection(final SdcSchoolCollectionEntity sdcSchoolCollectionEntity) {
    final Optional<SdcSchoolCollectionEntity> curSdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
    if (curSdcSchoolCollection.isPresent()) {
      SdcSchoolCollectionEntity curGetSdcSchoolCollection = curSdcSchoolCollection.get();
      BeanUtils.copyProperties(sdcSchoolCollectionEntity, curGetSdcSchoolCollection, "uploadDate", "uploadFileName", "schoolID", "collectionID", "sdcSchoolCollectionID", "collectionTypeCode", "collectionOpenDate", "collectionCloseDate", "students");
      TransformUtil.uppercaseFields(curGetSdcSchoolCollection);
      curGetSdcSchoolCollection = this.sdcSchoolCollectionRepository.save(curGetSdcSchoolCollection);
      return curGetSdcSchoolCollection;
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "SdcSchoolCollection", sdcSchoolCollectionEntity.getSdcSchoolCollectionID().toString());
    }
  }

  public SdcFileSummary getSummarySdcSchoolCollectionBeingProcessed(UUID sdcSchoolCollectionID) {
    var sdcSchoolCollectionEntity =  getSdcSchoolCollection(sdcSchoolCollectionID);
    SdcFileSummary summary = new SdcFileSummary();
    if(StringUtils.isNotBlank(sdcSchoolCollectionEntity.getUploadFileName())) {
      summary.setUploadDate(String.valueOf(sdcSchoolCollectionEntity.getUploadDate()));
      summary.setFileName(sdcSchoolCollectionEntity.getUploadFileName());
      summary.setUploadReportDate(sdcSchoolCollectionEntity.getUploadReportDate());
      var totalCount = sdcSchoolCollectionStudentRepository.countBySdcSchoolCollection_SdcSchoolCollectionID(sdcSchoolCollectionID);
      var loadedCount = sdcSchoolCollectionStudentRepository.countBySdcSchoolCollectionStudentStatusCodeAndSdcSchoolCollection_SdcSchoolCollectionID(SdcSchoolStudentStatus.LOADED.getCode(), sdcSchoolCollectionID);
      var totalProcessed = totalCount - loadedCount;
      long positionInQueue = 0;
      if(totalProcessed == 0) {
        positionInQueue = sdcSchoolCollectionRepository.findSdcSchoolCollectionsPositionInQueue(sdcSchoolCollectionEntity.getUploadDate());
      }
      summary.setTotalProcessed(Long.toString(totalProcessed));
      summary.setTotalStudents(Long.toString(totalCount));
      summary.setPositionInQueue(String.valueOf(positionInQueue));
    }
    return summary;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionEntity createSdcSchoolCollectionByCollectionID(SdcSchoolCollectionEntity sdcSchoolCollectionEntity ,UUID collectionID) {
    Optional<CollectionEntity> collectionEntityOptional = collectionRepository.findById(collectionID);
    CollectionEntity collectionEntity = collectionEntityOptional.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "collectionID", collectionID.toString()));

    TransformUtil.uppercaseFields(sdcSchoolCollectionEntity);
    sdcSchoolCollectionEntity.setCollectionEntity(collectionEntity);
    // Write Enrolled Program Codes
    for (SdcSchoolCollectionStudentEntity student : sdcSchoolCollectionEntity.getSDCSchoolStudentEntities()) {
      if (StringUtils.isNotBlank(student.getEnrolledProgramCodes())) {
        List<String> enrolledProgramList = TransformUtil.splitIntoChunks(student.getEnrolledProgramCodes(), 2);
        this.sdcSchoolCollectionStudentService.writeEnrolledProgramCodes(student, enrolledProgramList);
      }
    }
    sdcSchoolCollectionEntity.getSdcSchoolCollectionHistoryEntities().add(sdcSchoolCollectionHistoryService.createSDCSchoolHistory(sdcSchoolCollectionEntity, sdcSchoolCollectionEntity.getUpdateUser()));
    return sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteSdcCollection(UUID sdcSchoolCollectionID) {
    Optional<SdcSchoolCollectionEntity> entityOptional = sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID);
    SdcSchoolCollectionEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionEntity.class, "sdcSchoolCollectionID", sdcSchoolCollectionID.toString()));
    List<SdcSchoolCollectionStudentHistoryEntity> schoolHistoryEntities = sdcSchoolCollectionStudentHistoryRepository.findAllBySdcSchoolCollectionID(sdcSchoolCollectionID);
    if(!schoolHistoryEntities.isEmpty()) {
      sdcSchoolCollectionStudentHistoryRepository.deleteAll(schoolHistoryEntities);
    }
    sdcSchoolCollectionRepository.delete(entity);
  }

}
