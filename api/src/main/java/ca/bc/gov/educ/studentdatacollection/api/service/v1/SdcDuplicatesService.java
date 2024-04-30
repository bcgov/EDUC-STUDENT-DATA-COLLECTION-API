package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDuplicateRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class SdcDuplicatesService {

  private final SdcDuplicateRepository sdcDuplicateRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final ValidationRulesService validationRulesService;
  private final RestUtils restUtils;

  @Autowired
  public SdcDuplicatesService(SdcDuplicateRepository sdcDuplicateRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, ValidationRulesService validationRulesService, RestUtils restUtils) {
      this.sdcDuplicateRepository = sdcDuplicateRepository;
      this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
      this.validationRulesService = validationRulesService;
      this.restUtils = restUtils;
  }

  public List<SdcDuplicateEntity> getAllInDistrictCollectionDuplicates(UUID sdcDistrictCollectionID) {
    var existingDuplicates = sdcDuplicateRepository.findAllBySdcDuplicateStudentEntities_SdcDistrictCollectionID(sdcDistrictCollectionID);
    var duplicateStudentEntities = sdcSchoolCollectionStudentRepository.findAllInDistrictDuplicateStudentsInSdcDistrictCollection(sdcDistrictCollectionID);

    List<SdcDuplicateEntity> newDuplicates = new ArrayList<>();

    HashMap<UUID, List<SdcSchoolCollectionStudentEntity>> groupedDups = new HashMap<>();

    duplicateStudentEntities.forEach(student -> {
      if (!groupedDups.containsKey(student.getAssignedStudentId())) {
        groupedDups.put(student.getAssignedStudentId(), new ArrayList<>(Arrays.asList(student)));
      } else {
        groupedDups.get(student.getAssignedStudentId()).add(student);
      }
    });

    groupedDups.forEach((sdcStudentID, sdcSchoolCollectionStudentEntities) -> {
      for (SdcSchoolCollectionStudentEntity entity1 : sdcSchoolCollectionStudentEntities) {
        for (SdcSchoolCollectionStudentEntity entity2 : sdcSchoolCollectionStudentEntities) {
           if (!entity1.getSdcSchoolCollectionStudentID().equals(entity2.getSdcSchoolCollectionStudentID()) && !duplicateAlreadyExists(existingDuplicates, newDuplicates, entity1, entity2)) {
                newDuplicates.addAll(runDuplicatesCheck(DuplicateLevelCode.IN_DIST, entity1, entity2));
           }
        }
      }
    });

    sdcDuplicateRepository.saveAll(newDuplicates);
    return newDuplicates;
  }

  private List<SdcDuplicateEntity> runDuplicatesCheck(DuplicateLevelCode level, SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2){
    List<SdcDuplicateEntity> dups = new ArrayList<>();
    School school1 = restUtils.getSchoolBySchoolID(entity1.getSdcSchoolCollection().getSchoolID().toString()).orElseThrow(() ->
            new StudentDataCollectionAPIRuntimeException("School provided by ID " + entity1.getSdcSchoolCollection().getSchoolID() + "was not found - this is not expected"));
    School school2 = restUtils.getSchoolBySchoolID(entity2.getSdcSchoolCollection().getSchoolID().toString()).orElseThrow(() ->
            new StudentDataCollectionAPIRuntimeException("School provided by ID " + entity2.getSdcSchoolCollection().getSchoolID() + "was not found - this is not expected"));
    //Is the student an adult?
    if(entity1.getIsAdult() || entity2.getIsAdult()){
      addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
    }

    //In which grades are the two records reported - K-9 Check
    if(dups.isEmpty() && SchoolGradeCodes.getKToNineGrades().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getKToNineGrades().contains(entity2.getEnrolledGradeCode())){
      addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
    }

    //In which grades are the two records reported - K-7 & 10-12,SU Check
    if(dups.isEmpty() && ((SchoolGradeCodes.getKToSevenEuGrades().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode())) ||
            (SchoolGradeCodes.getKToSevenEuGrades().contains(entity2.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode())))){
      addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
    }

    //In which grades are the two records reported - 10,11,12,SU Check
    var isSchool1Independent = SchoolCategoryCodes.INDEPEND.getCode().equals(school1.getSchoolCategoryCode()) || SchoolCategoryCodes.INDP_FNS.getCode().equals(school1.getSchoolCategoryCode());
    var isSchool2Independent = SchoolCategoryCodes.INDEPEND.getCode().equals(school2.getSchoolCategoryCode()) || SchoolCategoryCodes.INDP_FNS.getCode().equals(school2.getSchoolCategoryCode());
    if(dups.isEmpty() && SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode())){
      if((FacilityTypeCodes.DIST_LEARN.getCode().equals(school1.getFacilityTypeCode()) && SchoolCategoryCodes.INDEPEND.getCode().equals(school1.getSchoolCategoryCode())) ||
              (FacilityTypeCodes.DIST_LEARN.getCode().equals(school2.getFacilityTypeCode()) && SchoolCategoryCodes.INDEPEND.getCode().equals(school2.getSchoolCategoryCode()))) {
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else if(isSchool1Independent || isSchool2Independent) {
        if((isSchool1Independent && FacilityTypeCodes.DIST_LEARN.getCode().equals(school2.getFacilityTypeCode())) || (isSchool2Independent && FacilityTypeCodes.DIST_LEARN.getCode().equals(school1.getFacilityTypeCode()))) {
          addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
        }else{
          addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
        }
      }else if(school1.getDistrictId().equals(school2.getDistrictId())){
        if(FacilityTypeCodes.ALT_PROGS.getCode().equals(school1.getFacilityTypeCode()) || FacilityTypeCodes.ALT_PROGS.getCode().equals(school2.getFacilityTypeCode())){
          addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
        }else{
          addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
        }
      }else if(FacilityTypeCodes.DIST_LEARN.getCode().equals(school1.getFacilityTypeCode()) || FacilityTypeCodes.DIST_LEARN.getCode().equals(school2.getFacilityTypeCode())){
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else{
        addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }
    }

    //In which grades are the two records reported - 8&9, 10-12,SU Check
    var isStudent1Grade8or9 = SchoolGradeCodes.getGrades8and9().contains(entity1.getEnrolledGradeCode());
    var isStudent2Grade8or9 = SchoolGradeCodes.getGrades8and9().contains(entity2.getEnrolledGradeCode());
    var isStudent1Grade10toSU = SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode());
    var isStudent2Grade10toSU = SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode());

    if(dups.isEmpty() && ((isStudent1Grade8or9 && isStudent2Grade10toSU) || (isStudent2Grade8or9 && isStudent1Grade10toSU))){
      if(FacilityTypeCodes.DIST_LEARN.getCode().equals(school1.getFacilityTypeCode()) && FacilityTypeCodes.DIST_LEARN.getCode().equals(school2.getFacilityTypeCode())) {
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else if((isStudent1Grade8or9 && FacilityTypeCodes.STANDARD.getCode().equals(school1.getFacilityTypeCode()) && isStudent2Grade10toSU && FacilityTypeCodes.DIST_LEARN.getCode().equals(school2.getFacilityTypeCode())) ||
              (isStudent2Grade8or9 && FacilityTypeCodes.STANDARD.getCode().equals(school2.getFacilityTypeCode()) && isStudent1Grade10toSU && FacilityTypeCodes.DIST_LEARN.getCode().equals(school1.getFacilityTypeCode()))){
        addAllowableDuplicateWithProgramDups(dups, level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
      }else {
        addNonAllowableDuplicate(dups,level, entity1, entity2, DuplicateTypeCode.ENROLLMENT, null);
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
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.CAREER));
        }

        if(StringUtils.isNotBlank(student1.getSpecialEducationCategoryCode()) && StringUtils.isNotBlank(student2.getSpecialEducationCategoryCode())){
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.SPECIAL_ED));
        }

        List<String> student1IndigenousPrograms = EnrolledProgramCodes.getIndigenousProgramCodes().stream().filter(student1Programs::contains).toList();
        List<String> student2IndigenousPrograms = EnrolledProgramCodes.getIndigenousProgramCodes().stream().filter(student2Programs::contains).toList();
        if(student1IndigenousPrograms.stream().anyMatch(student2IndigenousPrograms::contains)){
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.INDIGENOUS));
        }

        List<String> student1LanguagePrograms = EnrolledProgramCodes.getFrenchProgramCodesWithEll().stream().filter(student1Programs::contains).toList();
        List<String> student2LanguagePrograms = EnrolledProgramCodes.getFrenchProgramCodesWithEll().stream().filter(student2Programs::contains).toList();
        if(student1LanguagePrograms.stream().anyMatch(student2LanguagePrograms::contains)){
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.LANGUAGE));
        }

        List<String> student1CareerPrograms = EnrolledProgramCodes.getCareerProgramCodes().stream().filter(student1Programs::contains).toList();
        List<String> student2CareerPrograms = EnrolledProgramCodes.getCareerProgramCodes().stream().filter(student2Programs::contains).toList();
        if(student1CareerPrograms.stream().anyMatch(student2CareerPrograms::contains)){
          newDuplicates.add(generateDuplicateEntity(level, student1, student2, DuplicateTypeCode.PROGRAM, DuplicateSeverityCode.NON_ALLOWABLE, ProgramDuplicateTypeCode.CAREER));
        }
      }
    }
    return newDuplicates;
  }

  private void addAllowableDuplicateWithProgramDups(List<SdcDuplicateEntity> newDuplicates, DuplicateLevelCode levelCode, SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2, DuplicateTypeCode typeCode, ProgramDuplicateTypeCode programDuplicateTypeCode){
    newDuplicates.add(generateDuplicateEntity(levelCode,entity1,entity2,typeCode,DuplicateSeverityCode.ALLOWABLE,programDuplicateTypeCode));
    runProgramDuplicates(newDuplicates,entity1,entity2,levelCode);
  }

  private void addNonAllowableDuplicate(List<SdcDuplicateEntity> newDuplicates, DuplicateLevelCode levelCode, SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2, DuplicateTypeCode typeCode, ProgramDuplicateTypeCode programDuplicateTypeCode){
    newDuplicates.add(generateDuplicateEntity(levelCode,entity1,entity2,typeCode,DuplicateSeverityCode.NON_ALLOWABLE,programDuplicateTypeCode));
  }

  private SdcDuplicateEntity generateDuplicateEntity(DuplicateLevelCode levelCode, SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2, DuplicateTypeCode typeCode, DuplicateSeverityCode severityCode, ProgramDuplicateTypeCode programDuplicateTypeCode){
    SdcDuplicateEntity dupe = new SdcDuplicateEntity();
    dupe.setCreateDate(LocalDateTime.now());
    dupe.setUpdateDate(LocalDateTime.now());
    dupe.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    dupe.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    dupe.setDuplicateTypeCode(typeCode.getCode());
    dupe.setDuplicateSeverityCode(severityCode.getCode());
    dupe.setProgramDuplicateTypeCode(programDuplicateTypeCode != null ? programDuplicateTypeCode.getCode() : null);
    dupe.setDuplicateLevelCode(levelCode.getCode());

    dupe.getSdcDuplicateStudentEntities().add(createSdcDuplicateStudent(entity1, dupe));
    dupe.getSdcDuplicateStudentEntities().add(createSdcDuplicateStudent(entity2, dupe));
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

  private boolean duplicateAlreadyExists(List<SdcDuplicateEntity> existingDuplicates, List<SdcDuplicateEntity> newDups, SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2){
    return checkForDuplicates(existingDuplicates, entity1.getSdcSchoolCollectionStudentID(), entity2.getSdcSchoolCollectionStudentID()) ||
            checkForDuplicates(newDups, entity1.getSdcSchoolCollectionStudentID(), entity2.getSdcSchoolCollectionStudentID());
  }

  private boolean checkForDuplicates(List<SdcDuplicateEntity> duplicates, UUID sdcSchoolCollectionStudentID1, UUID sdcSchoolCollectionStudentID2){
    for(SdcDuplicateEntity sdcDuplicateEntity : duplicates){
      boolean foundStud1 = false;
      boolean foundStud2 = false;
      for(SdcDuplicateStudentEntity studentEntity: sdcDuplicateEntity.getSdcDuplicateStudentEntities()){
        if(studentEntity.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().equals(sdcSchoolCollectionStudentID1)){
          foundStud1 = true;
        }
        if(studentEntity.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID().equals(sdcSchoolCollectionStudentID2)){
          foundStud2 = true;
        }
      }
      if(foundStud1 && foundStud2){
        return true;
      }
    }
    return false;
  }

}
