package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionHistoryRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SdcSchoolCollectionHistoryService {

  private final SdcSchoolCollectionHistoryRepository sdcSchoolHistoryRepository;

  @Autowired
  public SdcSchoolCollectionHistoryService(SdcSchoolCollectionHistoryRepository sdcSchoolHistoryRepository) {
    this.sdcSchoolHistoryRepository = sdcSchoolHistoryRepository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void createSDCSchoolHistory(SdcSchoolCollectionEntity curSDCSchoolEntity, String updateUser) {
    final SdcSchoolCollectionHistoryEntity sdcSchoolHistoryEntity = new SdcSchoolCollectionHistoryEntity();
    BeanUtils.copyProperties(curSDCSchoolEntity, sdcSchoolHistoryEntity);
    sdcSchoolHistoryEntity.setCollectionID(curSDCSchoolEntity.getCollectionEntity().getCollectionID());
    sdcSchoolHistoryEntity.setCreateUser(updateUser);
    sdcSchoolHistoryEntity.setCreateDate(LocalDateTime.now());
    sdcSchoolHistoryEntity.setUpdateUser(updateUser);
    sdcSchoolHistoryEntity.setUpdateDate(LocalDateTime.now());

    sdcSchoolHistoryRepository.save(sdcSchoolHistoryEntity);
  }
}
