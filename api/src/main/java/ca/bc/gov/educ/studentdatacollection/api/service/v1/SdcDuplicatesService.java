package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EdxUser;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolCollectionSchoolID;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection1701Users;
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
  private final SdcSchoolCollectionHistoryRepository sdcSchoolHistoryRepository;
  private static final SdcSchoolCollectionStudentMapper sdcSchoolCollectionStudentMapper = SdcSchoolCollectionStudentMapper.mapper;
  private final RestUtils restUtils;
  private static final String SDC_DUPLICATE_ID_KEY = "sdcDuplicateID";
  private static final String COLLECTION_ID_NOT_ACTIVE_MSG = "Provided collectionID does not match currently active collectionID.";
  private static final String COLLECTION_DUPLICATES_ALREADY_RUN_MSG = "Provided collectionID has already run provincial duplicates.";
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<SdcDuplicateEntity> getAllInDistrictCollectionDuplicates(UUID sdcDistrictCollectionID) {
    var duplicateStudentEntities = sdcSchoolCollectionStudentRepository.findAllInDistrictDuplicateStudentsInSdcDistrictCollection(sdcDistrictCollectionID);

    return generateFinalDuplicatesSet(duplicateStudentEntities, DuplicateLevelCode.IN_DIST, false);
  }

  public List<SdcSchoolCollectionStudentEntity> getAllSchoolCollectionDuplicates(UUID sdcSchoolCollectionID) {
    return sdcSchoolCollectionStudentRepository.findAllDuplicateStudentsInSdcSchoolCollection(sdcSchoolCollectionID);
  }

  public List<SdcDuplicateEntity> getAllProvincialDuplicatesByCollectionID(UUID collectionID) {
    List<SdcSchoolCollectionStudentLightEntity> provinceDupes = sdcSchoolCollectionStudentRepository.findAllInProvinceDuplicateStudentsInCollection(collectionID);

    return generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL, false);
  }

  public List<SdcDuplicateEntity> getAllProvincialDuplicatesBySdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
    var districtCollection = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID).orElseThrow(() -> new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionEntity", sdcDistrictCollectionID.toString()));
    List<SdcSchoolCollectionStudentLightEntity> provinceDupes = sdcSchoolCollectionStudentRepository.findAllInProvinceDuplicateStudentsInSdcDistrictCollection(districtCollection.getCollectionEntity().getCollectionID(), sdcDistrictCollectionID);

    var dupes = generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL, false);

    var finalSet = new HashSet<SdcDuplicateEntity>();
    dupes.forEach(dupe -> {
      dupe.getSdcDuplicateStudentEntities().forEach(stud -> {
        if(stud.getSdcDistrictCollectionID() != null && stud.getSdcDistrictCollectionID().equals(sdcDistrictCollectionID)) {
          finalSet.add(dupe);
        }
      });
    });

    return finalSet.stream().toList();
  }

  public List<SdcDuplicateEntity> getAllProvincialDuplicatesBySdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
    var schoolCollection = sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID).orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionEntity.class, "sdcSchoolCollectionEntity", sdcSchoolCollectionID.toString()));
    List<SdcSchoolCollectionStudentLightEntity> provinceDupes = sdcSchoolCollectionStudentRepository.findAllInProvinceDuplicateStudentsInSdcSchoolCollection(schoolCollection.getCollectionEntity().getCollectionID(), sdcSchoolCollectionID);

    var dupes = generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL, false);

    var finalSet = new HashSet<SdcDuplicateEntity>();
    dupes.forEach(dupe -> {
      dupe.getSdcDuplicateStudentEntities().forEach(stud -> {
        if(stud.getSdcSchoolCollectionID().equals(sdcSchoolCollectionID)) {
          finalSet.add(dupe);
        }
      });
    });

    return finalSet.stream().toList();
  }

  public SdcDuplicateEntity getSdcDuplicate(UUID sdcDuplicateID) {
    Optional<SdcDuplicateEntity> sdcDuplicateEntity = sdcDuplicateRepository.findById(sdcDuplicateID);

    return sdcDuplicateEntity.orElseThrow(() -> new EntityNotFoundException(SdcDuplicateEntity.class, SDC_DUPLICATE_ID_KEY, sdcDuplicateID.toString()));
  }

  public void checkIfDuplicateIsGeneratedAndThrow(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity, boolean isCollectionInProvDupes){
    if(isCollectionInProvDupes) {
      List<SdcSchoolCollectionStudentEntity> allStudentsWithSameAssignedStudentId = sdcSchoolCollectionStudentRepository
              .findAllDuplicateStudentsByCollectionID(sdcSchoolCollectionStudentEntity.getSdcSchoolCollection().getCollectionEntity().getCollectionID(), Collections.singletonList(sdcSchoolCollectionStudentEntity.getAssignedStudentId()));

      //filter out the non-updated entity fetched from db
      List<SdcSchoolCollectionStudentEntity> filteredStudentsWithSameAssignedStudentId = allStudentsWithSameAssignedStudentId.stream().filter(student -> !student.getSdcSchoolCollectionStudentID().equals(sdcSchoolCollectionStudentEntity.getSdcSchoolCollectionStudentID())).toList();

      //map to light object
      List<SdcSchoolCollectionStudentLightEntity> duplicateStudentEntities = new ArrayList<>(filteredStudentsWithSameAssignedStudentId.stream().map(sdcSchoolCollectionStudentMapper::toSdcSchoolStudentLightEntity).toList());
      duplicateStudentEntities.add(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentLightEntity(sdcSchoolCollectionStudentEntity));

      //Generate new PROV dupes
      List<SdcDuplicateEntity> generatedDuplicates = generateFinalDuplicatesSet(duplicateStudentEntities, DuplicateLevelCode.PROVINCIAL, false);

      Optional<SdcDuplicateEntity> duplicateWithStudentID = generatedDuplicates.stream().filter(dupe -> dupe.getSdcDuplicateStudentEntities()
              .stream().anyMatch(stu -> stu.getSdcSchoolCollectionStudentEntity().getCurrentDemogHash().equals(sdcSchoolCollectionStudentEntity.getCurrentDemogHash()))).findFirst();

      if (duplicateWithStudentID.isPresent()) {
        log.debug("SdcSchoolCollectionStudent was not saved to the database because it would create a duplicate on save :: {}", sdcSchoolCollectionStudentEntity.getAssignedStudentId());
        throw new InvalidPayloadException(createDuplicatesThrow(sdcSchoolCollectionStudentEntity.getAssignedPen()));
      }
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<SdcSagaEntity> setupRequiredDuplicateEmailSagas(UUID collectionID){
    Optional<CollectionEntity> activeCollection = collectionRepository.findActiveCollection();

    if(activeCollection.isPresent()){
      if(!activeCollection.get().getCollectionID().equals(collectionID)) {
        throw new InvalidParameterException(COLLECTION_ID_NOT_ACTIVE_MSG);
      }else if(isCollectionInProvDupes(activeCollection.get())){
        throw new InvalidParameterException(COLLECTION_DUPLICATES_ALREADY_RUN_MSG);
      }
    }else{
      throw new InvalidParameterException(COLLECTION_ID_NOT_ACTIVE_MSG);
    }

    List<SdcDuplicateEntity> finalDuplicatesSet =  sdcDuplicateRepository.findAllByCollectionID(collectionID);

    return sendEmailNotificationsForProvinceDuplicates(finalDuplicatesSet, formatter.format(activeCollection.get().getDuplicationResolutionDueDate()));
  }

  public void startCreatedEmailSagas(List<SdcSagaEntity> savedSagas){
    scheduleHandlerService.startCreatedEmailSagas(savedSagas);
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

    this.collectionRepository.updateCollectionStatus(collectionID, String.valueOf(CollectionStatus.PROVDUPES));

    List<UUID> schoolCollectionsWithoutDupes = sdcSchoolCollectionRepository.findAllSchoolCollectionsWithoutProvincialDupes(collectionID)
            .stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList();
    List<UUID> schoolCollectionsWithDupes = sdcSchoolCollectionRepository.findAllSchoolCollectionsWithProvincialDupes(collectionID)
            .stream().map(SdcSchoolCollectionEntity::getSdcSchoolCollectionID).toList();

    //update school collection status
    sdcSchoolCollectionRepository.updateSchoolCollectionStatus(schoolCollectionsWithoutDupes, SdcSchoolCollectionStatus.COMPLETED.getCode());
    sdcSchoolCollectionRepository.updateSchoolCollectionStatus(schoolCollectionsWithDupes, SdcSchoolCollectionStatus.P_DUP_POST.getCode());

    //update school collection history
    sdcSchoolHistoryRepository.updateSchoolCollectionStatus(schoolCollectionsWithoutDupes, SdcSchoolCollectionStatus.COMPLETED.getCode());
    sdcSchoolHistoryRepository.updateSchoolCollectionStatus(schoolCollectionsWithDupes, SdcSchoolCollectionStatus.P_DUP_POST.getCode());

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

  private List<SdcSagaEntity> sendEmailNotificationsForProvinceDuplicates(List<SdcDuplicateEntity> finalDuplicatesSet, String dueDate){
    Map<UUID, SdcSchoolCollection1701Users> emailList = generateEmailListForProvinceDuplicates(finalDuplicatesSet);
    if(!emailList.isEmpty()){
      return scheduleHandlerService.createAndStartProvinceDuplicateEmailSagas(emailList, dueDate);
    }
    return new ArrayList<>();
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
      generateProgramDuplicates(dups,entity1,entity2,level);
      return dups;
    }

    //In which grades are the two records reported - K-9 Check
    if(!isTrickle && SchoolGradeCodes.getKToNineGrades().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getKToNineGrades().contains(entity2.getEnrolledGradeCode())){
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
        generateProgramDuplicates(dups,entity1,entity2,level);
      }else if(isSchool1Independent || isSchool2Independent) {
        if((facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode())) || (isSchool2Independent && facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()))) {
          generateProgramDuplicates(dups,entity1,entity2,level);
        }else if(!isTrickle){
          addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.NON_ALTDUP);
        }
      }else if(schoolTombstone1.getDistrictId().equals(schoolTombstone2.getDistrictId())){
        if(!isTrickle && (FacilityTypeCodes.ALT_PROGS.getCode().equals(schoolTombstone1.getFacilityTypeCode()) && !facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode())) || (FacilityTypeCodes.ALT_PROGS.getCode().equals(schoolTombstone2.getFacilityTypeCode()) && !facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()))){
          addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.ALT_DUP);
        }else{
          generateProgramDuplicates(dups,entity1,entity2,level);
        }
      } else if ((!facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()) && !facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode())) && !isTrickle){
        addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.ALT_DUP);
      } else {
        generateProgramDuplicates(dups,entity1,entity2,level);
      }
    }

    //In which grades are the two records reported - 8&9, 10-12,SU Check
    var isStudent1Grade8or9 = SchoolGradeCodes.getGrades8and9().contains(entity1.getEnrolledGradeCode());
    var isStudent2Grade8or9 = SchoolGradeCodes.getGrades8and9().contains(entity2.getEnrolledGradeCode());
    var isStudent1Grade10toSU = SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode());
    var isStudent2Grade10toSU = SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode());

    if(dups.isEmpty() && ((isStudent1Grade8or9 && isStudent2Grade10toSU) || (isStudent2Grade8or9 && isStudent1Grade10toSU))){
      if(facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()) && facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode())) {
        generateProgramDuplicates(dups,entity1,entity2,level);
      } else if((isStudent2Grade10toSU && facilityOnlineCodes.contains(schoolTombstone2.getFacilityTypeCode())) ||
              (isStudent1Grade10toSU && facilityOnlineCodes.contains(schoolTombstone1.getFacilityTypeCode()))){
        generateProgramDuplicates(dups,entity1,entity2,level);
      } else if(!isTrickle && level.equals(DuplicateLevelCode.IN_DIST)){
        addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.IN_8_9_DUP);
      } else {
        addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.IN_8_9_DUP);
        generateProgramDuplicates(dups,entity1,entity2,level);
      }
    }

    return dups;
  }

  private List<SdcDuplicateEntity> generateProgramDuplicates(List<SdcDuplicateEntity> newDuplicates, SdcSchoolCollectionStudentLightEntity student1, SdcSchoolCollectionStudentLightEntity student2, DuplicateLevelCode level) {
    List<String> student1Programs = validationRulesService.splitEnrolledProgramsString(student1.getEnrolledProgramCodes());
    List<String> student2Programs = validationRulesService.splitEnrolledProgramsString(student2.getEnrolledProgramCodes());

    if (StringUtils.isNotBlank(student1.getCareerProgramCode()) && StringUtils.isNotBlank(student2.getCareerProgramCode()) && student1.getCareerProgramCode().equals(student2.getCareerProgramCode())) {
      newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.CAREER, null));
    }

    if (StringUtils.isNotBlank(student1.getSpecialEducationCategoryCode()) && StringUtils.isNotBlank(student2.getSpecialEducationCategoryCode())) {
      newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.SPECIAL_ED, null));
    }

    List<String> student1IndigenousPrograms = EnrolledProgramCodes.getIndigenousProgramCodes().stream().filter(student1Programs::contains).toList();
    List<String> student2IndigenousPrograms = EnrolledProgramCodes.getIndigenousProgramCodes().stream().filter(student2Programs::contains).toList();
    if (student1IndigenousPrograms.stream().anyMatch(student2IndigenousPrograms::contains)) {
      newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.INDIGENOUS, null));
    }

    List<String> student1LanguagePrograms = EnrolledProgramCodes.getFrenchProgramCodesWithEll().stream().filter(student1Programs::contains).toList();
    List<String> student2LanguagePrograms = EnrolledProgramCodes.getFrenchProgramCodesWithEll().stream().filter(student2Programs::contains).toList();
    if (student1LanguagePrograms.stream().anyMatch(student2LanguagePrograms::contains)) {
      newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.LANGUAGE, null));
    }

    List<String> student1CareerPrograms = EnrolledProgramCodes.getCareerProgramCodes().stream().filter(student1Programs::contains).toList();
    List<String> student2CareerPrograms = EnrolledProgramCodes.getCareerProgramCodes().stream().filter(student2Programs::contains).toList();
    if (student1CareerPrograms.stream().anyMatch(student2CareerPrograms::contains) &&
            newDuplicates.stream().allMatch(dup -> dup.getProgramDuplicateTypeCode() == null || !dup.getProgramDuplicateTypeCode().equals(ProgramDuplicateTypeCode.CAREER.getCode()))) {
      newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.CAREER, null));
    }

    return newDuplicates;
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
