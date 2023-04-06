package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class SdcSchoolCollectionService {

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  private final SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService;

  private final SdcSchoolCollectionStudentHistoryService sdcSchoolCollectionStudentHistoryService;

  @Autowired
  public SdcSchoolCollectionService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService, SdcSchoolCollectionStudentHistoryService sdcSchoolCollectionStudentHistoryService) {
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
    this.sdcSchoolCollectionHistoryService = sdcSchoolCollectionHistoryService;
    this.sdcSchoolCollectionStudentHistoryService = sdcSchoolCollectionStudentHistoryService;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public SdcSchoolCollectionEntity saveSdcSchoolCollection(SdcSchoolCollectionEntity curSDCSchoolEntity) {
    var entity = this.sdcSchoolCollectionRepository.save(curSDCSchoolEntity);
    this.sdcSchoolCollectionHistoryService.createSDCSchoolHistory(entity, curSDCSchoolEntity.getUpdateUser());
    entity.getSDCSchoolStudentEntities().stream().forEach(sdcSchoolCollectionStudentEntity -> this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(sdcSchoolCollectionStudentEntity, curSDCSchoolEntity.getUpdateUser()));
    return entity;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity saveSdcSchoolCollectionStudent(SdcSchoolCollectionStudentEntity curSdcSchoolCollectionStudentEntity) {
    var entity = this.sdcSchoolCollectionStudentRepository.save(curSdcSchoolCollectionStudentEntity);
    this.sdcSchoolCollectionStudentHistoryService.createSDCSchoolStudentHistory(entity, curSdcSchoolCollectionStudentEntity.getUpdateUser());
    return entity;
  }

  public SdcSchoolCollectionEntity getSdcSchoolCollectionBySchoolID(UUID schoolID) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntity =  sdcSchoolCollectionRepository.findCollectionBySchoolId(schoolID);
    if(sdcSchoolCollectionEntity.isPresent()) {
      return sdcSchoolCollectionEntity.get();
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection for school Id", schoolID.toString());
    }
  }

  public SdcSchoolCollectionEntity getSdcSchoolCollection(UUID sdcSchoolCollectionID) {
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntity =  sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID);
    if(sdcSchoolCollectionEntity.isPresent()) {
      return sdcSchoolCollectionEntity.get();
    } else {
      throw new EntityNotFoundException(SdcSchoolCollectionEntity.class, "SdcSchoolCollection for sdcSchoolCollectionID", sdcSchoolCollectionID.toString());
    }
  }
}
