package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
}
