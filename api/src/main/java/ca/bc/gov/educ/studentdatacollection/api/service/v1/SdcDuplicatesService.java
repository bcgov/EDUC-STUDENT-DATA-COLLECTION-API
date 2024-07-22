package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateResolutionCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ProgramDuplicateTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class SdcDuplicatesService {

  private final SdcDuplicateRepository sdcDuplicateRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final CollectionRepository collectionRepository;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final ValidationRulesService validationRulesService;
  private final DuplicateClassNumberGenerationService duplicateClassNumberGenerationService;
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;
  private final ScheduleHandlerService scheduleHandlerService;
  private static final SdcSchoolCollectionMapper sdcSchoolCollectionMapper = SdcSchoolCollectionMapper.mapper;
  private static final SdcSchoolCollectionStudentMapper sdcSchoolCollectionStudentMapper = SdcSchoolCollectionStudentMapper.mapper;
  private final RestUtils restUtils;
  private static final String SDC_DUPLICATE_ID_KEY = "sdcDuplicateID";
  private static final List<String> independentSchoolCategoryCodes =Arrays.asList(SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.INDP_FNS.getCode());


  @Autowired
  public SdcDuplicatesService(SdcDuplicateRepository sdcDuplicateRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, ValidationRulesService validationRulesService, ScheduleHandlerService scheduleHandlerService, DuplicateClassNumberGenerationService duplicateClassNumberGenerationService, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService, CollectionRepository collectionRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
      this.sdcDuplicateRepository = sdcDuplicateRepository;
      this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
      this.collectionRepository = collectionRepository;
      this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
      this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
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

    List<SdcDuplicateEntity> finalDuplicatesSet = generateFinalDuplicatesSet(duplicateStudentEntities, DuplicateLevelCode.IN_DIST);

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

  public SdcDuplicateEntity getSdcDuplicate(UUID sdcDuplicateID) {
    Optional<SdcDuplicateEntity> sdcDuplicateEntity = sdcDuplicateRepository.findById(sdcDuplicateID);

    return sdcDuplicateEntity.orElseThrow(() -> new EntityNotFoundException(SdcDuplicateEntity.class, SDC_DUPLICATE_ID_KEY, sdcDuplicateID.toString()));
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public SdcDuplicateEntity updateStudentAndResolveDuplicates(UUID sdcDuplicateID, List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudent) {
    final Optional<SdcDuplicateEntity> curSdcDuplicateEntity = sdcDuplicateRepository.findBySdcDuplicateID(sdcDuplicateID);
    if (curSdcDuplicateEntity.isPresent()) {
      SdcDuplicateEntity curGetSdcDuplicateEntity = curSdcDuplicateEntity.get();
      // update student
      sdcSchoolCollectionStudent.forEach(student -> {
        RequestUtil.setAuditColumnsForUpdate(student);
        SdcSchoolCollectionStudentEntity updatedStudentEntity = sdcSchoolCollectionStudentService.updateSdcSchoolCollectionStudent(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentEntity(student), true);
        // There might only be one student to update, but we need both for dup check, so get both added
        curGetSdcDuplicateEntity.getSdcDuplicateStudentEntities().stream()
                .filter(duplicateStudent -> duplicateStudent.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().equals(updatedStudentEntity.getSdcSchoolCollectionStudentID()))
                .forEach(duplicateStudent -> duplicateStudent.setSdcSchoolCollectionStudentEntity(updatedStudentEntity));
      });
      var updatedStudents = curGetSdcDuplicateEntity.getSdcDuplicateStudentEntities().stream().map(SdcDuplicateStudentEntity::getSdcSchoolCollectionStudentEntity).toList();
      if (curGetSdcDuplicateEntity.getSdcDuplicateStudentEntities().stream().map(SdcDuplicateStudentEntity::getSdcSchoolCollectionStudentEntity).noneMatch(student -> student.getSdcSchoolCollectionStudentStatusCode().equalsIgnoreCase(StudentValidationIssueSeverityCode.ERROR.toString()))) {
        //re-run duplicates
        List<SdcDuplicateEntity> listOfDuplicates = runDuplicatesCheck(DuplicateLevelCode.valueOf(curGetSdcDuplicateEntity.getDuplicateLevelCode()), updatedStudents.get(0), updatedStudents.get(1));
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
  public SdcDuplicateEntity changeGrade(UUID sdcDuplicateID, SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
    final Optional<SdcDuplicateEntity> curSdcDuplicateEntity = sdcDuplicateRepository.findBySdcDuplicateID(sdcDuplicateID);

    if (curSdcDuplicateEntity.isPresent()) {
      SdcDuplicateEntity curGetSdcDuplicateEntity = curSdcDuplicateEntity.get();

      // update student
      RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollectionStudent);
      SdcSchoolCollectionStudentEntity updatedStudent = sdcSchoolCollectionStudentService.updateSdcSchoolCollectionStudent(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent), false);

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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void generateAllProvincialDuplicates(UUID collectionID){
    Optional<CollectionEntity> activeCollection = collectionRepository.findActiveCollection();

    if(activeCollection.isPresent()){
      if(!activeCollection.get().getCollectionID().equals(collectionID)) {
        throw new InvalidParameterException("Provided collectionID does not match currently active collectionID.");
      }else if(activeCollection.get().getCollectionStatusCode().equals(CollectionStatus.PROVDUPES.getCode())){
        throw new InvalidParameterException("Provided collectionID has already run provincial duplicates.");
      }
    }

    List<SdcSchoolCollectionStudentEntity> provinceDupes = sdcSchoolCollectionStudentRepository.findAllInProvinceDuplicateStudentsInCollection(collectionID);

    List<SdcDuplicateEntity> finalDuplicatesSet =  generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL);
    sdcDuplicateRepository.saveAll(finalDuplicatesSet);

    sendEmailNotificationsForProvinceDuplicates();

    this.collectionRepository.updateCollectionStatus(collectionID, String.valueOf(CollectionStatus.PROVDUPES));
    sdcSchoolCollectionRepository.updateAllSchoolCollectionStatus(collectionID, String.valueOf(SdcSchoolCollectionStatus.P_DUP_POST));
    sdcDistrictCollectionRepository.updateAllDistrictCollectionStatus(collectionID, String.valueOf(SdcDistrictCollectionStatus.P_DUP_POST));
  }

  private List<SdcDuplicateEntity> generateFinalDuplicatesSet(List<SdcSchoolCollectionStudentEntity> duplicateStudentEntities, DuplicateLevelCode duplicateLevelCode){
    HashMap<UUID, List<SdcSchoolCollectionStudentEntity>> groupedDups = new HashMap<>();

    duplicateStudentEntities.forEach(student -> {
      if (!groupedDups.containsKey(student.getAssignedStudentId())) {
        groupedDups.put(student.getAssignedStudentId(), new ArrayList<>(List.of(student)));
      } else {
        groupedDups.get(student.getAssignedStudentId()).add(student);
      }
    });

    List<SdcDuplicateEntity> finalDuplicatesSet = new ArrayList<>();

    for (List<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntities : groupedDups.values()){
      for (SdcSchoolCollectionStudentEntity entity1 : sdcSchoolCollectionStudentEntities) {
        for (SdcSchoolCollectionStudentEntity entity2 : sdcSchoolCollectionStudentEntities) {
          if (!entity1.getSdcSchoolCollectionStudentID().equals(entity2.getSdcSchoolCollectionStudentID())) {
            List<SdcDuplicateEntity> duplicateRecords = runDuplicatesCheck(duplicateLevelCode, entity1, entity2);
            var shrunkDuplicates = removeBiDirectionalDuplicatesFromFoundDups(finalDuplicatesSet, duplicateRecords);
            finalDuplicatesSet.addAll(shrunkDuplicates);
          }
        }
      }
    }

    return finalDuplicatesSet;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void resolveRemainingDuplicates(UUID collectionID){
    Optional<CollectionEntity> activeCollection = collectionRepository.findActiveCollection();

    if(activeCollection.isPresent()){
      if(!activeCollection.get().getCollectionID().equals(collectionID)) {
        throw new InvalidParameterException("Provided collectionID does not match currently active collectionID.");
      }else if(activeCollection.get().getCollectionStatusCode().equals(CollectionStatus.INPROGRESS.getCode())){
        throw new InvalidParameterException("Provided collectionID has not yet run provincial duplicates.");
      }else if(activeCollection.get().getCollectionStatusCode().equals(CollectionStatus.DUPES_RES.getCode())){
        throw new InvalidParameterException("Provided collectionID has already resolved all duplicates.");
      }
    }

    resolveEnrollmentDuplicates();

    // Generate new program dupes
    List<SdcSchoolCollectionStudentEntity> provinceDupes = sdcSchoolCollectionStudentRepository.findAllInProvinceDuplicateStudentsInCollection(collectionID);
    List<SdcDuplicateEntity> finalDuplicatesSet =  generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL);
    sdcDuplicateRepository.saveAll(finalDuplicatesSet);

    resolveProgramDuplicates();

    collectionRepository.updateCollectionStatus(collectionID, CollectionStatus.DUPES_RES.getCode());
    sdcSchoolCollectionRepository.updateCollectionsToCompleted(collectionID);
  }


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
        sdcSchoolCollectionStudentRepository.save(updatedStudent);

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
    } else if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.INDIGENOUS.getCode())){
      List<String> studentIndigenousProgramCodes = EnrolledProgramCodes.getIndigenousProgramCodes().stream().filter(enrolledPrograms::contains).toList();
      enrolledPrograms.removeAll(studentIndigenousProgramCodes);
    } else if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.LANGUAGE.getCode())){
      List<String> studentLanguageProgramCodes = EnrolledProgramCodes.getFrenchProgramCodesWithEll().stream().filter(enrolledPrograms::contains).toList();
      enrolledPrograms.removeAll(studentLanguageProgramCodes);
    } else if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.SPECIAL_ED.getCode())){
      enrolledPrograms.remove(student.getSpecialEducationCategoryCode());
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
  public void sendEmailNotificationsForProvinceDuplicates(){
    Map<UUID, SdcSchoolCollection1701Users> emailList = generateEmailListForProvinceDuplicates();
    if(!emailList.isEmpty()){
      scheduleHandlerService.createAndStartProvinceDuplicateEmailSagas(emailList);
    }
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

  private List<SdcDuplicateEntity> removeBiDirectionalDuplicatesFromFoundDups(List<SdcDuplicateEntity> existingDuplicates, List<SdcDuplicateEntity> foundDuplicates){
    List<Integer> existingDuplicatesHash = existingDuplicates.stream().map(SdcDuplicateEntity::getUniqueObjectHash).toList();
    HashMap<Integer, SdcDuplicateEntity> foundDupsMap = new HashMap<>();
    foundDuplicates.forEach(entry -> foundDupsMap.put(entry.getUniqueObjectHash(), entry));
    List<SdcDuplicateEntity> finalDups = new ArrayList<>();

    foundDupsMap.keySet().forEach(dupHash -> {
      if(!existingDuplicatesHash.contains(dupHash)){
        finalDups.add(foundDupsMap.get(dupHash));
      }
    });
    return finalDups;
  }


  public List<SdcDuplicateEntity> runDuplicatesCheck(DuplicateLevelCode level, SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2){
    List<SdcDuplicateEntity> dups = new ArrayList<>();
    SchoolTombstone schoolTombstone1 = restUtils.getSchoolBySchoolID(entity1.getSdcSchoolCollection().getSchoolID().toString()).orElseThrow(() ->
            new StudentDataCollectionAPIRuntimeException("School provided by ID " + entity1.getSdcSchoolCollection().getSchoolID() + "was not found - this is not expected"));
    SchoolTombstone schoolTombstone2 = restUtils.getSchoolBySchoolID(entity2.getSdcSchoolCollection().getSchoolID().toString()).orElseThrow(() ->
            new StudentDataCollectionAPIRuntimeException("School provided by ID " + entity2.getSdcSchoolCollection().getSchoolID() + "was not found - this is not expected"));
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
    if(dups.isEmpty() && SchoolGradeCodes.getKToNineGrades().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getKToNineGrades().contains(entity2.getEnrolledGradeCode())){
      addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.K_TO_9_DUP);
    }

    //In which grades are the two records reported - K-7 & 10-12,SU Check
    if(dups.isEmpty() && ((SchoolGradeCodes.getKToSevenEuGrades().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode())) ||
            (SchoolGradeCodes.getKToSevenEuGrades().contains(entity2.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode())))){
      addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.K_TO_7_DUP);
    }

    //In which grades are the two records reported - 10,11,12,SU Check
    var isSchool1Independent = SchoolCategoryCodes.INDEPEND.getCode().equals(schoolTombstone1.getSchoolCategoryCode()) || SchoolCategoryCodes.INDP_FNS.getCode().equals(schoolTombstone1.getSchoolCategoryCode());
    var isSchool2Independent = SchoolCategoryCodes.INDEPEND.getCode().equals(schoolTombstone2.getSchoolCategoryCode()) || SchoolCategoryCodes.INDP_FNS.getCode().equals(schoolTombstone2.getSchoolCategoryCode());
    if(dups.isEmpty() && SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode())){
      if((FacilityTypeCodes.DIST_LEARN.getCode().equals(schoolTombstone1.getFacilityTypeCode()) && SchoolCategoryCodes.INDEPEND.getCode().equals(schoolTombstone1.getSchoolCategoryCode())) ||
              (FacilityTypeCodes.DIST_LEARN.getCode().equals(schoolTombstone2.getFacilityTypeCode()) && SchoolCategoryCodes.INDEPEND.getCode().equals(schoolTombstone2.getSchoolCategoryCode()))) {
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else if(isSchool1Independent || isSchool2Independent) {
        if((isSchool1Independent && FacilityTypeCodes.DIST_LEARN.getCode().equals(schoolTombstone2.getFacilityTypeCode())) || (isSchool2Independent && FacilityTypeCodes.DIST_LEARN.getCode().equals(schoolTombstone1.getFacilityTypeCode()))) {
          addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
        }else{
          addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.ALT_DUP);
        }
      }else if(schoolTombstone1.getDistrictId().equals(schoolTombstone2.getDistrictId())){
        if(FacilityTypeCodes.ALT_PROGS.getCode().equals(schoolTombstone1.getFacilityTypeCode()) || FacilityTypeCodes.ALT_PROGS.getCode().equals(schoolTombstone2.getFacilityTypeCode())){
          addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.ALT_DUP);
        }else{
          addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
        }
      }else if(FacilityTypeCodes.DIST_LEARN.getCode().equals(schoolTombstone1.getFacilityTypeCode()) || FacilityTypeCodes.DIST_LEARN.getCode().equals(schoolTombstone2.getFacilityTypeCode())){
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else{
        addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.ALT_DUP);
      }
    }

    //In which grades are the two records reported - 8&9, 10-12,SU Check
    var isStudent1Grade8or9 = SchoolGradeCodes.getGrades8and9().contains(entity1.getEnrolledGradeCode());
    var isStudent2Grade8or9 = SchoolGradeCodes.getGrades8and9().contains(entity2.getEnrolledGradeCode());
    var isStudent1Grade10toSU = SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode());
    var isStudent2Grade10toSU = SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode());

    if(dups.isEmpty() && ((isStudent1Grade8or9 && isStudent2Grade10toSU) || (isStudent2Grade8or9 && isStudent1Grade10toSU))){
      if(FacilityTypeCodes.DIST_LEARN.getCode().equals(schoolTombstone1.getFacilityTypeCode()) && FacilityTypeCodes.DIST_LEARN.getCode().equals(schoolTombstone2.getFacilityTypeCode())) {
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else if((isStudent1Grade8or9 && FacilityTypeCodes.STANDARD.getCode().equals(schoolTombstone1.getFacilityTypeCode()) && isStudent2Grade10toSU && FacilityTypeCodes.DIST_LEARN.getCode().equals(schoolTombstone2.getFacilityTypeCode())) ||
              (isStudent2Grade8or9 && FacilityTypeCodes.STANDARD.getCode().equals(schoolTombstone2.getFacilityTypeCode()) && isStudent1Grade10toSU && FacilityTypeCodes.DIST_LEARN.getCode().equals(schoolTombstone1.getFacilityTypeCode()))){
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else {
        addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null, DuplicateErrorDescriptionCode.IN_8_9_DUP);
      }
    }

    return dups;
  }

  private List<SdcDuplicateEntity> runProgramDuplicates(List<SdcDuplicateEntity> newDuplicates, SdcSchoolCollectionStudentEntity student1, SdcSchoolCollectionStudentEntity student2, DuplicateLevelCode level){
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
        if(student1CareerPrograms.stream().anyMatch(student2CareerPrograms::contains)){
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.CAREER,null));
        }
      }
    }

    return newDuplicates;
  }

  private void addAllowableDuplicateWithProgramDups(List<SdcDuplicateEntity> newDuplicates, DuplicateLevelCode levelCode, SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2, DuplicateTypeCode typeCode, ProgramDuplicateTypeCode programDuplicateTypeCode){
    newDuplicates.add(generateDuplicateEntity(levelCode,entity1,entity2,typeCode,DuplicateSeverityCode.ALLOWABLE,programDuplicateTypeCode,null));
    runProgramDuplicates(newDuplicates,entity1,entity2,levelCode);
  }

  private void addNonAllowableDuplicate(List<SdcDuplicateEntity> newDuplicates, DuplicateLevelCode levelCode, SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2, DuplicateTypeCode typeCode, ProgramDuplicateTypeCode programDuplicateTypeCode, DuplicateErrorDescriptionCode duplicateErrorDescriptionCode){
    newDuplicates.add(generateDuplicateEntity(levelCode,entity1,entity2,typeCode,DuplicateSeverityCode.NON_ALLOWABLE,programDuplicateTypeCode,duplicateErrorDescriptionCode));
  }

  private SdcDuplicateEntity generateDuplicateEntity(DuplicateLevelCode levelCode, SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2, DuplicateTypeCode typeCode, DuplicateSeverityCode severityCode, ProgramDuplicateTypeCode programDuplicateTypeCode, DuplicateErrorDescriptionCode duplicateErrorDescriptionCode){
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
    dupe.setCollectionID(entity1.getSdcSchoolCollection().getCollectionEntity().getCollectionID());

    List<SdcDuplicateStudentEntity> studs = new ArrayList<>();
    studs.add(createSdcDuplicateStudent(entity1, dupe));
    studs.add(createSdcDuplicateStudent(entity2, dupe));
    dupe.getSdcDuplicateStudentEntities().addAll(studs);

    return dupe;
  }

  private SdcDuplicateStudentEntity createSdcDuplicateStudent(SdcSchoolCollectionStudentEntity studentEntity, SdcDuplicateEntity dupe){
    SdcDuplicateStudentEntity student = new SdcDuplicateStudentEntity();
    student.setSdcDistrictCollectionID(studentEntity.getSdcSchoolCollection().getSdcDistrictCollectionID());
    student.setSdcSchoolCollectionID(studentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID());
    student.setSdcDuplicateEntity(dupe);
    student.setSdcSchoolCollectionStudentEntity(studentEntity);
    student.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    student.setCreateDate(LocalDateTime.now());
    student.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    student.setUpdateDate(LocalDateTime.now());
    return student;
  }

}
