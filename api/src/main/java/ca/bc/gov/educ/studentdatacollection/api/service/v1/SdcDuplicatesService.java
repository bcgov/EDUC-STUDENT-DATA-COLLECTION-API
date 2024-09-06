package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateResolutionCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramDuplicateTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SdcDuplicatesService {

  private final SdcDuplicateRepository sdcDuplicateRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final CollectionRepository collectionRepository;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository;
  private final ValidationRulesService validationRulesService;
  private final DuplicateClassNumberGenerationService duplicateClassNumberGenerationService;
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;
  private final ScheduleHandlerService scheduleHandlerService;
  private final SdcSchoolCollectionService sdcSchoolCollectionService;
  private static final SdcSchoolCollectionMapper sdcSchoolCollectionMapper = SdcSchoolCollectionMapper.mapper;
  private static final SdcSchoolCollectionStudentMapper sdcSchoolCollectionStudentMapper = SdcSchoolCollectionStudentMapper.mapper;
  private static final SdcDuplicateMapper sdcDuplicateMapper = SdcDuplicateMapper.mapper;
  private final RestUtils restUtils;
  private static final String SDC_DUPLICATE_ID_KEY = "sdcDuplicateID";
  private static final String COLLECTION_ID_NOT_ACTIVE_MSG = "Provided collectionID does not match currently active collectionID.";
  private static final String COLLECTION_DUPLICATES_ALREADY_RUN_MSG = "Provided collectionID has already run provincial duplicates.";
  private static final String SDC_SCHOOL_COLLECTION_STUDENT_ID = "sdcSchoolCollectionStudentId";
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
  private static final String SDC_SCHOOL_COLLECTION_STUDENT_STRING = "SdcSchoolCollectionStudentEntity";
  private static final List<String> independentSchoolCategoryCodes = Arrays.asList(SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.INDP_FNS.getCode());

  @Autowired
  public SdcDuplicatesService(SdcDuplicateRepository sdcDuplicateRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, ValidationRulesService validationRulesService, ScheduleHandlerService scheduleHandlerService, DuplicateClassNumberGenerationService duplicateClassNumberGenerationService, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService, CollectionRepository collectionRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository, SdcSchoolCollectionService sdcSchoolCollectionService, RestUtils restUtils) {
      this.sdcDuplicateRepository = sdcDuplicateRepository;
      this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
      this.collectionRepository = collectionRepository;
      this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
      this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
      this.sdcStudentValidationErrorRepository = sdcStudentValidationErrorRepository;
      this.sdcSchoolCollectionService = sdcSchoolCollectionService;
      this.validationRulesService = validationRulesService;
      this.scheduleHandlerService = scheduleHandlerService;
      this.duplicateClassNumberGenerationService = duplicateClassNumberGenerationService;
      this.restUtils = restUtils;
      this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<SdcDuplicateEntity> getAllInDistrictCollectionDuplicates(UUID sdcDistrictCollectionID) {
    var existingDuplicates = sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcDistrictCollectionID(sdcDistrictCollectionID);
    var duplicateStudentEntities = sdcSchoolCollectionStudentRepository.findAllInDistrictDuplicateStudentsInSdcDistrictCollection(sdcDistrictCollectionID);

    List<SdcDuplicateEntity> finalDuplicatesSet = generateFinalDuplicatesSet(duplicateStudentEntities, DuplicateLevelCode.IN_DIST, false);

    HashMap<Integer, SdcDuplicateEntity> finalDuplicatesHashMap = new HashMap<>();
    finalDuplicatesSet.forEach(entry -> finalDuplicatesHashMap.put(entry.getUniqueObjectHash(), entry));
    List<SdcDuplicateEntity> dupsToDelete = new ArrayList<>();
    List<SdcDuplicateEntity> finalReturnList = new ArrayList<>();

    existingDuplicates.forEach(dup -> {
      var dupHash = dup.getUniqueObjectHash();
      if(finalDuplicatesHashMap.containsKey(dupHash)){
        finalDuplicatesHashMap.remove(dupHash);
        finalReturnList.add(dup);
      }else{
        if(dup.getDuplicateResolutionCode() == null) {
          dupsToDelete.add(dup);
        }else{
          finalReturnList.add(dup);
        }
      }
    });

    log.info("Found {} duplicates to delete", dupsToDelete.size());
    sdcDuplicateRepository.deleteAll(dupsToDelete);
    log.info("Found {} new duplicates to save", finalDuplicatesHashMap.values().size());
    var savedDupes = sdcDuplicateRepository.saveAll(finalDuplicatesHashMap.values());

    finalReturnList.addAll(savedDupes);

    return finalReturnList;
  }

  public List<SdcDuplicateEntity> getAllProvincialDuplicatesByCollectionID(UUID collectionID) {
    return sdcDuplicateRepository.findAllDuplicatesByCollectionIDAndDuplicateLevelCode(collectionID, "PROVINCIAL");
  }

  public List<SdcDuplicateEntity> getAllProvincialDuplicatesBySdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
    return sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcDistrictCollectionIDAndDuplicateLevelCode(sdcDistrictCollectionID, DuplicateLevelCode.PROVINCIAL.getCode());
  }

  public List<SdcDuplicateEntity> getAllProvincialDuplicatesBySdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
    return sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionIDAndDuplicateLevelCode(sdcSchoolCollectionID, DuplicateLevelCode.PROVINCIAL.getCode());
  }

  public List<SdcDuplicateEntity> getAllDuplicatesBySdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
    return sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionID(sdcSchoolCollectionID);
  }

  public SdcDuplicateEntity getSdcDuplicate(UUID sdcDuplicateID) {
    Optional<SdcDuplicateEntity> sdcDuplicateEntity = sdcDuplicateRepository.findById(sdcDuplicateID);

    return sdcDuplicateEntity.orElseThrow(() -> new EntityNotFoundException(SdcDuplicateEntity.class, SDC_DUPLICATE_ID_KEY, sdcDuplicateID.toString()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity createSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollection = this.sdcSchoolCollectionRepository.findById(studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID());
    if(sdcSchoolCollection.isPresent()) {
      studentEntity.setEnrolledProgramCodes(TransformUtil.sanitizeEnrolledProgramString(studentEntity.getEnrolledProgramCodes()));
      studentEntity.setSdcSchoolCollection(sdcSchoolCollection.get());
      studentEntity.setOriginalDemogHash(Integer.toString(studentEntity.getUniqueObjectHash()));
      return validateAndProcessSdcSchoolCollectionStudent(studentEntity, null, false);
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "SdcSchoolCollectionEntity", studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID().toString());
    }
  }

  @Transactional
  public SdcSchoolCollectionStudentEntity routeSdcSchoolCollectionStudentUpdate(SdcSchoolCollectionStudentEntity studentEntity, boolean allowDuplicateCreation){
    SdcSchoolCollectionStudentEntity updatedStudent =  updateSdcSchoolCollectionStudent(studentEntity, allowDuplicateCreation);

    // check to see if existing dupes can be resolved by the update - not creating any new dupes
    List<SdcDuplicateEntity> existingProgramDupes = sdcDuplicateRepository.findAllUnresolvedProgramDuplicatesForStudent(studentEntity.getSdcSchoolCollectionStudentID());
    if(!existingProgramDupes.isEmpty()){
      Optional<CollectionEntity> currCollection = collectionRepository.findActiveCollection();
      String collectionStatusCode = currCollection.map(CollectionEntity::getCollectionStatusCode).orElse(null);
      DuplicateLevelCode dupeLevel = Objects.equals(collectionStatusCode, CollectionStatus.INPROGRESS.getCode()) ? DuplicateLevelCode.IN_DIST : DuplicateLevelCode.PROVINCIAL;
      existingProgramDupes.forEach(dupe -> {
        Optional<SdcDuplicateStudentEntity> otherStudentDupeEntity = dupe.getSdcDuplicateStudentEntities().stream().filter(std -> std.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID() != studentEntity.getSdcSchoolCollectionStudentID()).findFirst();
        SdcSchoolCollectionStudentEntity otherStudentEntity= otherStudentDupeEntity.isPresent() ? otherStudentDupeEntity.get().getSdcSchoolCollectionStudentEntity() : null;
        List<SdcDuplicateEntity> newDupes = runDuplicatesCheck(dupeLevel, sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntity(updatedStudent), sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntity(otherStudentEntity), true);

        // if dupe is no longer present, resolve it
        if (newDupes.stream().map(SdcDuplicateEntity::getUniqueObjectHash).noneMatch(duplicateHash -> duplicateHash == dupe.getUniqueObjectHash())) {
          if(Objects.equals(dupe.getDuplicateTypeCode(), DuplicateTypeCode.ENROLLMENT.getCode())){
            dupe.setDuplicateResolutionCode(DuplicateResolutionCode.GRADE_CHNG.getCode());
          } else {
            dupe.setDuplicateResolutionCode(DuplicateResolutionCode.RESOLVED.getCode());
          }

          dupe.setUpdateUser(studentEntity.getUpdateUser());
          dupe.setUpdateDate(LocalDateTime.now());
          TransformUtil.uppercaseFields(dupe);
          sdcDuplicateRepository.save(dupe);
        }
      });
    }

    return updatedStudent;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcSchoolCollectionStudentEntity updateSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity studentEntity, boolean allowDuplicateCreation) {
    var currentStudentEntity = this.sdcSchoolCollectionStudentRepository.findById(studentEntity.getSdcSchoolCollectionStudentID());
    if(currentStudentEntity.isPresent()) {
      SdcSchoolCollectionStudentEntity getCurStudentEntity = currentStudentEntity.get();
      final SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = new SdcSchoolCollectionStudentEntity();
      BeanUtils.copyProperties(studentEntity, sdcSchoolCollectionStudentEntity, "sdcSchoolCollection", "createUser", "createDate", "sdcStudentValidationIssueEntities", "sdcStudentEnrolledProgramEntities");

      sdcSchoolCollectionStudentEntity.setEnrolledProgramCodes(TransformUtil.sanitizeEnrolledProgramString(sdcSchoolCollectionStudentEntity.getEnrolledProgramCodes()));
      sdcSchoolCollectionStudentEntity.setSdcSchoolCollection(getCurStudentEntity.getSdcSchoolCollection());
      sdcSchoolCollectionStudentEntity.setOriginalDemogHash(getCurStudentEntity.getOriginalDemogHash());
      sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().clear();
      sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().addAll(getCurStudentEntity.getSDCStudentValidationIssueEntities());
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().clear();
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().addAll(getCurStudentEntity.getSdcStudentEnrolledProgramEntities());

      return validateAndProcessSdcSchoolCollectionStudent(sdcSchoolCollectionStudentEntity, getCurStudentEntity, allowDuplicateCreation);
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, SDC_SCHOOL_COLLECTION_STUDENT_STRING, studentEntity.getSdcSchoolCollectionStudentID().toString());
    }
  }

  public SdcSchoolCollectionStudentEntity validateAndProcessSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity, SdcSchoolCollectionStudentEntity currentStudentEntity, boolean allowDuplicateCreation) {
    UUID originalAssignedPen = sdcSchoolCollectionStudentEntity.getAssignedStudentId();

    TransformUtil.uppercaseFields(sdcSchoolCollectionStudentEntity);
    var studentRuleData = sdcSchoolCollectionStudentService.createStudentRuleDataForValidation(sdcSchoolCollectionStudentEntity);

    var processedSdcSchoolCollectionStudent = sdcSchoolCollectionStudentService.processStudentRecord(studentRuleData.getSchool(), sdcSchoolCollectionStudentEntity, true);
    if (processedSdcSchoolCollectionStudent.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString())) {
      log.debug("SdcSchoolCollectionStudent was not saved to the database because it has errors :: {}", processedSdcSchoolCollectionStudent);
      if(currentStudentEntity != null) {
        processedSdcSchoolCollectionStudent.setUpdateDate(currentStudentEntity.getUpdateDate());
        processedSdcSchoolCollectionStudent.setUpdateUser(currentStudentEntity.getUpdateUser());
      }
      return processedSdcSchoolCollectionStudent;
    }
    if (!allowDuplicateCreation || (currentStudentEntity != null && sdcSchoolCollectionStudentEntity.getSdcSchoolCollection().getCollectionEntity().getCollectionStatusCode().equals( CollectionStatus.PROVDUPES.getCode()))) {
      checkIfUpdateWouldGenerateNewDupes(originalAssignedPen, currentStudentEntity.getSdcSchoolCollection().getCollectionEntity().getCollectionID());
    }

    processedSdcSchoolCollectionStudent.setCurrentDemogHash(Integer.toString(processedSdcSchoolCollectionStudent.getUniqueObjectHash()));
    return sdcSchoolCollectionStudentService.saveSdcStudentWithHistory(processedSdcSchoolCollectionStudent);
  }

  public void checkIfUpdateWouldGenerateNewDupes(UUID originalAssignedPen, UUID collectionID) {
    List<UUID> studentAssignedIdList = Collections.singletonList(originalAssignedPen);
    List<SdcSchoolCollectionStudentEntity> duplicateStudentsList = sdcSchoolCollectionStudentRepository.findAllDuplicateStudentsByCollectionID(collectionID, studentAssignedIdList);

    if (!duplicateStudentsList.isEmpty()) {
      log.debug("SdcSchoolCollectionStudent was not saved to the database because it would create a duplicate on save :: {}", studentAssignedIdList.stream().findFirst());

      ApiError error = ApiError.builder()
              .timestamp(LocalDateTime.now())
              .message("SdcSchoolCollectionStudent was not saved to the database because it would create a duplicate.")
              .status(HttpStatus.BAD_REQUEST)
              .build();

      var validationError = ValidationUtil.createFieldError(
              "sdcSchoolCollectionStudent",
              originalAssignedPen.toString(),
              "Duplicate ID found in the collection"
      );

      List<FieldError> fieldErrorList = new ArrayList<>();
      fieldErrorList.add(validationError);

      error.addValidationErrors(fieldErrorList);
      throw new InvalidPayloadException(error);    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcDuplicateEntity updateStudentAndResolveDuplicates(UUID sdcDuplicateID, List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudent) {
    final Optional<SdcDuplicateEntity> curSdcDuplicateEntity = sdcDuplicateRepository.findBySdcDuplicateID(sdcDuplicateID);
    if (curSdcDuplicateEntity.isPresent()) {
      SdcDuplicateEntity curGetSdcDuplicateEntity = curSdcDuplicateEntity.get();
      // update student
      sdcSchoolCollectionStudent.forEach(student -> {
        RequestUtil.setAuditColumnsForUpdate(student);
        SdcSchoolCollectionStudentEntity updatedStudentEntity = updateSdcSchoolCollectionStudent(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentEntity(student), true);
        // There might only be one student to update, but we need both for dup check, so get both added
        curGetSdcDuplicateEntity.getSdcDuplicateStudentEntities().stream()
                .filter(duplicateStudent -> duplicateStudent.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().equals(updatedStudentEntity.getSdcSchoolCollectionStudentID()))
                .forEach(duplicateStudent -> duplicateStudent.setSdcSchoolCollectionStudentEntity(updatedStudentEntity));
      });
      var updatedStudents = curGetSdcDuplicateEntity.getSdcDuplicateStudentEntities().stream().map(SdcDuplicateStudentEntity::getSdcSchoolCollectionStudentEntity).toList();
      if (curGetSdcDuplicateEntity.getSdcDuplicateStudentEntities().stream().map(SdcDuplicateStudentEntity::getSdcSchoolCollectionStudentEntity).noneMatch(student -> student.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString()))) {
        //re-run duplicates
        List<SdcDuplicateEntity> listOfDuplicates = runDuplicatesCheck(DuplicateLevelCode.valueOf(curGetSdcDuplicateEntity.getDuplicateLevelCode()), sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntity(updatedStudents.get(0)), sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntity(updatedStudents.get(1)), true);
        if (listOfDuplicates.stream().map(SdcDuplicateEntity::getUniqueObjectHash).noneMatch(duplicateHash -> duplicateHash == curGetSdcDuplicateEntity.getUniqueObjectHash())) {
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
  public SdcDuplicateEntity softDeleteEnrollmentDuplicate(UUID sdcDuplicateID, SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
    final Optional<SdcDuplicateEntity> curSdcDuplicateEntity = sdcDuplicateRepository.findBySdcDuplicateID(sdcDuplicateID);

    if (curSdcDuplicateEntity.isPresent()) {
      SdcDuplicateEntity curGetSdcDuplicateEntity = curSdcDuplicateEntity.get();
      curGetSdcDuplicateEntity = releaseEnrollmentDupe(curGetSdcDuplicateEntity, sdcSchoolCollectionStudent);

      // update student
      softDeleteSdcSchoolCollectionStudent(UUID.fromString(sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID()));

      trickleEnrollmentDupeUpdates(curGetSdcDuplicateEntity, sdcSchoolCollectionStudent);

      return sdcDuplicateRepository.save(curGetSdcDuplicateEntity);
    } else {
      throw new EntityNotFoundException(SdcDuplicateEntity.class, SDC_DUPLICATE_ID_KEY, sdcDuplicateID.toString());
    }
  }

  public SdcDuplicateEntity releaseEnrollmentDupe(SdcDuplicateEntity enrollmentDupe, SdcSchoolCollectionStudent sdcSchoolCollectionStudent){
    final SdcDuplicateStudentEntity retainedStudent =
            enrollmentDupe.getSdcDuplicateStudentEntities().stream()
                    .filter(student -> !student.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().toString()
                            .equalsIgnoreCase(sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID())).findFirst().orElseThrow(() ->
                            new EntityNotFoundException(SdcDuplicateStudentEntity.class, "Duplicate Student entity", enrollmentDupe.getSdcDuplicateID().toString()));

    // update duplicate entity
    enrollmentDupe.setRetainedSdcSchoolCollectionStudentEntity(retainedStudent.getSdcSchoolCollectionStudentEntity());
    enrollmentDupe.setDuplicateResolutionCode(DuplicateResolutionCode.RELEASED.getCode());
    enrollmentDupe.setUpdateUser(sdcSchoolCollectionStudent.getUpdateUser());
    enrollmentDupe.setUpdateDate(LocalDateTime.now());
    TransformUtil.uppercaseFields(enrollmentDupe);

    return enrollmentDupe;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcSchoolCollectionStudentEntity softDeleteSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    SdcSchoolCollectionStudentEntity student = sdcSchoolCollectionStudentEntityOptional.orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudent.class, SDC_SCHOOL_COLLECTION_STUDENT_ID, sdcSchoolCollectionStudentID.toString()));

    //delete student validation errors
    this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(student.getSdcSchoolCollectionStudentID());
    student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.toString());

    deleteResolvedDupes(sdcSchoolCollectionStudentID);
    updateResolvedGradeChangeDupes(student.getSdcSchoolCollectionStudentID());
    checkForDupesAndStartTrickle(sdcSchoolCollectionStudentID, student);

    return sdcSchoolCollectionStudentService.saveSdcStudentWithHistory(student);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public List<SdcSchoolCollectionStudentEntity> softDeleteSdcSchoolCollectionStudents(List<UUID> sdcSchoolCollectionStudentIDs) {
    List<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntities = sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollectionStudentIDIn(sdcSchoolCollectionStudentIDs);

    sdcSchoolCollectionStudentEntities.forEach(student -> {
      this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(student.getSdcSchoolCollectionStudentID());
      student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DELETED.toString());
      deleteResolvedDupes(student.getSdcSchoolCollectionStudentID());
      updateResolvedGradeChangeDupes(student.getSdcSchoolCollectionStudentID());
      checkForDupesAndStartTrickle(student.getSdcSchoolCollectionStudentID(), student);
    });

    return sdcSchoolCollectionStudentService.saveAllSdcStudentWithHistory(sdcSchoolCollectionStudentEntities);
  }

  public void checkForDupesAndStartTrickle(UUID sdcSchoolCollectionStudentID, SdcSchoolCollectionStudentEntity student){
    List<SdcDuplicateEntity> existingDupes = sdcDuplicateRepository.findAllUnresolvedEnrollmentDuplicatesForStudent(sdcSchoolCollectionStudentID);

    if(!existingDupes.isEmpty()){
      SdcDuplicateEntity updatedDupe = releaseEnrollmentDupe(existingDupes.get(0), SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(student));
      sdcDuplicateRepository.save(updatedDupe);
      trickleEnrollmentDupeUpdates(updatedDupe, SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(student));
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void deleteResolvedDupes(UUID sdcSchoolCollectionStudentID){
    //delete resolved enrollment dupes since both associated students have now been soft deleted
    List<SdcDuplicateEntity> resolvedEnrollmentDupes = sdcDuplicateRepository.findAllResolvedEnrollmentDuplicatesForStudent(sdcSchoolCollectionStudentID);
    sdcDuplicateRepository.deleteAll(resolvedEnrollmentDupes);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void updateResolvedGradeChangeDupes(UUID sdcSchoolCollectionStudentID){
    //update resolved grade change dupes to reflect current state
    List<SdcDuplicateEntity> resolvedGradeChangeDupes = sdcDuplicateRepository.findAllResolvedGradeChangeDuplicatesForStudent(sdcSchoolCollectionStudentID);
    resolvedGradeChangeDupes.forEach(dupe -> {
      Optional<SdcDuplicateStudentEntity> otherStudent = dupe.getSdcDuplicateStudentEntities().stream().filter(std -> std.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID() != sdcSchoolCollectionStudentID).findFirst();

      if(otherStudent.isPresent()){
        if(!Objects.equals(otherStudent.get().getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentStatusCode(), SdcSchoolStudentStatus.DELETED.toString())) {
          dupe.setDuplicateResolutionCode(DuplicateResolutionCode.RELEASED.getCode());
          dupe.setRetainedSdcSchoolCollectionStudentEntity(otherStudent.get().getSdcSchoolCollectionStudentEntity());
        } else {
          sdcDuplicateRepository.delete(dupe);
        }
      }
    });
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcDuplicateEntity changeGrade(UUID sdcDuplicateID, SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
    final Optional<SdcDuplicateEntity> curSdcDuplicateEntity = sdcDuplicateRepository.findBySdcDuplicateID(sdcDuplicateID);

    if (curSdcDuplicateEntity.isPresent()) {
      SdcDuplicateEntity curGetSdcDuplicateEntity = curSdcDuplicateEntity.get();

      // update student
      RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollectionStudent);
      SdcSchoolCollectionStudentEntity updatedStudent = updateSdcSchoolCollectionStudent(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent), true);

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

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcDuplicateEntity trickleGradeChangeDupeUpdates(UUID sdcDuplicateID, SdcSchoolCollectionStudent sdcSchoolCollectionStudent){
      SdcDuplicateEntity gradeChangedDupe = changeGrade(sdcDuplicateID, sdcSchoolCollectionStudent);

      if(gradeChangedDupe != null){
        trickleEnrollmentDupeUpdates(gradeChangedDupe, sdcSchoolCollectionStudent);
      } else {
        throw new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SdcSchoolCollectionStudentID", sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID());
      }

    return gradeChangedDupe;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void trickleEnrollmentDupeUpdates(SdcDuplicateEntity updatedDupe, SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
    List<SdcSchoolCollectionStudentLightEntity> potentialProgDupeStudents = new ArrayList<>();

    SdcSchoolCollectionStudentLightEntity updatedStudent = sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntityFromSdcStudent(sdcSchoolCollectionStudent);
    Optional<SdcSchoolCollectionEntity> updatedStudentCollectionEntity = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(updatedStudent.getSdcSchoolCollectionID());
    updatedStudentCollectionEntity.ifPresent(updatedStudent::setSdcSchoolCollectionEntity);

    potentialProgDupeStudents.add(updatedStudent);

    Optional<SdcDuplicateStudentEntity> nonUpdatedStudent = updatedDupe.getSdcDuplicateStudentEntities().stream().filter(student -> !Objects.equals(student.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().toString(), sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID())).findFirst();

    if (nonUpdatedStudent.isPresent()) {

      Optional<SdcSchoolCollectionStudentEntity> nonUpdatedStudentEntity = sdcSchoolCollectionStudentRepository.findById(nonUpdatedStudent.get().getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());

      if (nonUpdatedStudentEntity.isPresent()) {

        SdcSchoolCollectionStudentLightEntity nonUpdatedStudentLightEntity = sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntity(nonUpdatedStudentEntity.get());
        Optional<SdcSchoolCollectionEntity> nonUpdatedStudentCollectionEntity = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(nonUpdatedStudentLightEntity.getSdcSchoolCollectionID());
        nonUpdatedStudentCollectionEntity.ifPresent(nonUpdatedStudentLightEntity::setSdcSchoolCollectionEntity);
        potentialProgDupeStudents.add(nonUpdatedStudentLightEntity);
      }

      List<SdcDuplicateEntity> otherEnrollmentDupesForStudent = sdcDuplicateRepository.findAllUnresolvedDuplicatesForStudentByTypeAndSeverity(UUID.fromString(sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID()), DuplicateTypeCode.ENROLLMENT.getCode(), DuplicateSeverityCode.NON_ALLOWABLE.getCode());

      if (!otherEnrollmentDupesForStudent.isEmpty()) {
        potentialProgDupeStudents = generateListOfPotentialProgDupeStudents(otherEnrollmentDupesForStudent, sdcSchoolCollectionStudent, potentialProgDupeStudents);
      }

      List<SdcDuplicateEntity> finalDuplicates = generateFinalDuplicatesSet(potentialProgDupeStudents.stream().toList(), DuplicateLevelCode.valueOf(updatedDupe.getDuplicateLevelCode()), true);

      sdcDuplicateRepository.saveAll(finalDuplicates);
    }
  }


  @Transactional(propagation = Propagation.REQUIRED)
  public List<SdcSchoolCollectionStudentLightEntity> generateListOfPotentialProgDupeStudents(List<SdcDuplicateEntity> dupes, SdcSchoolCollectionStudent sdcSchoolCollectionStudent, List<SdcSchoolCollectionStudentLightEntity> potentialProgDupeStudents){
    dupes.forEach(dupe -> {
      SdcDuplicateEntity updatedDupe = changeGrade(dupe.getSdcDuplicateID(), sdcSchoolCollectionStudent);

      if (updatedDupe != null){
        List<UUID> studentIDs = potentialProgDupeStudents.stream()
                .map(SdcSchoolCollectionStudentLightEntity::getSdcSchoolCollectionStudentID)
                .toList();

        List<SdcDuplicateStudentEntity> updatedStudents = updatedDupe.getSdcDuplicateStudentEntities().stream().toList();
        SdcSchoolCollectionStudentEntity updatedStudent1 = updatedStudents.get(0).getSdcSchoolCollectionStudentEntity();
        SdcSchoolCollectionStudentEntity updatedStudent2 = updatedStudents.get(1).getSdcSchoolCollectionStudentEntity();

        if(!studentIDs.contains(updatedStudent1.getSdcSchoolCollectionStudentID())){
          potentialProgDupeStudents.add(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntity(updatedStudent1));
        }

        if(!studentIDs.contains(updatedStudent2.getSdcSchoolCollectionStudentID())){
          potentialProgDupeStudents.add(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntity(updatedStudent2));
        }
      }
    });
    return potentialProgDupeStudents;
  }

  public Map<UUID, SdcDuplicatesByInstituteID> getInFlightProvincialDuplicates(UUID collectionID, boolean isIndySchoolView) {
    Optional<CollectionEntity> activeCollection = collectionRepository.findActiveCollection();

    if (activeCollection.isPresent()) {
      if (!activeCollection.get().getCollectionID().equals(collectionID)) {
        throw new InvalidParameterException(COLLECTION_ID_NOT_ACTIVE_MSG);
      } else if (activeCollection.get().getCollectionStatusCode().equals(CollectionStatus.PROVDUPES.getCode())) {
        throw new InvalidParameterException(COLLECTION_DUPLICATES_ALREADY_RUN_MSG);
      }
    } else {
      throw new InvalidParameterException(COLLECTION_ID_NOT_ACTIVE_MSG);
    }
    List<SdcSchoolCollectionStudentLightEntity> provinceDupes = sdcSchoolCollectionStudentRepository.findAllInProvinceDuplicateStudentsInCollection(collectionID);
    List<SdcDuplicateEntity> generatedDuplicates = generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL, false);

    Map<UUID, List<SdcDuplicateEntity>> groupedByInstituteID;
    if(isIndySchoolView) {
      groupedByInstituteID = filterToSchoolView(generatedDuplicates);
    } else {
      groupedByInstituteID = filterToDistrictView(generatedDuplicates);
    }

    Map<UUID, SdcDuplicatesByInstituteID> sdcDuplicatesMappedByInstituteID = new HashMap<>();
    for (Map.Entry<UUID, List<SdcDuplicateEntity>> entry : groupedByInstituteID.entrySet()) {
      UUID instituteID = entry.getKey();
      List<SdcDuplicateEntity> duplicates = entry.getValue();

      SdcDuplicatesByInstituteID sdcDuplicatesByInstituteID = new SdcDuplicatesByInstituteID();
      List<SdcDuplicate> sdcDuplicateList = duplicates.stream()
              .map(sdcDuplicateMapper::toSdcDuplicate).toList();

      sdcDuplicatesByInstituteID.setSdcDuplicates(sdcDuplicateList);
      AtomicInteger numProgramDuplicates = new AtomicInteger();
      AtomicInteger numEnrollmentDuplicates = new AtomicInteger();

      duplicates.forEach(dup -> {
        if(dup.getDuplicateTypeCode().equals(DuplicateTypeCode.ENROLLMENT.getCode())) {
          numEnrollmentDuplicates.getAndIncrement();
        } else {
          numProgramDuplicates.getAndIncrement();
        }
      });

      sdcDuplicatesByInstituteID.setNumProgramDuplicates(numProgramDuplicates.intValue());
      sdcDuplicatesByInstituteID.setNumEnrollmentDuplicates(numEnrollmentDuplicates.intValue());

      sdcDuplicatesMappedByInstituteID.put(instituteID, sdcDuplicatesByInstituteID);
    }
    return sdcDuplicatesMappedByInstituteID;
  }

  private Map<UUID, List<SdcDuplicateEntity>> filterToDistrictView(List<SdcDuplicateEntity> duplicates) {
    return duplicates.stream()
            .filter(sdcDuplicateEntity ->
              sdcDuplicateEntity.getDuplicateSeverityCode().equals(DuplicateSeverityCode.NON_ALLOWABLE.getCode())
              && sdcDuplicateEntity.getSdcDuplicateStudentEntities().stream().map(SdcDuplicateStudentEntity::getSdcDistrictCollectionID).distinct().count() > 1
            )
            .flatMap(sdcDuplicateEntity -> sdcDuplicateEntity.getSdcDuplicateStudentEntities().stream()
                    .filter(sdcDuplicateStudentEntity -> sdcDuplicateStudentEntity.getSdcDistrictCollectionID() != null)
                    .map(sdcDuplicateStudentEntity -> new AbstractMap.SimpleEntry<>(sdcDuplicateStudentEntity.getSdcDistrictCollectionID(), sdcDuplicateEntity)))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }

  private Map<UUID, List<SdcDuplicateEntity>> filterToSchoolView(List<SdcDuplicateEntity> duplicates) {
    return duplicates.stream()
              .filter(sdcDuplicateEntity ->
                      sdcDuplicateEntity.getDuplicateSeverityCode().equals(DuplicateSeverityCode.NON_ALLOWABLE.getCode())
                              && sdcDuplicateEntity.getSdcDuplicateStudentEntities().stream().map(SdcDuplicateStudentEntity::getSdcSchoolCollectionID).distinct().count() > 1
              )
              .flatMap(sdcDuplicateEntity -> sdcDuplicateEntity.getSdcDuplicateStudentEntities().stream()
                      .filter(sdcDuplicateStudentEntity -> sdcDuplicateStudentEntity.getSdcDistrictCollectionID() == null)
                      .map(sdcDuplicateStudentEntity -> new AbstractMap.SimpleEntry<>(sdcDuplicateStudentEntity.getSdcSchoolCollectionID(), sdcDuplicateEntity)))
              .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void generateAllProvincialDuplicates(UUID collectionID){
    Optional<CollectionEntity> activeCollection = collectionRepository.findActiveCollection();

    if(activeCollection.isPresent()){
      if(!activeCollection.get().getCollectionID().equals(collectionID)) {
        throw new InvalidParameterException(COLLECTION_ID_NOT_ACTIVE_MSG);
      }else if(activeCollection.get().getCollectionStatusCode().equals(CollectionStatus.PROVDUPES.getCode())){
        throw new InvalidParameterException(COLLECTION_DUPLICATES_ALREADY_RUN_MSG);
      }
    }

    List<SdcSchoolCollectionStudentLightEntity> provinceDupes = sdcSchoolCollectionStudentRepository.findAllInProvinceDuplicateStudentsInCollection(collectionID);

    List<SdcDuplicateEntity> finalDuplicatesSet =  generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL, false);
    sdcDuplicateRepository.saveAll(finalDuplicatesSet);

    activeCollection.ifPresent(collectionEntity -> sendEmailNotificationsForProvinceDuplicates(formatter.format(collectionEntity.getDuplicationResolutionDueDate())));

    this.collectionRepository.updateCollectionStatus(collectionID, String.valueOf(CollectionStatus.PROVDUPES));

    List<SdcSchoolCollectionEntity> schoolCollectionsWithoutDupes = sdcSchoolCollectionRepository.findAllSchoolCollectionsWithoutProvincialDupes(collectionID);
    List<SdcSchoolCollectionEntity> schoolCollectionsWithDupes = sdcSchoolCollectionRepository.findAllSchoolCollectionsWithProvincialDupes(collectionID);

    updateSchoolCollectionStatuses(schoolCollectionsWithoutDupes, SdcSchoolCollectionStatus.COMPLETED.getCode());
    updateSchoolCollectionStatuses(schoolCollectionsWithDupes, SdcSchoolCollectionStatus.P_DUP_POST.getCode());

    sdcDistrictCollectionRepository.updateAllDistrictCollectionStatus(collectionID, String.valueOf(SdcDistrictCollectionStatus.P_DUP_POST));
  }

  private List<SdcDuplicateEntity> generateFinalDuplicatesSet(List<SdcSchoolCollectionStudentLightEntity> duplicateStudentEntities, DuplicateLevelCode duplicateLevelCode, Boolean isTrickle) {
    HashMap<UUID, List<SdcSchoolCollectionStudentLightEntity>> groupedDups = new HashMap<>();

    for (SdcSchoolCollectionStudentLightEntity student : duplicateStudentEntities) {
      groupedDups.computeIfAbsent(student.getAssignedStudentId(), k -> new ArrayList<>()).add(student);
    }
    List<SdcDuplicateEntity> finalDuplicatesSet = new ArrayList<>();

    for (List<SdcSchoolCollectionStudentLightEntity> group : groupedDups.values()) {
      for (int i = 0; i < group.size(); i++) {
        SdcSchoolCollectionStudentLightEntity entity1 = group.get(i);
        for (int j = i + 1; j < group.size(); j++) {
          SdcSchoolCollectionStudentLightEntity entity2 = group.get(j);
          List<SdcDuplicateEntity> duplicateRecords = runDuplicatesCheck(duplicateLevelCode, entity1, entity2, isTrickle);
          finalDuplicatesSet.addAll(duplicateRecords);
        }
      }
    }

    return finalDuplicatesSet;
  }

  @Transactional
  public void resolveRemainingDuplicates(UUID collectionID){
    Optional<CollectionEntity> activeCollection = collectionRepository.findActiveCollection();

    if(activeCollection.isPresent()){
      if(!activeCollection.get().getCollectionID().equals(collectionID)) {
        throw new InvalidParameterException(COLLECTION_ID_NOT_ACTIVE_MSG);
      }else if(activeCollection.get().getCollectionStatusCode().equals(CollectionStatus.INPROGRESS.getCode())){
        throw new InvalidParameterException("Provided collectionID has not yet run provincial duplicates.");
      }else if(activeCollection.get().getCollectionStatusCode().equals(CollectionStatus.DUPES_RES.getCode())){
        throw new InvalidParameterException("Provided collectionID has already resolved all duplicates.");
      }
    }

    resolveEnrollmentDuplicates();

    // Generate new program dupes
    List<SdcSchoolCollectionStudentLightEntity> provinceDupes = sdcSchoolCollectionStudentRepository.findAllInProvinceDuplicateStudentsInCollection(collectionID);
    List<SdcDuplicateEntity> finalDuplicatesSet =  generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL, false);
    sdcDuplicateRepository.saveAll(finalDuplicatesSet);

    resolveProgramDuplicates();

    CollectionEntity collection = collectionRepository.findById(collectionID).orElse(null);
    if (collection != null) {
      collection.setCollectionStatusCode(String.valueOf(CollectionStatus.DUPES_RES.getCode()));
      collectionRepository.save(collection);
    }

    List<SdcSchoolCollectionEntity> schoolCollections = sdcSchoolCollectionRepository.findUncompletedSchoolCollections(collectionID);
    updateSchoolCollectionStatuses(schoolCollections, SdcSchoolCollectionStatus.COMPLETED.getCode());
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void resolveEnrollmentDuplicates(){
    List<SdcDuplicateEntity> unresolvedDupes = sdcDuplicateRepository.findAllUnresolvedNonAllowableEnrollmentDuplicatesForCurrentCollection();

    unresolvedDupes.forEach(dupe -> {
      SdcDuplicateStudentEntity studentDupe1 = dupe.getSdcDuplicateStudentEntities().stream().findFirst().orElse(null);
      SdcDuplicateStudentEntity studentDupe2 = dupe.getSdcDuplicateStudentEntities().stream().skip(1).findFirst().orElse(null);

      if(studentDupe1 != null && studentDupe2 != null){
        SdcSchoolCollectionEntity school1Collection = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(studentDupe1.getSdcSchoolCollectionID()).get();
        SdcSchoolCollectionEntity school2Collection = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(studentDupe2.getSdcSchoolCollectionID()).get();

        SchoolTombstone school1 = restUtils.getSchoolBySchoolID(String.valueOf(school1Collection.getSchoolID())).get();
        SchoolTombstone school2 = restUtils.getSchoolBySchoolID(String.valueOf(school2Collection.getSchoolID())).get();

        SdcSchoolCollectionStudent student1 = sdcSchoolCollectionStudentMapper.toSdcSchoolStudent(studentDupe1.getSdcSchoolCollectionStudentEntity());
        SdcSchoolCollectionStudent student2 = sdcSchoolCollectionStudentMapper.toSdcSchoolStudent(studentDupe2.getSdcSchoolCollectionStudentEntity());

        Integer dupe1ClassNum = duplicateClassNumberGenerationService.generateDuplicateClassNumber(studentDupe1.getSdcDuplicateStudentID(), school1.getFacilityTypeCode(), school1.getSchoolCategoryCode(), student1.getEnrolledGradeCode());
        Integer dupe2ClassNum = duplicateClassNumberGenerationService.generateDuplicateClassNumber(studentDupe2.getSdcDuplicateStudentID(), school2.getFacilityTypeCode(), school2.getSchoolCategoryCode(), student2.getEnrolledGradeCode());

        SdcSchoolCollectionStudent studentToRemove = identifyStudentToRemove(student1, student2, dupe1ClassNum, dupe2ClassNum, school1, school2);
        softDeleteEnrollmentDuplicate(dupe.getSdcDuplicateID(), studentToRemove);
      } else {
        throw new EntityNotFoundException(SdcDuplicateEntity.class, SDC_DUPLICATE_ID_KEY, dupe.getSdcDuplicateID().toString());
      }
    });
  }

  public SdcSchoolCollectionStudent identifyStudentToRemove(SdcSchoolCollectionStudent student1, SdcSchoolCollectionStudent student2, Integer dupe1ClassNum, Integer dupe2ClassNum, SchoolTombstone school1, SchoolTombstone school2){
    SdcSchoolCollectionStudent studentToRemove = null;

    if (dupe1ClassNum == null || dupe2ClassNum == null || dupe1ClassNum.equals(dupe2ClassNum)){
      if(student1.getFte().compareTo(student2.getFte()) > 0){
        studentToRemove = student2;
      } else if (student1.getFte().compareTo(student2.getFte()) < 0){
        studentToRemove = student1;
      } else {
        Integer institute1Number = getInstituteNumber(school1);
        Integer institute2Number = getInstituteNumber(school2);

        studentToRemove = institute1Number < institute2Number ? student2 : student1;
      }
    } else {
      studentToRemove = dupe1ClassNum.compareTo(dupe2ClassNum) < 0 ? student1 : student2;
    }

    return studentToRemove;
  }

  public void resolveProgramDuplicates(){
    List<SdcDuplicateEntity> unresolvedDupes = sdcDuplicateRepository.findAllUnresolvedNonAllowableProgramDuplicatesForCurrentCollection();

    unresolvedDupes.forEach(dupe -> {
      SdcDuplicateStudentEntity studentDupe1 = dupe.getSdcDuplicateStudentEntities().stream().findFirst().orElse(null);
      SdcDuplicateStudentEntity studentDupe2 = dupe.getSdcDuplicateStudentEntities().stream().skip(1).findFirst().orElse(null);

      if(studentDupe1 != null && studentDupe2 != null){
        SdcSchoolCollectionStudentEntity student1 = studentDupe1.getSdcSchoolCollectionStudentEntity();
        SdcSchoolCollectionStudentEntity student2 = studentDupe2.getSdcSchoolCollectionStudentEntity();

        SdcSchoolCollectionEntity school1Collection = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(studentDupe1.getSdcSchoolCollectionID()).get();
        SdcSchoolCollectionEntity school2Collection = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(studentDupe2.getSdcSchoolCollectionID()).get();

        SchoolTombstone school1 = restUtils.getSchoolBySchoolID(String.valueOf(school1Collection.getSchoolID())).get();
        SchoolTombstone school2 = restUtils.getSchoolBySchoolID(String.valueOf(school2Collection.getSchoolID())).get();

        SdcSchoolCollectionStudentEntity studentToEdit = identifyStudentToEdit(student1, student2, school1, school2);

        SdcSchoolCollectionStudentEntity updatedStudent = removeDupeProgram(studentToEdit, dupe.getProgramDuplicateTypeCode());
        sdcSchoolCollectionStudentService.saveSdcStudentWithHistory(updatedStudent);

        dupe.setDuplicateResolutionCode(DuplicateResolutionCode.RESOLVED.getCode());
        dupe.setUpdateDate(LocalDateTime.now());
        dupe.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        TransformUtil.uppercaseFields(dupe);
        sdcDuplicateRepository.save(dupe);
      }
    });
  }

  SdcSchoolCollectionStudentEntity identifyStudentToEdit(SdcSchoolCollectionStudentEntity student1, SdcSchoolCollectionStudentEntity student2, SchoolTombstone school1, SchoolTombstone school2){
    SdcSchoolCollectionStudentEntity studentToEdit = null;

    if(student1.getNumberOfCoursesDec().compareTo(student2.getNumberOfCoursesDec()) > 0){
      studentToEdit = student2;
    } else if (student1.getNumberOfCoursesDec().compareTo(student2.getNumberOfCoursesDec()) < 0){
      studentToEdit = student1;
    } else if(independentSchoolCategoryCodes.contains(school2.getSchoolCategoryCode()) && !independentSchoolCategoryCodes.contains(school1.getSchoolCategoryCode())){
      studentToEdit = student2;
    } else if (independentSchoolCategoryCodes.contains(school1.getSchoolCategoryCode()) && !independentSchoolCategoryCodes.contains(school2.getSchoolCategoryCode())){
      studentToEdit = student1;
    } else if (Objects.equals(school1.getFacilityTypeCode(), FacilityTypeCodes.STANDARD.getCode()) && !Objects.equals(school2.getFacilityTypeCode(), FacilityTypeCodes.STANDARD.getCode())){
      studentToEdit = student2;
    } else if (Objects.equals(school2.getFacilityTypeCode(), FacilityTypeCodes.STANDARD.getCode()) && !Objects.equals(school1.getFacilityTypeCode(), FacilityTypeCodes.STANDARD.getCode())){
      studentToEdit = student1;
    } else {
      Integer institute1Number = getInstituteNumber(school1);
      Integer institute2Number = getInstituteNumber(school2);

      if (institute1Number < institute2Number){
        studentToEdit = student2;
      } else {
        studentToEdit = student1;
      }
    }
    return studentToEdit;
  }

  SdcSchoolCollectionStudentEntity removeDupeProgram(SdcSchoolCollectionStudentEntity student, String programDuplicateTypeCode){
    List<String> enrolledPrograms = new ArrayList<>(validationRulesService.splitEnrolledProgramsString(student.getEnrolledProgramCodes()));

    if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.CAREER.getCode())){
      List<String> studentCareerProgramCodes = EnrolledProgramCodes.getCareerProgramCodes().stream().filter(enrolledPrograms::contains).toList();
      enrolledPrograms.removeAll(studentCareerProgramCodes);
      student.setCareerProgramCode(null);
    } else if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.INDIGENOUS.getCode())){
      List<String> studentIndigenousProgramCodes = EnrolledProgramCodes.getIndigenousProgramCodes().stream().filter(enrolledPrograms::contains).toList();
      enrolledPrograms.removeAll(studentIndigenousProgramCodes);
    } else if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.LANGUAGE.getCode())){
      List<String> studentLanguageProgramCodes = EnrolledProgramCodes.getFrenchProgramCodesWithEll().stream().filter(enrolledPrograms::contains).toList();
      enrolledPrograms.removeAll(studentLanguageProgramCodes);
    } else if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.SPECIAL_ED.getCode())){
      enrolledPrograms.remove(student.getSpecialEducationCategoryCode());
      student.setSpecialEducationCategoryCode(null);
    }

    student.setEnrolledProgramCodes(String.join("", enrolledPrograms));
    return student;
  }

  public Integer getInstituteNumber(SchoolTombstone school){
    Integer instituteNumber = null;
    if(independentSchoolCategoryCodes.contains(school.getSchoolCategoryCode())){
      instituteNumber = Integer.parseInt(school.getMincode());
    } else {
      Optional<District> district = restUtils.getDistrictByDistrictID(school.getDistrictId());

      if(district.isPresent()){
        instituteNumber = Integer.parseInt(district.get().getDistrictNumber());
      } else {
        throw new EntityNotFoundException(District.class, "districtID", school.getDistrictId());
      }
    }
    return instituteNumber;
  }

  @Transactional
  public void sendEmailNotificationsForProvinceDuplicates(String dueDate){
    Map<UUID, SdcSchoolCollection1701Users> emailList = generateEmailListForProvinceDuplicates();
    if(!emailList.isEmpty()){
      scheduleHandlerService.createAndStartProvinceDuplicateEmailSagas(emailList, dueDate);
    }
  }

  public void updateSchoolCollectionStatuses(List<SdcSchoolCollectionEntity> schoolCollections, String schoolCollectionStatus){
    schoolCollections.forEach(schoolCollection -> {
      schoolCollection.setSdcSchoolCollectionStatusCode(schoolCollectionStatus);
      sdcSchoolCollectionService.saveSdcSchoolCollectionWithHistory(schoolCollection);
    });
  }

  public Map<UUID, SdcSchoolCollection1701Users> generateEmailListForProvinceDuplicates(){
    List<SdcDuplicateEntity> sdcDuplicateEntities = sdcDuplicateRepository.findAllProvincialDuplicatesForCurrentCollection();
    final HashMap<UUID, SdcSchoolCollection1701Users> schoolAndDistrict1701Emails = new HashMap<>();

    sdcDuplicateEntities.forEach(provinceDupe -> provinceDupe.getSdcDuplicateStudentEntities().forEach(student -> {
      Optional<SdcSchoolCollectionEntity> optionalEntity = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(student.getSdcSchoolCollectionID());

      SdcSchoolCollection sdcSchoolCollection = optionalEntity.map(sdcSchoolCollectionMapper::toStructure)
              .orElse(null);

      assert sdcSchoolCollection != null;
      UUID schoolID = UUID.fromString(sdcSchoolCollection.getSchoolID());
      UUID districtID = getDistrictID(schoolID);

      List<EdxUser> schoolEdxUsers = restUtils.get1701Users(schoolID, null);
      List<EdxUser> districtEdxUsers = restUtils.get1701Users(null, districtID);
      Set<String> emails = pluckEmailAddresses(schoolEdxUsers, schoolID, null);
      emails.addAll(pluckEmailAddresses(districtEdxUsers, null, districtID));

      if (!schoolAndDistrict1701Emails.containsKey(student.getSdcSchoolCollectionID())){
        SdcSchoolCollection1701Users schoolAndDistrictUsers = new SdcSchoolCollection1701Users();

        schoolAndDistrictUsers.setSchoolDisplayName(getSchoolName(schoolID));
        schoolAndDistrictUsers.setEmails(emails);

        schoolAndDistrict1701Emails.put(student.getSdcSchoolCollectionID(), schoolAndDistrictUsers);
      }
    }));

    log.info("Found :: {} school collections with provincial duplicates.", schoolAndDistrict1701Emails.size());

    return schoolAndDistrict1701Emails;
  }

  private UUID getDistrictID(UUID schoolID){
    Optional<SchoolTombstone> optionalSchool = restUtils.getSchoolBySchoolID(String.valueOf(schoolID));

    if (optionalSchool.isPresent()) {
      SchoolTombstone school = optionalSchool.get();
      return UUID.fromString(school.getDistrictId());
    }

    return null;
  }

  private String getSchoolName(UUID schoolID){
    Optional<SchoolTombstone> optionalSchool = restUtils.getSchoolBySchoolID(String.valueOf(schoolID));

    if (optionalSchool.isPresent()) {
      SchoolTombstone school = optionalSchool.get();
      return school.getDisplayName();
    }

    return null;
  }

  public Set<String> pluckEmailAddresses (List<EdxUser> edxUsers, UUID schoolID, UUID districtID){
    final Set<String> emailSet = new HashSet<>();
    edxUsers.forEach(user -> {
      if (schoolID != null && districtID == null) {
        user.getEdxUserSchools().forEach(school -> {
          if (school.getSchoolID().equals(schoolID)) {
            school.getEdxUserSchoolRoles().forEach(role -> {
              if (Objects.equals(role.getEdxRoleCode(), "SCHOOL_SDC")) {
                emailSet.add(user.getEmail());
              }
            });
          }
        });
      } else if(districtID != null && schoolID == null) {
        user.getEdxUserDistricts().forEach(district -> {
          if (district.getDistrictID().equals(districtID.toString())) {
            district.getEdxUserDistrictRoles().forEach(role -> {
              if (Objects.equals(role.getEdxRoleCode(), "DISTRICT_SDC")) {
                emailSet.add(user.getEmail());
              }
            });
          }
        });
      }
    });
    return emailSet;
  }

  public List<SdcDuplicateEntity> runDuplicatesCheck(DuplicateLevelCode level, SdcSchoolCollectionStudentLightEntity entity1, SdcSchoolCollectionStudentLightEntity entity2, boolean isTrickle){
    List<SdcDuplicateEntity> dups = new ArrayList<>();
    SchoolTombstone schoolTombstone1 = restUtils.getSchoolBySchoolID(entity1.getSdcSchoolCollectionEntitySchoolID().toString()).orElseThrow(() ->
            new StudentDataCollectionAPIRuntimeException("School provided by ID " + entity1.getSdcSchoolCollectionEntitySchoolID() + "was not found - this is not expected"));
    SchoolTombstone schoolTombstone2 = restUtils.getSchoolBySchoolID(entity2.getSdcSchoolCollectionEntitySchoolID().toString()).orElseThrow(() ->
            new StudentDataCollectionAPIRuntimeException("School provided by ID " + entity2.getSdcSchoolCollectionEntitySchoolID() + "was not found - this is not expected"));
    //Is the student an adult?
    if(entity1.getIsAdult() == null) {
      entity1.setIsAdult(DOBUtil.isAdult(entity1.getDob()));
    }
    if(entity2.getIsAdult() == null) {
      entity2.setIsAdult(DOBUtil.isAdult(entity2.getDob()));
    }
    if(entity1.getIsAdult() || entity2.getIsAdult()){
      addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
    }

    //In which grades are the two records reported - K-9 Check
    if(dups.isEmpty() && !isTrickle && SchoolGradeCodes.getKToNineGrades().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getKToNineGrades().contains(entity2.getEnrolledGradeCode())){
      addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.K_TO_9_DUP);
    }

    //In which grades are the two records reported - K-7 & 10-12,SU Check
    if(dups.isEmpty() && !isTrickle && ((SchoolGradeCodes.getKToSevenEuGrades().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode())) ||
            (SchoolGradeCodes.getKToSevenEuGrades().contains(entity2.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode())))){
      addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.K_TO_7_DUP);
    }

    List<String> facilityOnlineCodes = Arrays.asList(FacilityTypeCodes.DISTONLINE.getCode(), FacilityTypeCodes.DIST_LEARN.getCode());

    //In which grades are the two records reported - 10,11,12,SU Check
    var isSchool1Independent = SchoolCategoryCodes.INDEPENDENTS.contains(schoolTombstone1.getSchoolCategoryCode());
    var isSchool2Independent = SchoolCategoryCodes.INDEPENDENTS.contains(schoolTombstone2.getSchoolCategoryCode());
    if(dups.isEmpty() && SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode())){
      if((facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()) && isSchool1Independent) ||
              (facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode()) && isSchool2Independent)) {
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else if(isSchool1Independent || isSchool2Independent) {
        if((facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode())) || (isSchool2Independent && facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()))) {
          addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
        }else if(!isTrickle){
          addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.ALT_DUP);
        }
      }else if(schoolTombstone1.getDistrictId().equals(schoolTombstone2.getDistrictId())){
        if(!isTrickle && (FacilityTypeCodes.ALT_PROGS.getCode().equals(schoolTombstone1.getFacilityTypeCode()) && !facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode())) || (FacilityTypeCodes.ALT_PROGS.getCode().equals(schoolTombstone2.getFacilityTypeCode()) && !facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()))){
          addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.ALT_DUP);
        }else{
          addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
        }
      }else if(facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()) || facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode())){
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else if (!isTrickle){
        addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.ALT_DUP);
      }
    }

    //In which grades are the two records reported - 8&9, 10-12,SU Check
    var isStudent1Grade8or9 = SchoolGradeCodes.getGrades8and9().contains(entity1.getEnrolledGradeCode());
    var isStudent2Grade8or9 = SchoolGradeCodes.getGrades8and9().contains(entity2.getEnrolledGradeCode());
    var isStudent1Grade10toSU = SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode());
    var isStudent2Grade10toSU = SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode());

    if(dups.isEmpty() && ((isStudent1Grade8or9 && isStudent2Grade10toSU) || (isStudent2Grade8or9 && isStudent1Grade10toSU))){
      if(facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()) && facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode())) {
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else if((isStudent2Grade10toSU && facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode())) ||
              (isStudent1Grade10toSU && facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()))){
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else if(!isTrickle){
        addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.IN_8_9_DUP);
      }
    }

    return dups;
  }

  private List<SdcDuplicateEntity> runProgramDuplicates(List<SdcDuplicateEntity> newDuplicates, SdcSchoolCollectionStudentLightEntity student1, SdcSchoolCollectionStudentLightEntity student2, DuplicateLevelCode level){
    var dups = new ArrayList<>(newDuplicates);
    for(SdcDuplicateEntity duplicateEntity: dups){
      if(duplicateEntity.getDuplicateSeverityCode().equals(DuplicateSeverityCode.ALLOWABLE.getCode())){
        List<String> student1Programs = validationRulesService.splitEnrolledProgramsString(student1.getEnrolledProgramCodes());
        List<String> student2Programs = validationRulesService.splitEnrolledProgramsString(student2.getEnrolledProgramCodes());

        if(StringUtils.isNotBlank(student1.getCareerProgramCode()) && StringUtils.isNotBlank(student2.getCareerProgramCode()) && student1.getCareerProgramCode().equals(student2.getCareerProgramCode())){
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.CAREER,null));
        }

        if(StringUtils.isNotBlank(student1.getSpecialEducationCategoryCode()) && StringUtils.isNotBlank(student2.getSpecialEducationCategoryCode())){
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.SPECIAL_ED,null));
        }

        List<String> student1IndigenousPrograms = EnrolledProgramCodes.getIndigenousProgramCodes().stream().filter(student1Programs::contains).toList();
        List<String> student2IndigenousPrograms = EnrolledProgramCodes.getIndigenousProgramCodes().stream().filter(student2Programs::contains).toList();
        if(student1IndigenousPrograms.stream().anyMatch(student2IndigenousPrograms::contains)){
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.INDIGENOUS,null));
        }

        List<String> student1LanguagePrograms = EnrolledProgramCodes.getFrenchProgramCodesWithEll().stream().filter(student1Programs::contains).toList();
        List<String> student2LanguagePrograms = EnrolledProgramCodes.getFrenchProgramCodesWithEll().stream().filter(student2Programs::contains).toList();
        if(student1LanguagePrograms.stream().anyMatch(student2LanguagePrograms::contains)){
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.LANGUAGE,null));
        }

        List<String> student1CareerPrograms = EnrolledProgramCodes.getCareerProgramCodes().stream().filter(student1Programs::contains).toList();
        List<String> student2CareerPrograms = EnrolledProgramCodes.getCareerProgramCodes().stream().filter(student2Programs::contains).toList();
        if(student1CareerPrograms.stream().anyMatch(student2CareerPrograms::contains) &&
                newDuplicates.stream().allMatch(dup -> dup.getProgramDuplicateTypeCode() == null || !dup.getProgramDuplicateTypeCode().equals(ProgramDuplicateTypeCode.CAREER.getCode()))){
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.CAREER,null));
        }
      }
    }

    return newDuplicates;
  }

  private void addAllowableDuplicateWithProgramDups(List<SdcDuplicateEntity> newDuplicates, DuplicateLevelCode levelCode, SdcSchoolCollectionStudentLightEntity entity1, SdcSchoolCollectionStudentLightEntity entity2, DuplicateTypeCode typeCode, ProgramDuplicateTypeCode programDuplicateTypeCode){
    newDuplicates.add(generateDuplicateEntity(levelCode,entity1,entity2,typeCode,DuplicateSeverityCode.ALLOWABLE,programDuplicateTypeCode,null));
    runProgramDuplicates(newDuplicates,entity1,entity2,levelCode);
  }

  private void addNonAllowableDuplicate(List<SdcDuplicateEntity> newDuplicates, DuplicateLevelCode levelCode, SdcSchoolCollectionStudentLightEntity entity1, SdcSchoolCollectionStudentLightEntity entity2, DuplicateTypeCode typeCode, ProgramDuplicateTypeCode programDuplicateTypeCode, DuplicateErrorDescriptionCode duplicateErrorDescriptionCode){
    newDuplicates.add(generateDuplicateEntity(levelCode,entity1,entity2,typeCode,DuplicateSeverityCode.NON_ALLOWABLE,programDuplicateTypeCode,duplicateErrorDescriptionCode));
  }

  private SdcDuplicateEntity generateDuplicateEntity(DuplicateLevelCode levelCode, SdcSchoolCollectionStudentLightEntity entity1, SdcSchoolCollectionStudentLightEntity entity2, DuplicateTypeCode typeCode, DuplicateSeverityCode severityCode, ProgramDuplicateTypeCode programDuplicateTypeCode, DuplicateErrorDescriptionCode duplicateErrorDescriptionCode){
    SdcDuplicateEntity dupe = new SdcDuplicateEntity();
    dupe.setCreateDate(LocalDateTime.now());
    dupe.setUpdateDate(LocalDateTime.now());
    dupe.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    dupe.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    dupe.setDuplicateTypeCode(typeCode.getCode());
    dupe.setDuplicateSeverityCode(severityCode.getCode());
    dupe.setProgramDuplicateTypeCode(programDuplicateTypeCode != null ? programDuplicateTypeCode.getCode() : null);
    dupe.setDuplicateLevelCode(levelCode.getCode());
    dupe.setDuplicateErrorDescriptionCode(duplicateErrorDescriptionCode != null ? duplicateErrorDescriptionCode.getCode() : null);
    dupe.setCollectionID(entity1.getSdcSchoolCollectionEntity().getCollectionEntity().getCollectionID());

    List<SdcDuplicateStudentEntity> studs = new ArrayList<>();
    studs.add(createSdcDuplicateStudent(entity1, dupe));
    studs.add(createSdcDuplicateStudent(entity2, dupe));
    dupe.getSdcDuplicateStudentEntities().addAll(studs);

    return dupe;
  }

  private SdcDuplicateStudentEntity createSdcDuplicateStudent(SdcSchoolCollectionStudentLightEntity studentEntity, SdcDuplicateEntity dupe){
    SdcDuplicateStudentEntity student = new SdcDuplicateStudentEntity();
    student.setSdcDistrictCollectionID(studentEntity.getSdcSchoolCollectionEntity().getSdcDistrictCollectionID());
    student.setSdcSchoolCollectionID(studentEntity.getSdcSchoolCollectionEntity().getSdcSchoolCollectionID());
    student.setSdcDuplicateEntity(dupe);
    student.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentEntity(studentEntity));
    student.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    student.setCreateDate(LocalDateTime.now());
    student.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    student.setUpdateDate(LocalDateTime.now());
    return student;
  }

}
