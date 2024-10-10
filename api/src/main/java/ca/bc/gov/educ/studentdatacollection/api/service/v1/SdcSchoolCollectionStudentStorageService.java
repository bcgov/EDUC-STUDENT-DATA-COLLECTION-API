package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentHistoryRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentLightRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentStorageService {

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  private final SdcSchoolCollectionStudentLightRepository sdcSchoolCollectionStudentLightRepository;

  private final SdcSchoolCollectionStudentHistoryService sdcSchoolCollectionStudentHistoryService;

  private final SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;

  public SdcSchoolCollectionStudentLightEntity saveSdcStudentWithHistory(SdcSchoolCollectionStudentLightEntity studentEntity) {
    studentEntity.setUpdateDate(LocalDateTime.now());
    var savedEntity = this.sdcSchoolCollectionStudentLightRepository.save(studentEntity);
    sdcSchoolCollectionStudentHistoryRepository.save(this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(savedEntity, studentEntity.getUpdateUser()));
    return savedEntity;
  }


  public SdcSchoolCollectionStudentEntity saveSdcStudentWithHistory(SdcSchoolCollectionStudentEntity studentEntity) {
    studentEntity.setUpdateDate(LocalDateTime.now());
    var savedEntity = this.sdcSchoolCollectionStudentRepository.save(studentEntity);
    sdcSchoolCollectionStudentHistoryRepository.save(this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(savedEntity, studentEntity.getUpdateUser()));
    return savedEntity;
  }

  public List<SdcSchoolCollectionStudentEntity> saveAllSDCStudentsWithHistory(List<SdcSchoolCollectionStudentEntity> studentEntities) {
    List<SdcSchoolCollectionStudentHistoryEntity> history = new ArrayList<>();
    studentEntities.forEach(entity -> {
      entity.setUpdateDate(LocalDateTime.now());
      history.add(this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(entity, entity.getUpdateUser()));
    });
    sdcSchoolCollectionStudentHistoryRepository.saveAll(history);
    return this.sdcSchoolCollectionStudentRepository.saveAll(studentEntities);
  }

}
