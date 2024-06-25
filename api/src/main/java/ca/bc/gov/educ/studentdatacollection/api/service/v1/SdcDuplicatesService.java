package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDuplicateRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicate;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
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
  private final ValidationRulesService validationRulesService;

  private final RestUtils restUtils;

  @Autowired
  public SdcDuplicatesService(SdcDuplicateRepository sdcDuplicateRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, ValidationRulesService validationRulesService, CollectionRepository collectionRepository, RestUtils restUtils) {
      this.sdcDuplicateRepository = sdcDuplicateRepository;
      this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
      this.collectionRepository = collectionRepository;
      this.validationRulesService = validationRulesService;
      this.restUtils = restUtils;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<SdcDuplicateEntity> getAllInDistrictCollectionDuplicates(UUID sdcDistrictCollectionID) {
    var existingDuplicates = sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcDistrictCollectionID(sdcDistrictCollectionID);
    var duplicateStudentEntities = sdcSchoolCollectionStudentRepository.findAllInDistrictDuplicateStudentsInSdcDistrictCollection(sdcDistrictCollectionID);

    List<SdcDuplicateEntity> finalDuplicatesSet = generateFinalDuplicatesSet(duplicateStudentEntities);

    HashMap<Integer, SdcDuplicateEntity> finalDuplicatesHashMap = new HashMap<>();
    finalDuplicatesSet.stream().forEach(entry -> finalDuplicatesHashMap.put(entry.getUniqueObjectHash(), entry));
    List<SdcDuplicateEntity> dupsToDelete = new ArrayList<>();
    List<SdcDuplicateEntity> finalReturnList = new ArrayList<>();

    existingDuplicates.stream().forEach(dup -> {
      var dupHash = dup.getUniqueObjectHash();
      if(finalDuplicatesHashMap.keySet().contains(dupHash)){
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
    return sdcDuplicateRepository.findAllProvincialDuplicatesByCollectionID(collectionID);
  }

  public List<SdcDuplicateEntity> getAllProvincialDuplicatesBySdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
    return sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcDistrictCollectionIDAndDuplicateLevelCode(sdcDistrictCollectionID, DuplicateLevelCode.PROVINCIAL.getCode());
  }

  public List<SdcDuplicateEntity> getAllProvincialDuplicatesBySdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
    return sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcSchoolCollectionIDAndDuplicateLevelCode(sdcSchoolCollectionID, DuplicateLevelCode.PROVINCIAL.getCode());
  }

  public SdcDuplicateEntity getSdcDuplicate(UUID sdcDuplicateID) {
    Optional<SdcDuplicateEntity> sdcDuplicateEntity = sdcDuplicateRepository.findById(sdcDuplicateID);

    return sdcDuplicateEntity.orElseThrow(() -> new EntityNotFoundException(SdcDuplicateEntity.class, "sdcDuplicateID", sdcDuplicateID.toString()));
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

    List<SdcDuplicateEntity> finalDuplicatesSet =  generateFinalDuplicatesSet(provinceDupes);
    sdcDuplicateRepository.saveAll(finalDuplicatesSet);
  }

  private List<SdcDuplicateEntity> generateFinalDuplicatesSet(List<SdcSchoolCollectionStudentEntity> duplicateStudentEntities){
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
            List<SdcDuplicateEntity> duplicateRecords = runDuplicatesCheck(DuplicateLevelCode.IN_DIST, entity1, entity2);
            var shrunkDuplicates = removeBiDirectionalDuplicatesFromFoundDups(finalDuplicatesSet, duplicateRecords);
            finalDuplicatesSet.addAll(shrunkDuplicates);
          }
        }
      }
    }

    return finalDuplicatesSet;
  }

  private List<SdcDuplicateEntity> removeBiDirectionalDuplicatesFromFoundDups(List<SdcDuplicateEntity> existingDuplicates, List<SdcDuplicateEntity> foundDuplicates){
    List<Integer> existingDuplicatesHash = existingDuplicates.stream().map(SdcDuplicateEntity::getUniqueObjectHash).toList();
    HashMap<Integer, SdcDuplicateEntity> foundDupsMap = new HashMap<>();
    foundDuplicates.stream().forEach(entry -> foundDupsMap.put(entry.getUniqueObjectHash(), entry));
    List<SdcDuplicateEntity> finalDups = new ArrayList<>();

    foundDupsMap.keySet().stream().forEach(dupHash -> {
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
