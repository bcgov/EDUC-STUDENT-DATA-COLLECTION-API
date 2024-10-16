package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class SdcSchoolCollectionStudentHistoryService {

  @Transactional(propagation = Propagation.MANDATORY)
  public SdcSchoolCollectionStudentHistoryEntity createSDCSchoolStudentHistory(SdcSchoolCollectionStudentEntity curSdcSchoolStudentEntity, String updateUser) {
    final SdcSchoolCollectionStudentHistoryEntity sdcSchoolCollectionStudentHistoryEntity = new SdcSchoolCollectionStudentHistoryEntity();
    BeanUtils.copyProperties(curSdcSchoolStudentEntity, sdcSchoolCollectionStudentHistoryEntity);
    sdcSchoolCollectionStudentHistoryEntity.setSdcSchoolCollectionStudentID(curSdcSchoolStudentEntity.getSdcSchoolCollectionStudentID());
    sdcSchoolCollectionStudentHistoryEntity.setSdcSchoolCollectionID(curSdcSchoolStudentEntity.getSdcSchoolCollection().getSdcSchoolCollectionID());
    sdcSchoolCollectionStudentHistoryEntity.setCreateUser(updateUser);
    sdcSchoolCollectionStudentHistoryEntity.setCreateDate(LocalDateTime.now());
    sdcSchoolCollectionStudentHistoryEntity.setUpdateUser(updateUser);
    sdcSchoolCollectionStudentHistoryEntity.setUpdateDate(LocalDateTime.now());
    return sdcSchoolCollectionStudentHistoryEntity;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public SdcSchoolCollectionStudentHistoryEntity createSDCSchoolStudentHistory(SdcSchoolCollectionStudentLightEntity curSdcSchoolStudentEntity, String updateUser) {
    final SdcSchoolCollectionStudentHistoryEntity sdcSchoolCollectionStudentHistoryEntity = new SdcSchoolCollectionStudentHistoryEntity();
    BeanUtils.copyProperties(curSdcSchoolStudentEntity, sdcSchoolCollectionStudentHistoryEntity);
    sdcSchoolCollectionStudentHistoryEntity.setSdcSchoolCollectionStudentID(curSdcSchoolStudentEntity.getSdcSchoolCollectionStudentID());
    sdcSchoolCollectionStudentHistoryEntity.setSdcSchoolCollectionID(curSdcSchoolStudentEntity.getSdcSchoolCollectionID());
    sdcSchoolCollectionStudentHistoryEntity.setCreateUser(updateUser);
    sdcSchoolCollectionStudentHistoryEntity.setCreateDate(LocalDateTime.now());
    sdcSchoolCollectionStudentHistoryEntity.setUpdateUser(updateUser);
    sdcSchoolCollectionStudentHistoryEntity.setUpdateDate(LocalDateTime.now());
    return sdcSchoolCollectionStudentHistoryEntity;
  }
}
