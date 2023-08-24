package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentHistoryRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SdcSchoolCollectionStudentHistoryService {

  private final SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;

  @Autowired
  public SdcSchoolCollectionStudentHistoryService(SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository) {
    this.sdcSchoolCollectionStudentHistoryRepository = sdcSchoolCollectionStudentHistoryRepository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void createSDCSchoolStudentHistory(SdcSchoolCollectionStudentEntity curSdcSchoolStudentEntity, String updateUser) {
    final SdcSchoolCollectionStudentHistoryEntity sdcSchoolCollectionStudentHistoryEntity = new SdcSchoolCollectionStudentHistoryEntity();
    BeanUtils.copyProperties(curSdcSchoolStudentEntity, sdcSchoolCollectionStudentHistoryEntity);
    sdcSchoolCollectionStudentHistoryEntity.setSdcSchoolCollectionID(curSdcSchoolStudentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID());
    sdcSchoolCollectionStudentHistoryEntity.setCreateUser(updateUser);
    sdcSchoolCollectionStudentHistoryEntity.setCreateDate(LocalDateTime.now());
    sdcSchoolCollectionStudentHistoryEntity.setUpdateUser(updateUser);
    sdcSchoolCollectionStudentHistoryEntity.setUpdateDate(LocalDateTime.now());

    sdcSchoolCollectionStudentHistoryRepository.save(sdcSchoolCollectionStudentHistoryEntity);
  }
}
