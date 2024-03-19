package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionHistoryEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SdcSchoolCollectionHistoryService {

  public SdcSchoolCollectionHistoryEntity createSDCSchoolHistory(SdcSchoolCollectionEntity curSDCSchoolEntity, String updateUser) {
    final SdcSchoolCollectionHistoryEntity sdcSchoolHistoryEntity = new SdcSchoolCollectionHistoryEntity();
    BeanUtils.copyProperties(curSDCSchoolEntity, sdcSchoolHistoryEntity);
    sdcSchoolHistoryEntity.setSdcSchoolCollection(curSDCSchoolEntity);
    sdcSchoolHistoryEntity.setCollectionID(curSDCSchoolEntity.getCollectionEntity().getCollectionID());
    sdcSchoolHistoryEntity.setCreateUser(updateUser);
    sdcSchoolHistoryEntity.setCreateDate(LocalDateTime.now());
    sdcSchoolHistoryEntity.setUpdateUser(updateUser);
    sdcSchoolHistoryEntity.setUpdateDate(LocalDateTime.now());

    return sdcSchoolHistoryEntity;
  }
}
