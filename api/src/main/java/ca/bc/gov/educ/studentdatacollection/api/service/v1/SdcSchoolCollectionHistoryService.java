package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SdcSchoolCollectionHistoryService {

  private final SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;

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

  public SdcSchoolCollectionLightHistoryEntity createSDCLightSchoolHistory(SdcSchoolCollectionLightEntity curSDCSchoolEntity, String updateUser) {
    final SdcSchoolCollectionLightHistoryEntity sdcSchoolHistoryEntity = new SdcSchoolCollectionLightHistoryEntity();
    BeanUtils.copyProperties(curSDCSchoolEntity, sdcSchoolHistoryEntity);
    sdcSchoolHistoryEntity.setSdcSchoolCollection(curSDCSchoolEntity);
    sdcSchoolHistoryEntity.setCollectionID(curSDCSchoolEntity.getCollectionID());
    sdcSchoolHistoryEntity.setCreateUser(updateUser);
    sdcSchoolHistoryEntity.setCreateDate(LocalDateTime.now());
    sdcSchoolHistoryEntity.setUpdateUser(updateUser);
    sdcSchoolHistoryEntity.setUpdateDate(LocalDateTime.now());

    return sdcSchoolHistoryEntity;
  }

  public List<SdcSchoolCollectionStudentHistoryEntity> getFirstHistoryRecordsForStudentIDs(Set<UUID> sdcSchoolStudentIDs){
    return sdcSchoolCollectionStudentHistoryRepository.findOrginalHistoryRecordsForStudentIDList(sdcSchoolStudentIDs);
  }
}
