package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateResolutionCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramDuplicateTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

import static ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil.isCollectionInProvDupes;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcDuplicatesService {

  private final SdcDuplicateRepository sdcDuplicateRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final CollectionRepository collectionRepository;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final ValidationRulesService validationRulesService;
  private final ScheduleHandlerService scheduleHandlerService;
  private final SdcSchoolCollectionService sdcSchoolCollectionService;
  private static final SdcSchoolCollectionStudentMapper sdcSchoolCollectionStudentMapper = SdcSchoolCollectionStudentMapper.mapper;
  private static final SdcDuplicateMapper sdcDuplicateMapper = SdcDuplicateMapper.mapper;
  private final RestUtils restUtils;
  private static final String SDC_DUPLICATE_ID_KEY = "sdcDuplicateID";
  private static final String COLLECTION_ID_NOT_ACTIVE_MSG = "Provided collectionID does not match currently active collectionID.";
  private static final String COLLECTION_DUPLICATES_ALREADY_RUN_MSG = "Provided collectionID has already run provincial duplicates.";
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
  private static final List<String> independentSchoolCategoryCodes = Arrays.asList(SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.INDP_FNS.getCode());

  public List<SdcSchoolCollectionStudentEntity> getAllSchoolCollectionDuplicates(UUID sdcSchoolCollectionID) {
    return sdcSchoolCollectionStudentRepository.findAllDuplicateStudentsInSdcSchoolCollection(sdcSchoolCollectionID);
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

  public void generateAllowableDuplicatesOrElseThrow(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity, boolean isCollectionInProvDupes){
    List<SdcSchoolCollectionStudentEntity> allStudentsWithSameAssignedStudentId = sdcSchoolCollectionStudentRepository
            .findAllDuplicateStudentsByCollectionID(sdcSchoolCollectionStudentEntity.getSdcSchoolCollection().getCollectionEntity().getCollectionID(), Collections.singletonList(sdcSchoolCollectionStudentEntity.getAssignedStudentId()));

    List<SdcSchoolCollectionStudentLightEntity> duplicateStudentEntities = new ArrayList<>();
    //map to light object
    duplicateStudentEntities.addAll(allStudentsWithSameAssignedStudentId.stream().map(sdcSchoolCollectionStudentMapper::toSdcSchoolStudentLightEntity).toList());
    if(sdcSchoolCollectionStudentEntity.getSdcSchoolCollectionStudentID() == null) {
      duplicateStudentEntities.add(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntity(sdcSchoolCollectionStudentEntity));
    }
    //Generate new PROV dupes
    List<SdcDuplicateEntity> generatedDuplicates = generateFinalDuplicatesSet(duplicateStudentEntities, DuplicateLevelCode.PROVINCIAL, false);
    //Check if we have any non-allowable or program dupes created
    List<SdcDuplicateEntity> nonAllowableOrProgramDupes = generatedDuplicates.stream().filter(duplicate ->
            (duplicate.getDuplicateSeverityCode().equals(DuplicateSeverityCode.NON_ALLOWABLE.getCode()) &&
            duplicate.getDuplicateTypeCode().equals(DuplicateTypeCode.ENROLLMENT.getCode())) || duplicate.getDuplicateTypeCode().equals(DuplicateTypeCode.PROGRAM.getCode())).toList();

    if(!nonAllowableOrProgramDupes.isEmpty() && isCollectionInProvDupes) {
      log.debug("SdcSchoolCollectionStudent was not saved to the database because it would create a duplicate on save :: {}", sdcSchoolCollectionStudentEntity.getAssignedStudentId());
      throw new InvalidPayloadException(createDuplicatesThrow(sdcSchoolCollectionStudentEntity.getAssignedPen()));
    }

    List<SdcDuplicateEntity> allowableDupes = generatedDuplicates.stream().filter(duplicate ->  duplicate.getDuplicateSeverityCode().equals(DuplicateSeverityCode.ALLOWABLE.getCode())).toList();
    if(!allowableDupes.isEmpty()) {
      List<SdcDuplicateEntity> existingUnresolvedDupesForStudent = sdcDuplicateRepository.findAllUnresolvedDuplicatesForStudent(sdcSchoolCollectionStudentEntity.getSdcSchoolCollectionStudentID());
      List<SdcDuplicateEntity> existingAllowableDupes = existingUnresolvedDupesForStudent.stream().filter(duplicate -> duplicate.getDuplicateSeverityCode().equals(DuplicateSeverityCode.ALLOWABLE.getCode())).toList();

      allowableDupes.forEach(allowableDupe -> {
        if (existingAllowableDupes.stream().map(SdcDuplicateEntity::getUniqueObjectHash).noneMatch(duplicateHash -> duplicateHash == allowableDupe.getUniqueObjectHash())) {
          sdcDuplicateRepository.save(allowableDupe);
        }
      });
    }
  }

  private ApiError createDuplicatesThrow(String assignedPEN) {
    ApiError error = ApiError.builder()
            .timestamp(LocalDateTime.now())
            .message("SdcSchoolCollectionStudent was not saved to the database because it would create a new duplicate.")
            .status(HttpStatus.BAD_REQUEST)
            .build();

    var validationError = ValidationUtil.createFieldError(
            "sdcSchoolCollectionStudent",
            assignedPEN,
            "Edits would create a new duplicate."
    );

    List<FieldError> fieldErrorList = new ArrayList<>();
    fieldErrorList.add(validationError);
    error.addValidationErrors(fieldErrorList);

    return error;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void deleteAllDuplicatesForStudent(UUID sdcSchoolCollectionStudentID) {
    sdcDuplicateRepository.deleteAllBySdcDuplicateStudentEntities_SdcSchoolCollectionStudentEntity_SdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void resolveAllExistingDuplicatesForSoftDelete(UUID sdcSchoolCollectionStudentID){
    //delete resolved enrollment dupes since both associated students have now been soft deleted
    List<SdcDuplicateEntity> studentDuplicates = sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionStudentEntity_SdcSchoolCollectionStudentID(sdcSchoolCollectionStudentID);
    studentDuplicates.forEach(dupe -> {
      Optional<SdcDuplicateStudentEntity> otherStudent = dupe.getSdcDuplicateStudentEntities().stream().filter(dupeStd -> !dupeStd.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().equals(sdcSchoolCollectionStudentID)).findFirst();
      if((StringUtils.isNotBlank(dupe.getDuplicateResolutionCode()) && dupe.getDuplicateResolutionCode().equals(DuplicateResolutionCode.RELEASED.getCode()))
              || dupe.getDuplicateTypeCode().equals(DuplicateTypeCode.PROGRAM.getCode()) ||
              (dupe.getDuplicateTypeCode().equals(DuplicateTypeCode.ENROLLMENT.getCode()) && dupe.getDuplicateSeverityCode().equals(DuplicateSeverityCode.ALLOWABLE.getCode()))){
        sdcDuplicateRepository.delete(dupe);
      }else{
        dupe.setDuplicateResolutionCode(DuplicateResolutionCode.RELEASED.getCode());
        dupe.setRetainedSdcSchoolCollectionStudentEntity(otherStudent.get().getSdcSchoolCollectionStudentEntity());
        sdcDuplicateRepository.save(dupe);
      }
    });
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void resolveAllExistingDuplicatesForType(SdcSchoolCollectionStudentEntity curStudentEntity, DuplicateResolutionCode resolutionCode){
    // get all existing dupes
    List<SdcDuplicateEntity> existingStudentDuplicates = sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionStudentEntity_SdcSchoolCollectionStudentID(curStudentEntity.getSdcSchoolCollectionStudentID());
    //for each existing dupe
    existingStudentDuplicates.forEach(dupe -> {
      // get other student
      Optional<SdcDuplicateStudentEntity> otherStudent = dupe.getSdcDuplicateStudentEntities().stream().filter(dupeStd -> !dupeStd.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().equals(curStudentEntity.getSdcSchoolCollectionStudentID())).findFirst();
      // get new duplicates between the 2 students
      var studentDupes = runDuplicatesCheck(DuplicateLevelCode.PROVINCIAL, sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntity(curStudentEntity), otherStudent.get().getSdcSchoolCollectionStudentEntity(), true);
      var nonAllowableDupes = studentDupes.stream().filter(sdcDuplicateEntity -> sdcDuplicateEntity.getDuplicateSeverityCode().equals(DuplicateSeverityCode.NON_ALLOWABLE.getCode())
              && sdcDuplicateEntity.getDuplicateTypeCode().equals(DuplicateTypeCode.ENROLLMENT.getCode())).toList();
      // check if any new non-allowable enrollment dupes ?
      if(!nonAllowableDupes.isEmpty()) {
        log.debug("SdcSchoolCollectionStudent was not saved to the database because it would create a duplicate on save :: {}", curStudentEntity.getAssignedStudentId());
        throw new InvalidPayloadException(createDuplicatesThrow(curStudentEntity.getAssignedPen()));
      }

      // check if existing dupe is also present in new dupes list
      if (studentDupes.stream().map(SdcDuplicateEntity::getUniqueObjectHash).noneMatch(duplicateHash -> duplicateHash == dupe.getUniqueObjectHash())) {
        // resolve non-allowable dupe
        if(dupe.getDuplicateTypeCode().equals(DuplicateTypeCode.ENROLLMENT.getCode()) && dupe.getDuplicateSeverityCode().equals(DuplicateSeverityCode.NON_ALLOWABLE.getCode())){
          if(StringUtils.isBlank(dupe.getDuplicateResolutionCode()))
          {
            dupe.setDuplicateResolutionCode(resolutionCode.getCode());
            dupe.setRetainedSdcSchoolCollectionStudentEntity(otherStudent.get().getSdcSchoolCollectionStudentEntity());
            sdcDuplicateRepository.save(dupe);

            //save new allowable and prog. dupe
            var programAndAllowableDupes = studentDupes.stream().filter(sdcDuplicateEntity -> sdcDuplicateEntity.getDuplicateSeverityCode().equals(DuplicateSeverityCode.ALLOWABLE.getCode()) || sdcDuplicateEntity.getDuplicateTypeCode().equals(DuplicateTypeCode.PROGRAM.getCode()));
            programAndAllowableDupes.forEach(allowable -> {
              if (existingStudentDuplicates.stream().map(SdcDuplicateEntity::getUniqueObjectHash).noneMatch(duplicateHash -> duplicateHash == allowable.getUniqueObjectHash())) {
                sdcDuplicateRepository.save(allowable);
              }
            });
          }
        } else if(dupe.getDuplicateTypeCode().equals(DuplicateTypeCode.PROGRAM.getCode()) && studentDupes.stream().map(SdcDuplicateEntity::getUniqueObjectHash).noneMatch(duplicateHash -> duplicateHash == dupe.getUniqueObjectHash())) {
          dupe.setDuplicateResolutionCode(DuplicateResolutionCode.RESOLVED.getCode());
          dupe.setRetainedSdcSchoolCollectionStudentEntity(otherStudent.get().getSdcSchoolCollectionStudentEntity());
          sdcDuplicateRepository.save(dupe);
        }
      }
    });
  }

  public Map<UUID, SdcDuplicatesByInstituteID> getInFlightProvincialDuplicates(UUID collectionID, boolean isIndySchoolView) {
    Optional<CollectionEntity> activeCollection = collectionRepository.findActiveCollection();

    if (activeCollection.isPresent()) {
      if (!activeCollection.get().getCollectionID().equals(collectionID)) {
        throw new InvalidParameterException(COLLECTION_ID_NOT_ACTIVE_MSG);
      } else if (isCollectionInProvDupes(activeCollection.get())) {
        throw new InvalidParameterException(COLLECTION_DUPLICATES_ALREADY_RUN_MSG);
      }
    } else {
      throw new InvalidParameterException(COLLECTION_ID_NOT_ACTIVE_MSG);
    }
    List<SdcSchoolCollectionStudentLightEntity> provinceDupes = sdcSchoolCollectionStudentRepository.findAllInProvinceDuplicateStudentsInCollection(collectionID);
    List<SdcDuplicateEntity> generatedDuplicates = generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL, false);
    generatedDuplicates.removeIf(duplicate -> duplicate.getDuplicateSeverityCode().equals(DuplicateSeverityCode.ALLOWABLE.getCode()));

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
      }else if(isCollectionInProvDupes(activeCollection.get())){
        throw new InvalidParameterException(COLLECTION_DUPLICATES_ALREADY_RUN_MSG);
      }
    }

    List<SdcSchoolCollectionStudentLightEntity> provinceDupes = sdcSchoolCollectionStudentRepository.findAllInProvinceDuplicateStudentsInCollection(collectionID);

    List<SdcDuplicateEntity> finalDuplicatesSet =  generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL, false);
    sdcDuplicateRepository.saveAll(finalDuplicatesSet);

    activeCollection.ifPresent(collectionEntity -> sendEmailNotificationsForProvinceDuplicates(finalDuplicatesSet, formatter.format(collectionEntity.getDuplicationResolutionDueDate())));

    this.collectionRepository.updateCollectionStatus(collectionID, String.valueOf(CollectionStatus.PROVDUPES));

    List<SdcSchoolCollectionEntity> schoolCollectionsWithoutDupes = sdcSchoolCollectionRepository.findAllSchoolCollectionsWithoutProvincialDupes(collectionID);
    List<SdcSchoolCollectionEntity> schoolCollectionsWithDupes = sdcSchoolCollectionRepository.findAllSchoolCollectionsWithProvincialDupes(collectionID);

    sdcSchoolCollectionService.updateSchoolCollectionStatuses(schoolCollectionsWithoutDupes, SdcSchoolCollectionStatus.COMPLETED.getCode());
    sdcSchoolCollectionService.updateSchoolCollectionStatuses(schoolCollectionsWithDupes, SdcSchoolCollectionStatus.P_DUP_POST.getCode());

    sdcDistrictCollectionRepository.updateAllDistrictCollectionStatus(collectionID, String.valueOf(SdcDistrictCollectionStatus.P_DUP_POST));
  }

  public List<SdcDuplicateEntity> generateFinalDuplicatesSet(List<SdcSchoolCollectionStudentLightEntity> duplicateStudentEntities, DuplicateLevelCode duplicateLevelCode, Boolean isTrickle) {
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

  private void sendEmailNotificationsForProvinceDuplicates(List<SdcDuplicateEntity> finalDuplicatesSet, String dueDate){
    Map<UUID, SdcSchoolCollection1701Users> emailList = generateEmailListForProvinceDuplicates(finalDuplicatesSet);
    if(!emailList.isEmpty()){
      scheduleHandlerService.createAndStartProvinceDuplicateEmailSagas(emailList, dueDate);
    }
  }

  public Map<UUID, SdcSchoolCollection1701Users> generateEmailListForProvinceDuplicates(List<SdcDuplicateEntity> sdcDuplicateEntities){
    final HashMap<UUID, SdcSchoolCollection1701Users> schoolAndDistrict1701Emails = new HashMap<>();

    var sdcSchoolCollectionIDs = new ArrayList<UUID>();

    sdcDuplicateEntities.forEach(provinceDupe -> provinceDupe.getSdcDuplicateStudentEntities().forEach(student ->  sdcSchoolCollectionIDs.add(student.getSdcSchoolCollectionID())));

    List<SchoolCollectionSchoolID> schoolCollectionSchoolIDS = sdcSchoolCollectionRepository.findSchoolIDBySdcSchoolCollectionIDIn(sdcSchoolCollectionIDs);

    schoolCollectionSchoolIDS.forEach(schoolCollectionSchoolID -> {
      if (!schoolAndDistrict1701Emails.containsKey(schoolCollectionSchoolID.getSdcSchoolCollectionID())){
        UUID schoolID = schoolCollectionSchoolID.getSchoolID();
        UUID districtID = getDistrictID(schoolID);

        List<EdxUser> schoolEdxUsers = restUtils.getEdxUsersForSchool(schoolID);
        List<EdxUser> districtEdxUsers = restUtils.getEdxUsersForDistrict(districtID);
        Set<String> emails = pluckEmailAddressesFromSchool(schoolEdxUsers);
        emails.addAll(pluckEmailAddressesFromDistrict(districtEdxUsers));

        SdcSchoolCollection1701Users schoolAndDistrictUsers = new SdcSchoolCollection1701Users();

        schoolAndDistrictUsers.setSchoolDisplayName(getSchoolName(schoolID));
        schoolAndDistrictUsers.setEmails(emails);

        schoolAndDistrict1701Emails.put(schoolCollectionSchoolID.getSdcSchoolCollectionID(), schoolAndDistrictUsers);
      }
    });

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

  public Set<String> pluckEmailAddressesFromSchool(List<EdxUser> edxUsers){
    final Set<String> emailSet = new HashSet<>();
    edxUsers.forEach(user ->
      user.getEdxUserSchools().forEach(school ->
        school.getEdxUserSchoolRoles().forEach(role -> {
          if (Objects.equals(role.getEdxRoleCode(), "SCHOOL_SDC")) {
            emailSet.add(user.getEmail());
          }
    })));
    return emailSet;
  }

  public Set<String> pluckEmailAddressesFromDistrict(List<EdxUser> edxUsers){
    final Set<String> emailSet = new HashSet<>();
    edxUsers.forEach(user ->
      user.getEdxUserDistricts().forEach(district ->
        district.getEdxUserDistrictRoles().forEach(role -> {
          if (Objects.equals(role.getEdxRoleCode(), "DISTRICT_SDC")) {
            emailSet.add(user.getEmail());
          }
    })));
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

  private List<SdcDuplicateEntity> generateProgramDuplicates(List<SdcDuplicateEntity> newDuplicates, SdcSchoolCollectionStudentLightEntity student1, SdcSchoolCollectionStudentLightEntity student2, DuplicateLevelCode level){
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
    generateProgramDuplicates(newDuplicates,entity1,entity2,levelCode);
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
    student.setSdcSchoolCollectionStudentEntity(studentEntity);
    student.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    student.setCreateDate(LocalDateTime.now());
    student.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    student.setUpdateDate(LocalDateTime.now());
    return student;
  }

}
