package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ReportZeroEnrollmentSdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileSummary;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.UnsubmitSdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

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

  private final SdcDistrictCollectionService sdcDistrictCollectionService;

  private final CollectionRepository collectionRepository;

  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final SdcSchoolCollectionStudentValidationIssueRepository sdcSchoolCollectionStudentValidationIssueRepository;

  private static final String INVALID_PAYLOAD_MSG = "Payload contains invalid data.";
  private static final String SDC_SCHOOL_COLLECTION_ID_KEY = "sdcSchoolCollectionID";
  private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

  @Autowired
  public SdcSchoolCollectionService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService, SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository, SdcDuplicateRepository sdcDuplicateRepository, SdcSchoolCollectionStudentHistoryService sdcSchoolCollectionStudentHistoryService, CollectionRepository collectionRepository, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcDistrictCollectionService sdcDistrictCollectionService, SdcSchoolCollectionStudentValidationIssueRepository sdcSchoolCollectionStudentValidationIssueRepository) {
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
    this.sdcSchoolCollectionHistoryService = sdcSchoolCollectionHistoryService;
    this.sdcSchoolCollectionStudentHistoryRepository = sdcSchoolCollectionStudentHistoryRepository;
    this.sdcDuplicateRepository = sdcDuplicateRepository;
    this.sdcSchoolCollectionStudentHistoryService = sdcSchoolCollectionStudentHistoryService;
    this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
    this.collectionRepository = collectionRepository;
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
    this.sdcDistrictCollectionService = sdcDistrictCollectionService;
    this.sdcSchoolCollectionStudentValidationIssueRepository = sdcSchoolCollectionStudentValidationIssueRepository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public SdcSchoolCollectionEntity reconcileStudentsAndSaveSdcSchoolCollection(SdcSchoolCollectionEntity curSDCSchoolEntity, List<SdcSchoolCollectionStudentEntity> finalStudents, List<UUID> removedStudents) {
    log.debug("Removing duplicate records for SDC school collection: {}", curSDCSchoolEntity.getSdcSchoolCollectionID());
    this.sdcDuplicateRepository.deleteAllBySdcDuplicateStudentEntities_SdcSchoolCollectionID(curSDCSchoolEntity.getSdcSchoolCollectionID());

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
      BeanUtils.copyProperties(sdcSchoolCollectionEntity, curGetSdcSchoolCollection, "uploadDate", "uploadFileName", "schoolID", "collectionEntity", SDC_SCHOOL_COLLECTION_ID_KEY);
      TransformUtil.uppercaseFields(curGetSdcSchoolCollection);
      return saveSdcSchoolCollectionWithHistory(curGetSdcSchoolCollection);
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
      var demogHash = Integer.toString(student.getUniqueObjectHash());
      student.setOriginalDemogHash(demogHash);
      student.setCurrentDemogHash(demogHash);
    }
    sdcSchoolCollectionEntity.getSdcSchoolCollectionHistoryEntities().add(sdcSchoolCollectionHistoryService.createSDCSchoolHistory(sdcSchoolCollectionEntity, sdcSchoolCollectionEntity.getUpdateUser()));
    return sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteSdcCollection(UUID sdcSchoolCollectionID) {
    Optional<SdcSchoolCollectionEntity> entityOptional = sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID);
    SdcSchoolCollectionEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionEntity.class, SDC_SCHOOL_COLLECTION_ID_KEY, sdcSchoolCollectionID.toString()));
    List<SdcSchoolCollectionStudentHistoryEntity> schoolHistoryEntities = sdcSchoolCollectionStudentHistoryRepository.findAllBySdcSchoolCollectionID(sdcSchoolCollectionID);
    if(!schoolHistoryEntities.isEmpty()) {
      sdcSchoolCollectionStudentHistoryRepository.deleteAll(schoolHistoryEntities);
    }
    this.sdcDuplicateRepository.deleteAllBySdcDuplicateStudentEntities_SdcSchoolCollectionID(sdcSchoolCollectionID);
    sdcSchoolCollectionRepository.delete(entity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionEntity unsubmitSchoolCollection(UnsubmitSdcSchoolCollection unsubmitData) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionOptional = sdcSchoolCollectionRepository.findById(unsubmitData.getSdcSchoolCollectionID());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionOptional.orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionEntity.class, SDC_SCHOOL_COLLECTION_ID_KEY, unsubmitData.getSdcSchoolCollectionID().toString()));

    if(!StringUtils.equals(sdcSchoolCollectionEntity.getSdcSchoolCollectionStatusCode(), SdcSchoolCollectionStatus.SUBMITTED.getCode())) {
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
      var validationError = ValidationUtil.createFieldError(SDC_SCHOOL_COLLECTION_ID_KEY, unsubmitData.getSdcSchoolCollectionID(), "Cannot un-submit a SDC School Collection that is not in submitted status.");
      List<FieldError> fieldErrorList = new ArrayList<>();
      fieldErrorList.add(validationError);
      error.addValidationErrors(fieldErrorList);
      throw new InvalidPayloadException(error);
    }
    if(sdcSchoolCollectionEntity.getSdcDistrictCollectionID() != null) {
      Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionOptional = sdcDistrictCollectionRepository.findBySdcDistrictCollectionID(sdcSchoolCollectionEntity.getSdcDistrictCollectionID());
      SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionOptional.orElseThrow(() -> new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID", sdcSchoolCollectionEntity.getSdcDistrictCollectionID() != null ? sdcSchoolCollectionEntity.getSdcDistrictCollectionID().toString() : "null"));

      if(!StringUtils.equals(sdcDistrictCollectionEntity.getSdcDistrictCollectionStatusCode(), SdcDistrictCollectionStatus.LOADED.getCode())) {
        sdcDistrictCollectionEntity.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.LOADED.getCode());
        sdcDistrictCollectionEntity.setUpdateDate(LocalDateTime.now());
        sdcDistrictCollectionEntity.setUpdateUser(unsubmitData.getUpdateUser());
        sdcDistrictCollectionService.updateSdcDistrictCollection(sdcDistrictCollectionEntity);
      }
      sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.DUP_VRFD.getCode());
    } else {
      //indy school
      sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SCH_C_VRFD.getCode());
    }
    sdcSchoolCollectionEntity.setUpdateDate(LocalDateTime.now());
    sdcSchoolCollectionEntity.setUpdateUser(unsubmitData.getUpdateUser());
    saveSdcSchoolCollectionWithHistory(sdcSchoolCollectionEntity);

    return sdcSchoolCollectionEntity;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionEntity reportZeroEnrollment(ReportZeroEnrollmentSdcSchoolCollection reportZeroEnrollmentData) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionOptional = sdcSchoolCollectionRepository.findById(reportZeroEnrollmentData.getSdcSchoolCollectionID());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionOptional.orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionEntity.class, SDC_SCHOOL_COLLECTION_ID_KEY, reportZeroEnrollmentData.getSdcSchoolCollectionID().toString()));

    this.sdcDuplicateRepository.deleteAllBySdcDuplicateStudentEntities_SdcSchoolCollectionID(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());

    this.sdcSchoolCollectionStudentHistoryRepository.deleteBySdcSchoolCollectionStudentIDs(
            sdcSchoolCollectionEntity.getSDCSchoolStudentEntities().stream().map(SdcSchoolCollectionStudentEntity::getSdcSchoolCollectionStudentID).toList()
    );

    this.sdcSchoolCollectionStudentRepository.deleteAll(sdcSchoolCollectionEntity.getSDCSchoolStudentEntities());
    sdcSchoolCollectionEntity.getSDCSchoolStudentEntities().clear();

    sdcSchoolCollectionEntity.setUploadFileName(null);
    sdcSchoolCollectionEntity.setUploadDate(null);
    sdcSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());
    sdcSchoolCollectionEntity.setUpdateDate(LocalDateTime.now());
    sdcSchoolCollectionEntity.setUpdateUser(reportZeroEnrollmentData.getUpdateUser());

    saveSdcSchoolCollectionWithHistory(sdcSchoolCollectionEntity);

    return sdcSchoolCollectionEntity;
  }

  public List<ValidationIssueTypeCode> getStudentValidationIssueCodes(UUID sdcSchoolCollectionID) {
    return sdcSchoolCollectionStudentValidationIssueRepository.findAllValidationIssueCodesBySdcSchoolCollectionID(sdcSchoolCollectionID).stream()
            .map(StudentValidationIssueTypeCode::findByValue)
            .filter(Objects::nonNull)
            .map(code -> new ValidationIssueTypeCode(code.getCode(), code.getMessage(), code.getSeverityCode().toString()))
            .sorted(Comparator.comparing(ValidationIssueTypeCode::getSeverityTypeCode)
                    .thenComparing(ValidationIssueTypeCode::getMessage))
            .toList();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void startSDCCollectionFromLastSDCCollectionDataSet(UUID sdcSchoolCollectionID, String updateUser) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionOptional = sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID);
    SdcSchoolCollectionEntity currentSchoolCollectionEntity = sdcSchoolCollectionOptional.orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionEntity.class, SDC_SCHOOL_COLLECTION_ID_KEY, sdcSchoolCollectionID.toString()));
    sdcSchoolCollectionStudentHistoryRepository.deleteAllBySdcSchoolCollectionID(sdcSchoolCollectionID);
    var septemberCollectionOptional = sdcSchoolCollectionRepository.findLastCollectionBySchoolIDAndType(currentSchoolCollectionEntity.getSchoolID(), CollectionTypeCodes.SEPTEMBER.getTypeCode(), currentSchoolCollectionEntity.getSdcSchoolCollectionID());
    SdcSchoolCollectionEntity septemberCollection = septemberCollectionOptional.orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionEntity.class, SDC_SCHOOL_COLLECTION_ID_KEY, sdcSchoolCollectionID.toString()));

    currentSchoolCollectionEntity.getSDCSchoolStudentEntities().clear();
    septemberCollection.getSDCSchoolStudentEntities().stream().forEach(sdcSchoolCollectionStudentEntity -> {
      if(!sdcSchoolCollectionStudentEntity.getSdcSchoolCollectionStudentStatusCode().equals(SdcSchoolStudentStatus.DELETED.getCode())) {
        var studentEntity = new SdcSchoolCollectionStudentEntity();
        BeanUtils.copyProperties(sdcSchoolCollectionStudentEntity, studentEntity, "sdcSchoolCollectionStudentID", "sdcStudentEnrolledProgramEntities", "sdcStudentValidationIssueEntities");
        TransformUtil.clearCalculatedFields(studentEntity, true);
        studentEntity.setSdcSchoolCollection(currentSchoolCollectionEntity);
        var hash = Integer.toString(studentEntity.getUniqueObjectHash());
        studentEntity.setOriginalDemogHash(hash);
        studentEntity.setCurrentDemogHash(hash);
        studentEntity.setCreateUser(updateUser);
        studentEntity.setUpdateUser(updateUser);
        studentEntity.setCreateDate(LocalDateTime.now());
        studentEntity.setUpdateDate(LocalDateTime.now());
        studentEntity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.LOADED.getCode());
        currentSchoolCollectionEntity.getSDCSchoolStudentEntities().add(studentEntity);
      }
    });

    currentSchoolCollectionEntity.setUploadFileName("UPLOAD_FROM_PRIOR_SEPT_COLLECTION");
    currentSchoolCollectionEntity.setUploadDate(LocalDateTime.now());
    currentSchoolCollectionEntity.setUploadReportDate(formatter.format(LocalDate.now()));
    currentSchoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());
    currentSchoolCollectionEntity.setUpdateDate(LocalDateTime.now());
    currentSchoolCollectionEntity.setUpdateUser(updateUser);

    saveSdcSchoolCollectionWithHistory(currentSchoolCollectionEntity);
    sdcSchoolCollectionStudentService.saveAllSdcStudentWithHistory(currentSchoolCollectionEntity.getSDCSchoolStudentEntities().stream().toList());
  }

  public SdcSchoolCollectionEntity saveSdcSchoolCollectionWithHistory(SdcSchoolCollectionEntity sdcSchoolCollectionEntity) {
    TransformUtil.uppercaseFields(sdcSchoolCollectionEntity);
    sdcSchoolCollectionEntity.getSdcSchoolCollectionHistoryEntities().add(sdcSchoolCollectionHistoryService.createSDCSchoolHistory(sdcSchoolCollectionEntity, sdcSchoolCollectionEntity.getUpdateUser()));
    return this.sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);
  }
}
