package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolHistoryRepository;
import java.time.LocalDateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SdcSchoolHistoryService {

  private final SdcSchoolHistoryRepository sdcSchoolHistoryRepository;

  @Autowired
  public SdcSchoolHistoryService(SdcSchoolHistoryRepository sdcSchoolHistoryRepository) {
    this.sdcSchoolHistoryRepository = sdcSchoolHistoryRepository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void createSDCSchoolHistory(SdcSchoolEntity curSDCSchoolEntity, String updateUser) {
    final SdcSchoolHistoryEntity sdcSchoolHistoryEntity = new SdcSchoolHistoryEntity();
    BeanUtils.copyProperties(curSDCSchoolEntity, sdcSchoolHistoryEntity);
    sdcSchoolHistoryEntity.setCreateUser(updateUser);
    sdcSchoolHistoryEntity.setCreateDate(LocalDateTime.now());
    sdcSchoolHistoryEntity.setUpdateUser(updateUser);
    sdcSchoolHistoryEntity.setUpdateDate(LocalDateTime.now());

    sdcSchoolHistoryRepository.save(sdcSchoolHistoryEntity);
  }
}
