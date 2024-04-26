package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcInDistrictDuplicateEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDuplicateRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class SdcDuplicatesService {

  private final SdcDuplicateRepository sdcDuplicateRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final RestUtils restUtils;

  @Autowired
  public SdcDuplicatesService(SdcDuplicateRepository sdcDuplicateRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
    this.sdcDuplicateRepository = sdcDuplicateRepository;
    this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
    this.restUtils = restUtils;
  }

  public List<SdcInDistrictDuplicateEntity> getAllInDistrictCollectionDuplicates(UUID sdcDistrictCollectionID) {
    var existingDuplicates = sdcDuplicateRepository.findAllBySdcDistrictCollectionID(sdcDistrictCollectionID);
    var duplicateStudentEntities = sdcSchoolCollectionStudentRepository.findAllInDistrictDuplicateStudentsInSdcDistrictCollection(sdcDistrictCollectionID);

    List<SdcInDistrictDuplicateEntity> duplicates = new ArrayList<>();

    HashMap<UUID, List<SdcSchoolCollectionStudentEntity>> groupedDups = new HashMap<>();

    duplicateStudentEntities.forEach(student -> {
      if (!groupedDups.containsKey(student.getSdcSchoolCollectionStudentID())) {
        groupedDups.put(student.getSdcSchoolCollectionStudentID(), Arrays.asList(student));
      } else {
        groupedDups.get(student.getSdcSchoolCollectionStudentID()).add(student);
      }
    });

    groupedDups.forEach((sdcStudentID, sdcSchoolCollectionStudentEntities) -> {
      for (SdcSchoolCollectionStudentEntity entity1 : sdcSchoolCollectionStudentEntities) {
        for (SdcSchoolCollectionStudentEntity entity2 : sdcSchoolCollectionStudentEntities) {
           if (!entity1.getSdcSchoolCollectionStudentID().equals(entity2.getSdcSchoolCollectionStudentID()) && !duplicateAlreadyExists(existingDuplicates, entity1, entity2)) {
                duplicates.addAll(runDuplicatesCheck(entity1, entity2, sdcDistrictCollectionID));
              }
           }
      }
    });

    sdcDuplicateRepository.saveAll(duplicates);
    return duplicates;
  }

  private List<SdcInDistrictDuplicateEntity> runDuplicatesCheck(SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2, UUID sdcDistrictCollectionID){
    List<SdcInDistrictDuplicateEntity> dups = new ArrayList<>();
    School school1 = restUtils.getSchoolBySchoolID(entity1.getSdcSchoolCollection().getSchoolID().toString()).get();
    School school2 = restUtils.getSchoolBySchoolID(entity2.getSdcSchoolCollection().getSchoolID().toString()).get();
    //Is the student an adult?
    if(entity1.getIsAdult() || entity2.getIsAdult()){
      dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.ALLOWABLE, null, sdcDistrictCollectionID));
    }

    //In which grades are the two records reported
    if(SchoolGradeCodes.getKToNineGrades().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getKToNineGrades().contains(entity2.getEnrolledGradeCode())){
      dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.NON_ALLOWABLE, null, sdcDistrictCollectionID));
    }

    if((SchoolGradeCodes.getKToSevenEuGrades().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode())) ||
            (SchoolGradeCodes.getKToSevenEuGrades().contains(entity2.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode()))){
      dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.NON_ALLOWABLE, null, sdcDistrictCollectionID));
    }


    var isSchool1Independent = SchoolCategoryCodes.INDEPEND.getCode().equals(school1.getSchoolCategoryCode()) || SchoolCategoryCodes.INDP_FNS.getCode().equals(school1.getSchoolCategoryCode());
    var isSchool2Independent = SchoolCategoryCodes.INDEPEND.getCode().equals(school2.getSchoolCategoryCode()) || SchoolCategoryCodes.INDP_FNS.getCode().equals(school2.getSchoolCategoryCode());
    if(SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode()) && SchoolGradeCodes.getGrades10toSU().contains(entity2.getEnrolledGradeCode())){
      if((FacilityTypeCodes.DIST_LEARN.getCode().equals(school1.getFacilityTypeCode()) && SchoolCategoryCodes.INDEPEND.getCode().equals(school1.getSchoolCategoryCode())) ||
              (FacilityTypeCodes.DIST_LEARN.getCode().equals(school2.getFacilityTypeCode()) && SchoolCategoryCodes.INDEPEND.getCode().equals(school2.getSchoolCategoryCode()))) {
        dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.ALLOWABLE, null, sdcDistrictCollectionID));
      }else if(isSchool1Independent || isSchool2Independent) {
        if((isSchool1Independent && FacilityTypeCodes.DIST_LEARN.getCode().equals(school2.getFacilityTypeCode())) || (isSchool2Independent && FacilityTypeCodes.DIST_LEARN.getCode().equals(school1.getFacilityTypeCode()))) {
          dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.ALLOWABLE, null, sdcDistrictCollectionID));
        }else{
          dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.NON_ALLOWABLE, null, sdcDistrictCollectionID));
        }
      }else if(school1.getDistrictId().equals(school2.getDistrictId())){
        if(FacilityTypeCodes.ALT_PROGS.getCode().equals(school1.getFacilityTypeCode()) || FacilityTypeCodes.ALT_PROGS.getCode().equals(school2.getFacilityTypeCode())){
          dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.NON_ALLOWABLE, null, sdcDistrictCollectionID));
        }else{
          dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.ALLOWABLE, null, sdcDistrictCollectionID));
        }
      }
    }

    var isStudent1Grade8or9 = SchoolGradeCodes.getGrades8and9().contains(entity1.getEnrolledGradeCode());
    var isStudent2Grade8or9 = SchoolGradeCodes.getGrades8and9().contains(entity2.getEnrolledGradeCode());
    var isStudent1Grade10toSU = SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode());
    var isStudent2Grade10toSU = SchoolGradeCodes.getGrades10toSU().contains(entity1.getEnrolledGradeCode());

    if((isStudent1Grade8or9 && isStudent2Grade10toSU) || (isStudent2Grade8or9 && isStudent1Grade10toSU)){
      if(FacilityTypeCodes.DIST_LEARN.getCode().equals(school1.getFacilityTypeCode()) && FacilityTypeCodes.DIST_LEARN.getCode().equals(school2.getFacilityTypeCode())) {
        dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.ALLOWABLE, null, sdcDistrictCollectionID));
      }else if((isStudent1Grade8or9 && FacilityTypeCodes.STANDARD.getCode().equals(school1.getFacilityTypeCode()) && isStudent2Grade10toSU && FacilityTypeCodes.DIST_LEARN.getCode().equals(school2.getFacilityTypeCode())) ||
              (isStudent2Grade8or9 && FacilityTypeCodes.STANDARD.getCode().equals(school2.getFacilityTypeCode()) && isStudent1Grade10toSU && FacilityTypeCodes.DIST_LEARN.getCode().equals(school1.getFacilityTypeCode()))){
        dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.ALLOWABLE, null, sdcDistrictCollectionID));
      }else {
        dups.add(generateDuplicateEntity(entity1, entity2, DuplicateTypeCode.ENROLLMENT, DuplicateSeverityCode.NON_ALLOWABLE, null, sdcDistrictCollectionID));
      }
    }


    return dups;
  }

  private SdcInDistrictDuplicateEntity generateDuplicateEntity(SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2, DuplicateTypeCode typeCode, DuplicateSeverityCode severityCode, ProgramDuplicateTypeCode programDuplicateTypeCode, UUID sdcDistrictCollectionID){
    SdcInDistrictDuplicateEntity dupe = new SdcInDistrictDuplicateEntity();
    dupe.setSdcDistrictCollectionID(sdcDistrictCollectionID);
    dupe.setSdcSchoolCollectionStudent1Entity(entity1);
    dupe.setSdcSchoolCollectionStudent2Entity(entity2);
    dupe.setCreateDate(LocalDateTime.now());
    dupe.setUpdateDate(LocalDateTime.now());
    dupe.setDuplicateTypeCode(typeCode.getCode());
    dupe.setDuplicateSeverityCode(severityCode.getCode());
    dupe.setProgramDuplicateTypeCode(programDuplicateTypeCode.getCode());
    return dupe;
  }

  private boolean duplicateAlreadyExists(List<SdcInDistrictDuplicateEntity> existingDuplicates, SdcSchoolCollectionStudentEntity entity1, SdcSchoolCollectionStudentEntity entity2){
    for(SdcInDistrictDuplicateEntity sdcInDistrictDuplicateEntity : existingDuplicates){
      if((entity1.getSdcSchoolCollectionStudentID().equals(sdcInDistrictDuplicateEntity.getSdcSchoolCollectionStudent1Entity().getSdcSchoolCollectionStudentID())
              && entity2.getSdcSchoolCollectionStudentID().equals(sdcInDistrictDuplicateEntity.getSdcSchoolCollectionStudent2Entity().getSdcSchoolCollectionStudentID())) ||
              (entity2.getSdcSchoolCollectionStudentID().equals(sdcInDistrictDuplicateEntity.getSdcSchoolCollectionStudent1Entity().getSdcSchoolCollectionStudentID())
                      && entity1.getSdcSchoolCollectionStudentID().equals(sdcInDistrictDuplicateEntity.getSdcSchoolCollectionStudent2Entity().getSdcSchoolCollectionStudentID()))){
        return true;
      }
    }
    return false;
  }

}
